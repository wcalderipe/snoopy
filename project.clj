(defproject snoopy "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/core.async "0.4.490"]
                 [clj-http "3.9.1"]
                 [cheshire "5.8.1"]
                 [stylefruits/gniazdo "1.1.1"]]
  :main ^:skip-aot snoopy.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
