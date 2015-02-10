(ns pandeiro.boot-test-cljs
  (:require [clojure.string :as s]
            [clojure.java.io :as io]
            [clojure.stacktrace :as st]
            [clojure.edn :as edn]
            [boot.core :as core :refer [deftask]]
            [boot.util :as util]
            [boot.file :as file]
            [adzerk.boot-cljs :refer [cljs]]
            [pandeiro.boot-http :refer [serve]])
  (:import [java.net ServerSocket]
           [com.gargoylesoftware.htmlunit WebClient BrowserVersion]
           [com.gargoylesoftware.htmlunit.html HtmlPage]
           [java.util.logging Logger Level]))

(defn web-client
  ([]
   (web-client BrowserVersion/FIREFOX_24))
  ([browser]
   (WebClient. browser))
  ([browser proxyserver port]
   (WebClient. browser proxyserver port)))

(defn test-cljs-runner-ns-src [namespaces]
  (let [required-ns (s/join " " namespaces)
        quoted-ns (s/join " " (map #(str "'" %) namespaces))]
    (format "
(ns pandeiro.boot-test-cljs.runner
  (:require
   %s
   [clojure.string :as s]
   [cljs.test :include-macros true]))

(def test-output (atom []))

(defn capture-tests [x]
   (swap! test-output conj x))

(set! *print-fn* capture-tests)

(defn run []
  (let [summary (cljs.test/run-tests (cljs.test/empty-env) %s)]
    ;; We throw here no matter what in order to export the test summary
    ;; and output back into Clojure via HtmlUnit
    (throw (js/Error. (pr-str {:summary summary
                               :message (s/join \"\n\" @test-output)})))))"
     required-ns
     quoted-ns)))

(defn test-cljs-html-page [basename]
  (format
   (str "<!doctype html><html><meta charset=\"utf-8\">"
        "<body><script src=\"%s.js\"></script></body></html>")
   basename))

(defn free-port []
  (let [ss (ServerSocket. 0)
        port (.getLocalPort ss)]
    (.close ss)
    port))

(def js-console-error-re (re-pattern "^Error: (\\{.*\\})"))

(defn extract-test-summary [e]
  (->> e st/root-cause .getMessage
    (re-seq js-console-error-re)
    first second
    edn/read-string))

(defn random-str []
  (apply str (repeatedly 10 #(first (shuffle (range 10))))))

(defn silence-htmlunit! []
  (.setLevel (Logger/getLogger "com.gargoylesoftware.htmlunit") Level/OFF))

(require 'clojure.set)

(deftask test-cljs
  "Test one or more ClojureScript namespaces by compiling them and checking that
  their tests run without producing any exceptions."
  [n namespaces NAMESPACE #{sym} "Namespaces whose tests will be run."]
  (let [rsc-dir   (core/temp-dir!)
        src-dir   (core/temp-dir!)
        basename  (atom "")
        req-ns    (conj namespaces 'pandeiro.test-cljs.runner)
        http-port (free-port)]
    (comp
     (core/with-pre-wrap fileset
       (file/empty-dir! rsc-dir src-dir)
       (reset! basename (str "boot_test_cljs_" (random-str)))
       (let [cljs-main  (io/file rsc-dir (str @basename ".cljs.edn"))
             html-page  (io/file rsc-dir (str @basename ".html"))
             test-cljs-ns-dir (doto (io/file src-dir "pandeiro/boot_test_cljs")
                                (.mkdirs))
             cljs-test-runner (io/file test-cljs-ns-dir "runner.cljs")]
         (spit cljs-test-runner (test-cljs-runner-ns-src namespaces))
         (spit cljs-main (pr-str {:require  namespaces
                                  :init-fns ['pandeiro.boot-test-cljs.runner/run]}))
         (spit html-page (test-cljs-html-page @basename))
         (-> (->> fileset
               core/input-files
               (core/by-ext [".cljs.edn"])
               (core/rm fileset))
           (core/add-source src-dir)
           (core/add-resource rsc-dir)
           (core/commit!))))
     (do
       ;;(swap! util/*verbosity* + (* -1 @util/*verbosity*)) ; turn off cljs output
       (cljs :optimizations :none, :source-map true))
     (do
       ;;(swap! util/*verbosity* + (* -1 @util/*verbosity*)) ; turn output back on
       (serve :dir "target" :port http-port))
     (core/with-pre-wrap fileset
       (silence-htmlunit!)
       (let [test-page-url (format "http://localhost:%d/%s.html" http-port @basename)
             wc            (web-client)]
         (util/info "<< HtmlUnit connecting to %s... >>\n" test-page-url)
         (try
           (.getPage wc test-page-url)
           (catch Exception e
             (let [{:keys [message summary inner]} (extract-test-summary e)]
               (util/info (str message "\n\n"))
               ;;(println inner)
               (when (> (apply + (map summary [:fail :error])) 0)
                 (throw (ex-info "Some tests failed or errored" summary)))))
           (finally
             (util/info "<< Closing all HtmlUnit webclients... >>\n")
             (.closeAllWindows wc))))
       fileset))))

