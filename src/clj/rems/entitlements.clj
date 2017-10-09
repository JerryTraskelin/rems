(ns rems.entitlements
  (:require [clojure.string :refer [join]]
            [compojure.core :refer [GET defroutes]]
            [rems.auth.util :refer [throw-unauthorized]]
            [rems.db.core :as db]
            [rems.roles :refer [has-roles?]]
            [rems.text :as text]
            [ring.util.http-response :as response]))

(defn- get-entitlements-for-export []
  (when-not (has-roles? :approver)
    (throw-unauthorized))
  (let [ents (db/get-entitlements-for-export)]
    (with-out-str
      (println "resource,application,user,start")
      (doseq [e ents]
        (println (join "," [(:resid e) (:catappid e) (:userid e) (text/localize-time (:start e))]))))))

(defroutes entitlements-routes
  (GET "/entitlements.csv" []
       (response/content-type
        {:status 200
         :body (get-entitlements-for-export)}
        "text/csv")))