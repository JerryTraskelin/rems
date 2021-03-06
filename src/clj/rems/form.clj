(ns rems.form
  (:require [rems.db.applications :refer [create-new-draft
                                          get-application-state
                                          get-form-for
                                          make-draft-application
                                          submit-application
                                          get-catalogue-items-by-application-id]]
            [rems.db.catalogue :refer [disabled-catalogue-item?]]
            [rems.db.core :as db]
            [rems.InvalidRequestException]
            [rems.roles :refer [has-roles?]]
            [rems.text :refer :all]
            [rems.util :refer [get-user-id getx]]))


(defn- may-see-event?
  "May the current user see this event?

  Applicants can't see review events, reviewers and approvers can see everything."
  [event]
  ;; could implement more granular checking based on authors etc.
  ;; now strictly role-based
  (let [applicant-types #{"apply" "autoapprove" "approve" "reject" "return" "withdraw" "close"}]
    (or (has-roles? :reviewer :approver) ;; reviewer and approver can see everything
        (applicant-types (:event event)))))

(defn- title-localizations [item]
  (into {} (for [[lang {title :title}] (:localizations item)
                 :when title]
             [lang title])))

;; TODO: in the validation :text, we always use the english title for
;; items since they don't have a non-localized title like licenses.
;; Should probably get rid of non-localize title for licenses as well?

(defn- validate-item
  [item]
  (when-not (:optional item)
    (when (empty? (:value item))
      {:type :item
       :id (:id item)
       :title (title-localizations item)
       :key :t.form.validation/required
       :text (text-format :t.form.validation/required (get-in item [:localizations :en :title]))})))

(defn- validate-license
  [license]
  (when-not (:approved license)
    {:type :license
     :id (:id license)
     :title (title-localizations license)
     :key :t.form.validation/required
     :text (text-format :t.form.validation/required (:title license))}))

(defn- validate
  "Validates a filled in form from (get-form-for application).

   Returns either :valid or a sequence of validation errors."
  [form]
  (let [messages (vec (concat (filterv identity (mapv validate-item (sort-by :id (:items form))))
                              (filterv identity (mapv validate-license (sort-by :id (:licenses form))))))]
    (if (empty? messages)
      :valid
      messages)))

(defn save-application-items [application-id catalogue-item-ids]
  (assert application-id)
  (assert (empty? (filter nil? catalogue-item-ids)) "nils sent in catalogue-item-ids")
  (assert (not (empty? catalogue-item-ids)))
  (doseq [catalogue-item-id catalogue-item-ids]
    (db/add-application-item! {:application application-id :item catalogue-item-id})))

(defn- save-fields
  [application-id input]
  (let [form (get-form-for application-id)]
    (doseq [{item-id :id :as item} (:items form)]
      (when-let [value (get input item-id)]
        (db/save-field-value! {:application application-id
                               :form (:id form)
                               :item item-id
                               :user (get-user-id)
                               :value value})))))

(defn save-licenses
  [application-id input]
  (let [form (get-form-for application-id)]
    (doseq [{licid :id :as license} (sort-by :id (:licenses form))]
      (if-let [state (get input licid (get input (str "license" licid)))]
        (db/save-license-approval! {:catappid application-id
                                    :round 0
                                    :licid licid
                                    :actoruserid (get-user-id)
                                    :state state})
        (db/delete-license-approval! {:catappid application-id
                                      :licid licid
                                      :actoruserid (get-user-id)})))))

(defn- save-form-inputs [application-id submit? items licenses]
  (save-fields application-id items)
  (save-licenses application-id licenses)
  (let [form (get-form-for application-id)
        validation (validate form)
        valid? (= :valid validation)
        perform-submit? (and submit? valid?)
        success? (or (not submit?) perform-submit?)]
    (when perform-submit?
      (submit-application application-id))
    (merge {:valid? valid?
            :success? success?}
           (when-not valid? {:validation validation}))))

(defn- check-for-disabled-items! [items]
  (let [disabled-items (filter disabled-catalogue-item? items)]
    (when (seq disabled-items)
      (throw (rems.InvalidRequestException. (str "Disabled catalogue items " (pr-str disabled-items)))))))

(defn- create-new-draft-for-items [catalogue-item-ids]
  (let [draft (make-draft-application catalogue-item-ids)]
    (check-for-disabled-items! (getx draft :catalogue-items))
    (let [wfid (getx draft :wfid)
          id (create-new-draft wfid)]
      (save-application-items id catalogue-item-ids)
      id)))

(defn api-save [request]
  (let [{:keys [application-id items licenses command]} request
        catalogue-item-ids (:catalogue-items request)
        ;; if no application-id given, create a new application
        application-id (or application-id
                           (create-new-draft-for-items catalogue-item-ids))
        _ (check-for-disabled-items! (get-catalogue-items-by-application-id application-id))
        submit? (= command "submit")
        {:keys [success? valid? validation]} (save-form-inputs application-id submit? items licenses)]
    (cond-> {:success success?
             :valid valid?}
      (not valid?) (assoc :validation validation)
      success? (assoc :id application-id
                      :state (:state (get-application-state application-id))))))
