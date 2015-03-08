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
           [com.gargoylesoftware.htmlunit WebClient BrowserVersion AjaxController]
           [com.gargoylesoftware.htmlunit.html HtmlPage]
           [java.util.logging Logger Level]))

(defn web-client
  ([]
   (web-client BrowserVersion/FIREFOX_24))
  ([browser]
   (WebClient. browser))
  ([browser proxyserver port]
   (WebClient. browser proxyserver port)))

(defn runner-src [namespaces]
  (let [required-ns (s/join " " namespaces)
        quoted-ns (s/join " " (map #(str "'" %) namespaces))]
    (format "
(ns pandeiro.boot-test-cljs.runner
  (:require
   %s
   [clojure.string :as s]
   [pandeiro.reporter :as reporter]
   [cljs.test :include-macros true]))

(set! *print-fn* reporter/capture-tests)

(defn run []
  (cljs.test/run-tests (assoc (cljs.test/empty-env)
                              :reporter reporter/label)
                       %s))"
     required-ns
     quoted-ns)))

(defn html-page-src [basename]
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

(deftask ^:private scaffold
  "Creates the test runner, compiler config and HTML files needed for testing"
  [b basename   BASE str    "The basename of the files used for tests"
   n namespaces NS   #{sym} "Namespaces whose tests will be run."]
  (let [resource-dir (core/temp-dir!)
        source-dir   (core/temp-dir!)]
    (core/with-pre-wrap fileset
      (file/empty-dir! resource-dir source-dir)
      (let [ns-dir    (doto (io/file source-dir "pandeiro/boot_test_cljs")
                        (.mkdirs))
            runner    (io/file ns-dir "runner.cljs")
            cljs-cfg  (io/file resource-dir (str basename ".cljs.edn"))
            html-page (io/file resource-dir (str basename ".html"))]
         (spit runner (runner-src namespaces))
         (spit cljs-cfg (pr-str {:require  namespaces
                                  :init-fns ['pandeiro.boot-test-cljs.runner/run]}))
         (spit html-page (html-page-src basename))
         (-> (->> fileset
               core/input-files
               (core/by-ext [".cljs.edn"])
               (core/rm fileset))
           (core/add-source source-dir)
           (core/add-resource resource-dir)
           (core/commit!))))))

(deftask ^:private execute
  "Runs the tests and captures the results"
  [b basename  BASE str "The basename of the files used for tests"
   p port      PORT int "The port the test server is running on"]
  (core/with-pre-wrap fileset
    (silence-htmlunit!)
    (let [url (format "http://localhost:%d/%s.html" port basename)
          wc  (web-client)]
      (util/info "<< HtmlUnit connecting to %s... >>\n" url)
      (try
        (.setAjaxController wc (proxy [AjaxController] []
                                 (processSynchron [_, _, _] true)))
        (.getPage wc url)
        (.waitForBackgroundJavaScript wc 60000)
        (catch Exception e
          (let [{:keys [message summary inner]} (extract-test-summary e)]
            (util/info (str message "\n\n"))
            (if (> (apply + (map #(get summary % 1) [:fail :error])) 0)
              (throw (ex-info "Some tests failed or errored" (or summary {})))
              (do
                (util/info "Tests all passing\n\n")
                (util/info (str (pr-str summary) "\n\n"))))))
        (finally
          (util/info "<< Closing all HtmlUnit webclients... >>\n")
          (.closeAllWindows wc))))
    fileset))

(deftask test-cljs
  "Test one or more ClojureScript namespaces by compiling them and checking that
  their tests run without producing any exceptions."
  [n namespaces NS #{sym} "Namespaces whose tests will be run."]
  (let [basename  (str "boot_test_cljs_" (random-str))
        http-port (free-port)]
    (comp
     (scaffold :basename      basename
               :namespaces    namespaces)
     (cljs     :optimizations :none
               :source-map    true)
     (serve    :port          http-port
               :silent        true)
     (execute  :basename      basename
               :port          http-port))))
