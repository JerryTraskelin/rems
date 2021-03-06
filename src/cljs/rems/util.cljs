(ns rems.util)

(defn select-vals
  "Select values in map `m` specified by given keys `ks`.

  Values will be returned in the order specified by `ks`.

  You can specify a `default-value` that will be used if the
  key is not found in the map. This is like `get` function."
  [m ks & [default-value]]
  (vec (reduce #(conj %1 (get m %2 default-value)) [] ks)))

(defn index-by
  "Index the collection coll with given keys `ks`.

  Result is a map indexed by the first key
  that contains a map indexed by the second key."
  [ks coll]
  (if (empty? ks)
    (first coll)
    (->> coll
         (group-by (first ks))
         (map (fn [[k v]] [k (index-by (rest ks) v)]))
         (into {}))))

(defn dispatch!
  "Dispatches to the given url."
  [url]
  (set! (.-location js/window) url))

(defn redirect-when-unauthorized [{:keys [status status-text]}]
  (when (= 401 status)
    (if-let [redirect-ongoing (.getItem js/sessionStorage "rems-redirect-ongoing")]
      (do
        ;; NB: When the user logs in and is redirected, it's still possible that the user does not 
        ;; actually have access to the target page and we will get another 401. 
        ;; In this case we don't want to redirect again to the login and the same target page
        ;; so let's clear the redirect-url so the default starting page will be used instead.
        (println "Redirecting to authorization again")
        (.removeItem js/sessionStorage "rems-redirect-ongoing")
        (.removeItem js/sessionStorage "rems-redirect-url"))
      (let [current-url (.. js/window -location -href)]
        (println "Redirecting to authorization from" current-url)
        (.setItem js/sessionStorage "rems-redirect-url" current-url)))
    (dispatch! "/")))
