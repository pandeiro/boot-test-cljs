(ns app-tests
  (:require [cljs.test :refer-macros [deftest testing is]]))

(deftest should-pass
  (testing "equality"
    (is (= 2 2))))

(deftest should-fail
  (testing "equality"
    (is (= 2 3))))


