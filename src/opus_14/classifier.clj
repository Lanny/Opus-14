(ns opus-14.classifier
  "A simple naive bayes classifier and word based feature extractor."
  (:require
    (clojure [set :as sets])
    [swiss.arrows :refer :all]))

(defn normalize
  "Given a map m with all numeric values, return a map n where the sum of
  values is (approximately, in the case of floating point input) 1 and where
  (m x) is proportional to (n x) for all values of x."
  [m]
  (let [denom (reduce + 0 (vals m))]
    (if (= 0 denom)
      m ; Sums to zero, all vals are probably zero, just give them the orig
        ; map back I guess
      (into {} (for [[k v] m]
                 [k (/ v denom)])))))

(defn make-classifier
  "Returns an untrained classifier with `klasses` as the potential classes"
  [klasses]
  {:observations 0
   :classes (reduce
             (fn [accum klass]
               (assoc accum klass { :observations 0 :features {} }))
             {}
             klasses)
   :features {}})

(defn train
  "Takes a classifier, class, and coll of features extracted from a piece of
  training data. Returns a new classifier that incorperates this new data."
  [classifier klass feature-set]
  (let [intermed (-> classifier
                     (update-in [:observations] + (count feature-set))
                     (update-in [:classes klass :observations]
                                + (count feature-set)))]
    (reduce (fn [accum feature]
              (-> accum
                  (update-in [:classes klass :features feature] (fnil inc 0))
                  (update-in [:features feature] (fnil inc 0))))
            intermed
            feature-set)))

(defn prob-of-X
  "Returns a function that, when called with a single argument `feature`,
  returns p(feature|K) where K is the argument to this function."
  [klass]
  (fn [feature]
    (/ (inc (get-in klass [:features feature] 0))
       (+ (:observations klass) (count (:features klass))))))

(defn pC|F
  "Given a classifier, class(name), and a coll of features, return the
  probability that the object with those features is in that class"
  [classifier klass-name feature-set]
  (let [klass (-> classifier :classes klass-name)
        pC (/ (:observations klass)           ; p that anything is in C before
              (:observations classifier))     ; evidence is examiled

        pF|C (-<> (prob-of-X klass)           ; p of these features occuring
                  (map feature-set)           ; a member known to be in C
                  (reduce * 1 <>))

        pF (-<> (prob-of-X classifier)        ; p of these features occuring
                (map feature-set)             ; at all
                (reduce * 1 <>))]

    ; ( prior * likelihood ) / evidence
    ; http://en.wikipedia.org/wiki/Naive_Bayes_classifier#Probabilistic_model
    ; (println (format "klass: %s\n pC: %s\n pF|C: %s\n pF: %s"
    ;                  klass-name pC pF|C pF))
    (/ (* pC pF|C) pF)))

(defn prob-dist
  "Given a classifier and a coll of features returns a map of possible classes
  and the probabibility of an object with the given feature set being a member
  of that class (per the NB model)"
  [classifier feature-set]
  (->> (keys (:classes classifier))
       (map (fn [klass] [klass (float (pC|F classifier klass feature-set))]))
       (into {})
       (normalize)))

(defn most-likely-class
  "Returns the name of the class the object holding these features most likely
  belong to."
  [classifier feature-set]
  (->> (prob-dist classifier feature-set)
       (into [])
       (sort-by second >)
       (first)))
