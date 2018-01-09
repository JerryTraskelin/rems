(ns rems.events
  "Components and endpoints for submitting events (review, approve, etc.) for applications"
  (:require [compojure.core :refer [POST defroutes]]
            [hiccup.core :as hiccup]
            [rems.anti-forgery :refer [anti-forgery-field]]
            [rems.db.applications :as applications]
            [rems.db.core :as db]
            [rems.db.users :as users]
            [rems.roles :refer [has-roles?]]
            [rems.text :refer [text]]
            [rems.util :refer :all]
            [ring.util.response :refer [redirect]]))

(defn- actions-form-attrs [app]
  {:method "post"
   :action (str "/event/" (:id app) "/" (:curround app))})

(defn- confirm-modal
  "Creates a confimation pop-up for actions that could potentially cause harm if triggered by mistake.
   Takes the following arguments:
   name-field:   name of the button
   action-title: text for the button shown to user
   app:          application id the modal refers to
   title-txt:    desired text for the title of the pop-up"
  [name-field action-title app title-txt]
  [:div.modal.fade {:id (str name-field "-modal") :tabindex "-1" :role "dialog" :aria-labelledby "confirmModalLabel" :aria-hidden "true"}
   [:div.modal-dialog {:role "document"}
    [:div.modal-content
     [:form (actions-form-attrs app)
      (anti-forgery-field)
      [:div.modal-header
       [:h5#confirmModalLabel.modal-title title-txt]
       [:button.close {:type "button" :data-dismiss "modal" :aria-label (text :t.actions/cancel)}
        [:span {:aria-hidden "true"} "&times;"]]]
      [:div.modal-body
       [:div.form-group
        [:textarea.form-control {:name "comment"}]]]
      [:div.modal-footer
       [:button.btn.btn-secondary {:data-dismiss "modal"} (text :t.actions/cancel)]
       [:button.btn.btn-primary {:type "submit" :name name-field} action-title]]]]]])

(defn approval-confirm-modal [name-field action-title app]
  (confirm-modal name-field action-title app (if (has-roles? :approver) (text :t.form/add-comments) (text :t.form/add-comments-applicant))))

(defn review-confirm-modal [name-field action-title app]
  (confirm-modal name-field action-title app (if (has-roles? :reviewer) (text :t.form/add-comments) (text :t.form/add-comments-applicant))))

(defn- reviewer-selection [user-attrs]
  (let [username (get-username user-attrs)
        mail (get-user-mail user-attrs)]
    (when (and username mail)
      [:option {:value (get-user-id user-attrs)} (str username (hiccup/h " <") mail (hiccup/h ">"))])))

(defn review-request-modal [app]
  [:div.modal.fade {:id "review-request-modal" :tabindex "-1" :role "dialog" :aria-labelledby "confirmModalLabel" :aria-hidden "true"}
   [:div.modal-dialog {:role "document"}
    [:div.modal-content
     [:form (actions-form-attrs app)
      (anti-forgery-field)
      [:div.modal-header
       [:h5#confirmModalLabel.modal-title (text :t.form/add-comments)]
       [:button.close {:type "button" :data-dismiss "modal" :aria-label (text :t.actions/cancel)}
        [:span {:aria-hidden "true"} "&times;"]]]
      [:div.modal-body
       [:div.form-group
        [:textarea.form-control {:name "comment"}]]
       [:div.form-group
        [:label (text :t.actions/review-request-selection)]
        [:select.form-control {:name "recipients" :multiple "multiple" :required true}
         (let [other-users (filter #(not= (get-user-id) %) (map :userid (db/get-users)))
               users-attrs (map users/get-user-attributes other-users)]
           (for [user-attrs users-attrs]
             (reviewer-selection user-attrs)))]]]
      [:div.modal-footer
       [:button.btn.btn-secondary {:data-dismiss "modal"} (text :t.actions/cancel)]
       [:button.btn.btn-primary {:type "submit" :name "review-request"} (text :t.actions/review-request)]]]]]])

(defn- approve-button [app]
  (list
   [:button#approve.btn.btn-primary {:type "button" :data-toggle "modal" :data-target "#approve-modal"}
    (text :t.actions/approve)]
   (approval-confirm-modal "approve" (text :t.actions/approve) app)))

(defn- reject-button [app]
  (list
   [:button#reject.btn.btn-secondary {:type "button" :data-toggle "modal" :data-target "#reject-modal"}
    (text :t.actions/reject)]
   (approval-confirm-modal "reject" (text :t.actions/reject) app)))

(defn review-button [app]
  (case (:review-type app)
    :normal
    (list
     [:button#review.btn.btn-primary {:type "button" :data-toggle "modal" :data-target "#review-modal"}
      (text :t.actions/review)]
     (review-confirm-modal "review" (text :t.actions/review) app))
    :third-party
    (list
     [:button#third-party-review.btn.btn-primary {:type "button" :data-toggle "modal" :data-target "#third-party-review-modal"}
      (text :t.actions/review)]
     (review-confirm-modal "third-party-review" (text :t.actions/review) app))))

(defn review-request-button [app]
  (list
    [:button#review-request.btn.btn-secondary {:type "button" :data-toggle "modal" :data-target "#review-request-modal"}
     (text :t.actions/review-request)]
    (review-request-modal app)))

(defn- return-button [app]
  (list
   [:button#return.btn.btn-secondary {:type "button" :data-toggle "modal" :data-target "#return-modal"}
    (text :t.actions/return)]
   (approval-confirm-modal "return" (text :t.actions/return) app)))

(defn- close-button [app]
  (list
   [:button#close.btn.btn-secondary {:type "button" :data-toggle "modal" :data-target "#close-modal"}
    (text :t.actions/close)]
   (approval-confirm-modal "close" (text :t.actions/close) app)))

(defn back-to-actions-button []
  [:a#back-actions.btn.btn-secondary.pull-left {:href "/actions"} (text :t.form/back-actions)])

(defn approve-form [app]
  [:div.commands
   (back-to-actions-button)
   (reject-button app)
   (return-button app)
   (review-request-button app)
   (approve-button app)])

(defn review-form [app]
  [:div.commands
   (back-to-actions-button)
   (review-button app)])

(defn approved-form [app]
  [:div.commands
   (back-to-actions-button)
   (close-button app)])

;; TODO handle closing when no draft or anything saved yet
(defroutes events-routes
  (POST "/event/:id/:round" [id round :as request]
        (let [id (Long/parseLong id)
              round (Long/parseLong round)
              input (:form-params request)
              action (cond (get input "approve") :approve
                           (get input "reject") :reject
                           (get input "return") :return
                           (get input "review") :review
                           (get input "review-request") :review-request
                           (get input "withdraw") :withdraw
                           (get input "close") :close
                           (get input "third-party-review") :third-party-review
                           :else (errorf "Unknown action!"))
              comment (get input "comment")
              comment (when-not (empty? comment) comment)]
          (case action
            :approve (applications/approve-application id round comment)
            :reject (applications/reject-application id round comment)
            :return (applications/return-application id round comment)
            :review (applications/review-application id round comment)
            :review-request (applications/send-review-request id round comment (get input "recipients"))
            :withdraw (applications/withdraw-application id round comment)
            :close (applications/close-application id round comment)
            :third-party-review (applications/perform-third-party-review id round comment))
          (assoc (redirect (if (or (has-roles? :approver) (has-roles? :reviewer)) "/actions" "/applications") :see-other)
                 :flash [{:status :success
                          :contents (case action
                                      :approve (text :t.actions/approve-success)
                                      :reject (text :t.actions/reject-success)
                                      :return (text :t.actions/return-success)
                                      :review (text :t.actions/review-success)
                                      :review-request (text :t.actions/review-request-success)
                                      :withdraw (text :t.actions/withdraw-success)
                                      :close (text :t.actions/close-success)
                                      :third-party-review (text :t.actions/review-success))}]))))
