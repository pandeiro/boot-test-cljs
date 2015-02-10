(ns app.tests
  (:require [cljs.test :refer-macros [deftest testing is]]
            [app.main :as app]))

(deftest should-pass
  (testing "addition"
    (is (= (app/add 2 2) 4)))
  (testing "concatenation"
    (is (= (app/add "wat" "wat") "watwat"))))

(deftest should-fail
  (testing "nonsense"
    (is (zero? :zero))))

