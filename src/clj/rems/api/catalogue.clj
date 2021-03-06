(ns rems.api.catalogue
  (:require [compojure.api.sweet :refer :all]
            [rems.api.schema :refer :all]
            [rems.api.util :refer [check-user check-roles]]
            [rems.context :as context]
            [rems.db.catalogue :as catalogue]
            [rems.db.core :as db]
            [rems.util :refer [get-user-id]]
            [ring.util.http-response :refer :all]
            [schema.core :as s]))

(def GetCatalogueResponse
  [CatalogueItem])

;; TODO resid is misleading: it's the internal id, not the string id
;; Should we take the string id instead?
(def CreateCatalogueItemCommand
  {:title s/Str
   :form s/Num
   :resid s/Num
   :wfid s/Num})

(def CreateCatalogueItemResponse
  CatalogueItem)

(def CreateCatalogueItemLocalizationCommand
  {:id s/Num
   :langcode s/Str
   :title s/Str})

(def UpdateCatalogueItemCommand
  {:id s/Num
   :state (s/enum "disabled" "enabled")})

(def catalogue-api
  (context "/catalogue" []
    :tags ["catalogue"]

    (GET "/" []
      :summary "Get catalogue items"
      :query-params [{resource :- (describe s/Str "resource id") nil}]
      :return GetCatalogueResponse
      (binding [context/*lang* :en]
        (check-user)
        (ok (catalogue/get-localized-catalogue-items {:resource resource}))))

    (GET "/:item-id" []
      :summary "Get a single catalogue item"
      :path-params [item-id :- (describe s/Num "catalogue item")]
      :responses {200 {:schema CatalogueItem}
                  404 {:schema s/Str :description "Not found"}}

      (binding [context/*lang* :en]
        (check-user)
        (if-let [it (catalogue/get-localized-catalogue-item item-id)]
          (ok it)
          (not-found! "not found"))))

    (PUT "/create" []
      :summary "Create a new catalogue item"
      :body [command CreateCatalogueItemCommand]
      :return CreateCatalogueItemResponse
      (check-user)
      (check-roles :owner)
      (ok (catalogue/create-catalogue-item-command! command)))

    (PUT "/update" []
      :summary "Update catalogue item"
      :body [command UpdateCatalogueItemCommand]
      :return SuccessResponse
      (check-user)
      (check-roles :owner)
      (db/set-catalogue-item-state! {:item (:id command) :state (:state command)})
      (ok {:success true}))

    (PUT "/create-localization" []
      :summary "Create a new catalogue item localization"
      :body [command CreateCatalogueItemLocalizationCommand]
      :return SuccessResponse
      (check-user)
      (check-roles :owner)
      (ok (catalogue/create-catalogue-item-localization-command! command)))))
