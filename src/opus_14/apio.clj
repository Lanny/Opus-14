(ns opus-14.apio
  (:require
    (clojure [pprint :as pprint])
    (clojure.data [json :as json])
    [org.httpkit.client :as http]
    (korma [core :as k])
    (twitter [oauth :as oauth])
    (twitter.api [restful :as twitter-rest])
    (cemerick [url :refer (url)])
    (net.cgrand [enlive-html :as enlive])
    (opus-14 [entities :as e]
             [utils :as utils]))
  (:use environ.core))

(def loid (keyword "last_insert_rowid()"))

(def tw-credentials 
  (apply oauth/make-oauth-creds ((juxt :tw-api-key :tw-api-secret
                                       :tw-access-token :tw-access-secret)
                                 env)))

;(def kek (twitter-rest/users-search :oauth-creds tw-credentials
;                               :params {:q "Miss Representation"}))

;(-> kek :body (get 1) pprint/pprint)

(defn indiegogo-url->film-record
  "Give me a indiegog url and I'll give you a record ready to be entered into
  the `films` table. Validates the url and scrapes indiegogo.com so naturally
  a lot of shit can happen here. Returns a [record err] vector. `record` is 
  only meaningful if `err` is nil. If `err` is not nil it's a map with at least
  :code and :reason keys, indicating a unique error and a human readable 
  message respectively."
  [campaign-url]
  (let [u (url campaign-url)]
    (cond
      (not (= (utils/domain-of u) "indiegogo.com"))
        [nil
         {:code :wronghost
          :reason "URLs must point to indiegogo.com"}]
      (not (re-matches #"/projects/[^/ ]+/?" (:path u)))
        [nil
         {:code :wrongpath
          :reason "URL didn't pick out a project page"}]
      :else
        ; IGG loads the base of the page (header and fundraising goals) first
        ; and then uses XHR to load the seperate tabs (which also contain
        ; info that we need). We'll start them both at once to cut down on
        ; latency.
        (let [base-resp (http/get (str u))
              home-resp (http/get (str (url u "show_tab" "home"))
                                  {:headers {"x-requested-with" 
                                             "XMLHttpRequest"}})
              err-resp (first (filter #(not= (:status @%) 200)
                                      [base-resp home-resp]))]
          (if-not (nil? err-resp)
            (do (println @err-resp)
            [nil {:code :http-error 
                  :http-code (:status @err-resp)
                  :reason (case (:status @err-resp)
                            404 "Indiegogo campaign not found"
                            500 "Indiegogo server error, retry maybe"
                            (format "HTTP error %d" (:status @err-resp)))}])
            (let [base-body (enlive/html-snippet (:body @base-resp))
                  home-body (enlive/html-snippet (:body @home-resp))
                  film-title (-> base-body
                                 (enlive/select 
                                   [[:meta (enlive/attr= :property "og:title")]])
                                 first
                                 :attrs
                                 :content)
                  crew (enlive/select home-body [:ul.i-team-members :.i-info])
                  director-name (-> crew
                                    first ; First crew member
                                    (enlive/select [:.i-name])
                                    first ; First name element (there's only one)
                                    :content
                                    first) ; First child (text) node
                  ext-links (enlive/select home-body
                                           [:.i-external-links :.link])
                  fb-link (-> (utils/first-with-content "Facebook" ext-links)
                              :attrs
                              :href)
                  tw-link (-> (utils/first-with-content "Twitter" ext-links)
                              :attrs
                              :href)
                  ex-link (-> (utils/first-with-content "Website" ext-links)
                              :attrs
                              :href)]
              [{:director_name director-name
                :title film-title
                :tw-link tw-link
                :fb-link fb-link
                :ex-link ex-link}
               nil] ; We golang now
              )
            )
          )
      )
    )
  )

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
   :title [identity :maf_title]
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


;(first-time-director? "Jennifer Siebel Newsom")

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
