(defproject opus-14 "0.1.0-SNAPSHOT"
  :description "A system for assessing the likely impact of social justice films."
  :url "https://github.com/RyanJenkins/Opus-14"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.xerial/sqlite-jdbc "3.7.15-M1"]
                 [http-kit "2.1.16"]
                 [org.clojure/data.json "0.2.5"]
                 [korma "0.3.0"]
                 [environ "0.5.0"]]
  :plugins [[lein-environ "0.5.0"]]
  :main ^:skip-aot opus-14.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
