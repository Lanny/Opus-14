(ns opus-14.apio
  (:require
    [clojure.data.json :as json]
    [org.httpkit.client :as http]))

(defn imdb-fetch-name
  ""
  [cast-name]
  (let [resp (http/get "http://www.imdb.com/xml/find"
                       {:query-params {:json "1" :nr "1"
                                       :nm "on" :q cast-name}})]
    (delay
      (let [{:keys [body]} @resp]
        (json/read-str body :key-fn keyword)))))

(defn maf-actor-lookup
  "Looks up actor (or director) infromation from myapifilms.com given a IMDb
  actor ID."
  ([imdb-id]
   (maf-actor-lookup imdb-id {}))
  ([imdb-id params]
   (let [defaults {:bornDied "0" :format "JSON" :filmography 1}
         working-params (merge defaults params)
         resp (http/get "http://www.imdb.com/xml/find"
                        {:query-params working-params})]
    (delay
      (let [{:keys [body]} @resp]
        (json/read-str body :key-fn keyword))))))

(defn maf-actor-by-name
  "Looks up actor (or director) infromation from myapifilms.com given a name
  and returns the first result."
  ([actor-name]
   (maf-actor-by-name actor-name {}))
  ([actor-name params]
   (let [defaults {:bornDied "0" :format "JSON" :filmography 1 
                   :name actor-name :limit 1}
         working-params (merge defaults params)
         resp (http/get "http://www.myapifilms.com/imdb"
                        {:query-params working-params})]
    (delay
      (let [{:keys [body]} @resp]
        (first (json/read-str body :key-fn keyword)))))))

(defn first-time-director?
  "Given the name of a director, returns true if the director is credited as a
  director two or more times, false otherwise"
  [director-name]
    (let [filmogs (:filmographies @(maf-actor-by-name director-name))
          director-credits (some #(if (= (:section %) "Director") %) filmogs)]
      (< (count (:filmography director-credits)) 2)))

;(first-time-director? "Jennifer Siebel Newsom")

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
