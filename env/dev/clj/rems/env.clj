(ns rems.env
  (:require [clojure.tools.logging :as log]
            [conman.core :as conman]
            [mount.core :refer [defstate]]
            [rems.middleware.dev :refer [wrap-dev]]
            [rems.config :refer [env]]))

;; TODO these could be moved to config.edn
;; TODO component-guide does nothing for SPA
(def +defaults+
  {:init
   (fn []
     (log/info "\n-=[rems started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[rems has shut down successfully]=-"))
   :middleware wrap-dev
   :component-guide true})

(defstate ^:dynamic *db*
           :start (conman/connect! {:jdbc-url (env :database-url)})
           :stop (conman/disconnect! *db*))
