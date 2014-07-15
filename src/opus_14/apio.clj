(ns opus-14.apio
  (:require
    [clojure.data.json :as json]
    [org.httpkit.client :as http]
    (korma [core :as k])
    (opus-14 [entities :as e]
             [utils :as utils])))

(def loid (keyword "last_insert_rowid()"))

(defn maf-query
  "Queries myapifilms.com using params. Returns a delay of the first item in
  the parsed JSON response."
  [params]
  (let [resp (http/get "http://www.myapifilms.com/imdb"
                       {:query-params params})]
    (delay
      (let [{:keys [body]} @resp]
        (first (json/read-str body :key-fn keyword))))))

(defn maf-actor-by-name
  "Looks up actor (or director) infromation from myapifilms.com given a name
  and returns the first result. Return value is a delay that derefrences to a 
  map."
  ([actor-name]
   (maf-actor-by-name actor-name {}))
  ([actor-name params]
   (let [defaults {:bornDied "0" 
                   :format "JSON"
                   :filmography 1
                   :name actor-name
                   :limit 1}
         working-params (merge defaults params)]
     (maf-query working-params))))

(defn maf-film-by-name
  "Looks up film information from myapifilms.com fiven a film name. Returns the
  first result. Return value is a delay that derefrences to a map."
  ([film-name]
   (maf-film-by-name film-name {}))
  ([film-name params]
    (let [defaults {:format "JSON"
                    :actors "F"
                    :aka 1
                    :filter "M"
                    :title film-name
                    :limit 1}
          working-params (merge defaults params)]
      (maf-query working-params))))

(def maf-film-f2f
  {:idIMDB [identity :idIMDB]
   :plot [identity :plot]
   :title [identity :title]
   :urlPoster [identity :urlPoster]
   :year [utils/parse-int :year]})

(def maf-cast-actor-f2f
  "f2f for the `actors` list on MAF film queries."
  {:actorId [identity :idIMDB]
   :actorName [identity :name]
   :urlPhoto [identity :urlPhoto]})

(def maf-writer-director-f2f
  "f2f for the `directors` and `writers` entries on MAF film queries"
  {:idIMDB [identity :idIMDB]
   :name [identity :name]})

(defn distribute-maf-film-result!
  "Takes a myapifilms result and records it in the database, creating
  relationships and record stubs. Returns the id of the new film record."
  [query-result]
  (let [base-record (utils/field-to-field query-result maf-film-f2f)
        film-id (-> (k/insert* e/films)
                    (k/values base-record)
                    (k/exec)
                    (loid))
        ;; Stick all our `actors` (by the internal definition) into one list
        ;; and annotate them with a :role key that will be stripped out later
        actors (concat (for [actor (:actors query-result)]
                         (assoc (utils/field-to-field actor maf-cast-actor-f2f)
                                :role "actor"))
                       (for [director (:directors query-result)]
                         (assoc (utils/field-to-field director 
                                                      maf-writer-director-f2f)
                                :role "director"))
                       (for [writer (:writers query-result)]
                         (assoc (utils/field-to-field writer 
                                                      maf-writer-director-f2f)
                                :role "writer")))]
    (doall
      (for [actor actors]
        (let [actor-id (-> (k/select* e/actors)
                           (k/where {:idIMDB (:idIMDB actor)})
                           (k/limit 1)
                           (k/exec)
                           (first)
                           (:id))
              ;; If the actor isn't already in the database add them and
              ;; set actor-id to the new record id
              actor-id (if (nil? actor-id)
                         (loid
                           (k/insert e/actors 
                             (k/values (dissoc actor :role))))
                         actor-id)]
          (k/insert e/credits
            (k/values {:films_id film-id
                       :actors_id actor-id
                       :role (:role actor)})))))
    film-id))

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
