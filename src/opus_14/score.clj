(ns opus-14.score
  (:require
    (clojure [pprint :as pprint])
    (korma [core :as k])
    (opus-14 [entities :as e]
             [apio :as apio]
             [classifier :as clsr]
             [utils :as utils])))

(defn log7+1
  "Returns base 7 log of x plus one."
  [x]
  (inc (/ (Math/log x) (Math/log 7))))


(def test-record
  {:title "Miss Representation"
   :twitter_username "RepresentPledge"
   :director "Jennifer Siebel Newsom"
   :website_url "http://therepresentationproject.org/films/miss-representation/"
   :facebook_url "https://www.facebook.com/MissRepresentationCampaign"
   :description (str "Explores the under-representation of women in positions "
                     "of power and influence in America, and challenges the "
                     "media's limited portrayal of what it means to be a "
                     "powerful woman.")})

(defn director-experience-score
  [maf-record]
  (if (nil? maf-record)
    0
    (let [filmogs (:filmographies maf-record)
          director-credits (some #(if (= (:section %) "Director") %) filmogs)
          other-credits (some #(if (not (= (:section %) "Director")) %) filmogs)
          director-count (count (:filmography director-credits))]
      (if-not (pos? director-count)
        (if (pos? (count other-credits)) 1/2 0)
        (-> (log7+1 director-count)
            (min 2)
            (max 0))))))

(defn populate-film-record
  [film-record]
  (let [maf-record @(apio/maf-actor-by-name (:director_name film-record))
        first-tw (apio/first-twitter-result? (:twitter_username film-record)
                                             (:title film-record))
        _ (println "1")
        first-fb (apio/first-facebook-result? (:facebook_url film-record)
                                              (:title film-record))
        _ (println "2")
        klout-score (apio/twitter-screen-name->klout-score
                      (:twitter_username film-record))
        _ (println "3")
        extralinkage (if (nil? (:website_url film-record))
                       0
                       (count (apio/reciprocal-linkers
                                (:website_url film-record))))
        _ (println "4")
        [category prob] (->> film-record
                             (:description)
                             (clsr/tokenize)
                             (clsr/most-likely-class @clsr/movie-classifier))]
        _ (println "5")
    (merge film-record
           {:director_experience (director-experience-score maf-record)
            :first_twitter_result (if first-tw 1 0)
            :first_facebook_result (if first-fb 1 0)
            :klout_score klout-score
            :extralinkage extralinkage
            :category category
            :ambiguous_classification (if (< prob 0.4) 1 0)})))

(defn compute-score
  [film-record]

  )
