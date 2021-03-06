(ns rems.test.theming
  (:require [clojure.test :refer :all]
            [rems.context :as context]
            [rems.util :as util]))

(defn read-from-theme-file
  [filename]
  (read-string (slurp (str "resources/themes/" filename ".edn"))))

(deftest test-theme-loading
  (is (= (merge (read-from-theme-file "default") (read-from-theme-file "lbr")) (context/load-theme "lbr")) "Should load the default theme combined with overridden values from the given theme.")
  (is (= (read-from-theme-file "default") (context/load-theme "non-existent")) "Given a non-existent theme the default one should be returned.")
  (is (= (read-from-theme-file "default") (context/load-theme nil))))

(deftest test-theme-attributes
  (binding [context/*theme* {:test "success"
                             :test-color 2}]
    (is (= 2 (util/get-theme-attribute :test-color)))
    (is (= "success" (util/get-theme-attribute :test)))
    (is (nil? (util/get-theme-attribute :no-such-attribute)))))
