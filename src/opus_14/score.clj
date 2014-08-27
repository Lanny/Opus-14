(ns opus-14.score
  (:require
    (clojure [pprint :as pprint])
    (korma [core :as k])
    (opus-14 [entities :as e]
             [utils :as utils])))

(defn log7+1
  "Returns base 7 log of x plus one."
  [x]
  (inc (/ (Math/log x) (Math/log 7))))

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

(defn compute-score [film-record]
  ""
  )
