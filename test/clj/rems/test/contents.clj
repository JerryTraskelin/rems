(ns rems.test.contents
  (:require [clojure.test :refer :all]
            [hiccup-find.core :refer :all]
            [rems.context :as context]
            [rems.contents :refer :all]))

;; TODO: factor out if needed elswwhere
(use-fixtures
  :once
  (fn [f]
    (binding [context/*tempura* (fn [[k]] (str k))]
      (f))))

(deftest test-catalogue-item
  (testing "catalogue item with urn"
    (let [urn "http://urn.fi/urn:nbn:fi:lb-201403262"
          c (catalogue-item {:title "U" :resid urn})
          link (first (hiccup-find [:a] c))]
      (is (= :a (first link)) "is a link")
      (is (= urn (:href (second link))) "links to the urn")
      (is (= :_blank (:target (second link))) "opens in new tab"))))

(defn check-row-text [row text]
  (is (= text (hiccup-text (first (hiccup-find [:td] row))))))

(deftest test-catalogue-list
  (let [c (catalogue-list [{:title "B"} {:title "A"} {:title "C"}])
        rows (rest (hiccup-find [:tr] c))]
    (is (= 3 (count rows)))
    (check-row-text (nth rows 0) "A")
    (check-row-text (nth rows 1) "B")
    (check-row-text (nth rows 2) "C")))

(deftest test-cart-list
  (let [c (cart-list [{:title "D"} {:title "C"}])
        rows (rest (hiccup-find [:tr] c))]
    (is (= 2 (count rows)))
    (check-row-text (first rows) "C")
    (check-row-text (second rows) "D")))