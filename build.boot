(set-env!
 :source-paths   #{"src"}
 :dependencies   '[[adzerk/boot-cljs                  "0.0-2760-0" :scope "test"]
                   [pandeiro/boot-http                "0.6.2"      :scope "test"]
                   [adzerk/bootlaces                  "0.1.10"     :scope "test"]
                   [net.sourceforge.htmlunit/htmlunit "2.15"       :scope "test"]])

(require
 '[adzerk.bootlaces :refer [bootlaces!]]
 '[pandeiro.boot-test-cljs :refer [test-cljs]])

(def +version+ "0.1.0-SNAPSHOT")

(bootlaces! +version+)

(task-options!
 pom {:project     'pandeiro/boot-test-cljs
      :version     +version+
      :description "Boot task to test ClojureScript namespaces"
      :url         "https://github.com/pandeiro/boot-test-cljs"
      :scm         {:url "https://github.com/pandeiro/boot-test-cljs"}
      :license     {"Eclipse Public License" "http://www.eclipse.org/legal/epl-v10.html"}})
