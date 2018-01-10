(defproject mujic2 "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [ [org.clojure/clojure "1.8.0"]
                  [overtone "0.10.3"]
                  [leipzig "0.10.0"] ]
  :main ^:skip-aot mujic2.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
