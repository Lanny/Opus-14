(defproject opus-14 "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.xerial/sqlite-jdbc "3.7.15-M1"]
                 [http-kit "2.1.16"]
                 [org.clojure/data.json "0.2.5"]
                 [korma "0.3.0"]]
  :main ^:skip-aot opus-14.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
