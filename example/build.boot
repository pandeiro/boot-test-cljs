(set-env!
 :source-paths   #{"src" "test"}
 :resource-paths #{"conf" "html"}
 :dependencies   '[[adzerk/boot-cljs        "0.0-2760-0"     :scope "test"]
                   [pandeiro/boot-test-cljs "0.1.0-SNAPSHOT" :scope "test"]])

(require '[pandeiro.boot-test-cljs :refer [test-cljs]])
