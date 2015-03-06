(ns pandeiro.reporter
  (:require [clojure.string :as s]
            [cljs.test :refer [report inc-report-counter! testing-vars-str get-current-env
                               testing-contexts-str]]))

(def test-output (atom []))

(defn capture-tests [x]
  (swap! test-output conj x))

(def label ::basic)

;; these behaviours match [::default *], but we can't dispatch there

(defmethod report [::basic :pass] [m]
  (inc-report-counter! :pass))

(defmethod report [::basic :fail] [m]
  (inc-report-counter! :fail)
  (println "\nFAIL in" (testing-vars-str m))
  (when (seq (:testing-contexts (get-current-env)))
    (println (testing-contexts-str)))
  (when-let [message (:message m)] (println message))
  (println "expected:" (pr-str (:expected m)))
  (println "  actual:" (pr-str (:actual m))))

(defmethod report [::basic :error] [m]
  (inc-report-counter! :error)
  (println "\nERROR in" (testing-vars-str m))
  (when (seq (:testing-contexts (get-current-env)))
    (println (testing-contexts-str)))
  (when-let [message (:message m)] (println message))
  (println "expected:" (pr-str (:expected m)))
  (print "  actual: ") (prn (:actual m)))

(defmethod report [::basic :begin-test-ns] [m]
  (println "\nTesting" (name (:ns m))))

;; Ignore these message types:
(defmethod report [::basic :end-test-ns] [m])
(defmethod report [::basic :begin-test-var] [m]
  #_(println ":begin-test-var" (testing-vars-str m)))
(defmethod report [::basic :end-test-var] [m])

;; Here is where we differ

(defmethod report [::basic :summary] [m]
  ;; regular behaviour
  (println "\nRan" (:test m) "tests containing"
    (+ (:pass m) (:fail m) (:error m)) "assertions.")
  (println (:fail m) "failures," (:error m) "errors.")
  ;; We throw here no matter what in order to export the test summary
  ;; and output back into Clojure via HtmlUnit
  (throw (js/Error. (pr-str {:summary m
                             :message (s/join "\n" @test-output)}))))
