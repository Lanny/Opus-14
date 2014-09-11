(ns opus-14.core-test
  (:require [clojure.test :refer :all]
            [opus-14.core :refer :all]
            [opus-14.classifier :as clsr]))

(deftest test-normalize
  (is (every? (partial = 1/3)
              (vals (clsr/normalize {:a 1 :b 1 :c 1}))))
  (is (= {:a 1/10 :b 2/10 :c 7/10}
         (clsr/normalize {:a 1 :b 2 :c 7})))
  (is (= {:a 1/10 :b 2/10 :c 7/10}
         (clsr/normalize {:a 1/100 :b 2/100 :c 7/100})))
  (is (= 1 (->> (for [i (range 10)] [(str "hai-" i) (rand-int 100)])
                (into {})
                clsr/normalize
                vals
                (reduce +)))))

(def sent-classifier
  (-> (clsr/make-classifier [:pos :neg])
      (clsr/train :pos ["squids" "love" "squids" "life"])
      (clsr/train :pos ["puppies" "cute"])
      (clsr/train :pos ["love" "you" "jesus" "christ"])
      (clsr/train :neg ["hate" "squids"])
      (clsr/train :neg ["hitler" "godwin" "evil" "holocaust"])
      (clsr/train :neg ["hate" "evil" "fascists"])))

(deftest lame-train-test
  (is (-> sent-classifier :observations pos?))
  (is (-> sent-classifier :classes :pos :observations pos?))
  (is (-> sent-classifier :classes :neg :observations pos?)))

(deftest slurpage-test
  (is @clsr/movie-classifier)
  (is (map? @clsr/movie-classifier))
  (println (clsr/benchmark-classifier "resources/training_data.json")))

(deftest classification-test
  (->> #{"jesus" "is" "love"}
       (clsr/prob-dist sent-classifier)
       :pos
       (> 1/2))

  (->> #{"jesus" "is" "hate"}
       (clsr/prob-dist sent-classifier)
       :neg
       (> 1/2)))
