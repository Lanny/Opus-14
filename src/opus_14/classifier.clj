(ns opus-14.classifier
  "A simple naive bayes classifier and word based feature extractor."
  (:require
    (clojure [set :as sets]
             [string :as string]
             [pprint :refer :all])
    (clojure.data [json :as json])
    [swiss.arrows :refer :all]))

(declare train tokenize make-classifier)

(defn pnr [a] (println a) a)

(defn classifier-from-data
  [training-data]
   (-<> training-data
        keys
        make-classifier
        (reduce (fn [accum1 [klass data]]
                  (reduce (fn [accum2 datum]
                            (train accum2 klass
                                   (tokenize datum)))
                          accum1
                          data))
                <> ; empty classifier goes here
                training-data)
        (into {} <>)))

(def movie-classifier
  (delay
    (->> "resources/training_data.json"
         slurp
         json/read-str
         (map (fn [[k v]] [k (filter (comp not empty?) v)]))
         (into {})
         classifier-from-data)))

(def stop-words
  #{"" "a" "about" "above" "after" "again" "against" "all" "am" "an" "and"
    "any" "are" "aren't" "as" "at" "be" "because" "been" "before" "being"
    "below" "between" "both" "but" "by" "can't" "cannot" "could" "couldn't"
    "did" "didn't" "do" "does" "doesn't" "doing" "don't" "down" "during" "each"
    "few" "for" "from" "further" "had" "hadn't" "has" "hasn't" "have" "haven't"
    "having" "he" "he'd" "he'll" "he's" "her" "here" "here's" "hers" "herself"
    "him" "himself" "his" "how" "how's" "i" "i'd" "i'll" "i'm" "i've" "if" "in"
    "into" "is" "isn't" "it" "it's" "its" "itself" "let's" "me" "more" "most"
    "mustn't" "my" "myself" "no" "nor" "not" "of" "off" "on" "once" "only" "or"
    "other" "ought" "our" "oursourselves" "out" "over" "own" "same" "shan't"
    "she" "she'd" "she'll" "she's" "should" "shouldn't" "so" "some" "such"
    "than" "that" "that's" "the" "their" "theirs" "them" "themselves" "then"
    "there" "there's" "these" "they" "they'd" "they'll" "they're" "they've"
    "this" "those" "through" "to" "too" "under" "until" "up" "very" "was"
    "wasn't" "we" "we'd" "we'll" "we're" "we've" "were" "weren't" "what"
    "what's" "when" "when's" "where" "where's" "which" "while" "who" "who's"
    "whom" "why" "why's" "with" "won't" "would" "wouldn't" "you" "you'd"
    "you'll" "you're" "you've" "your" "yours" "yourself" "yourselves" "written"
    "film" "documentary" "-" "anonymous" "also"})

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

(defn tokenize
  "A primitive word-wise tokenization algorithm."
  [s]
  (-<>> s
        (string/lower-case)
        (string/split <> #"\s+")
        (map #(second (re-find #"^['\"]*(.+?)[.,?!'\"]*$" %)))
        (filter #(not (nil? %)))
        (filter #(not (contains? stop-words %)))
        set))

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
       (Math/log (+ (:observations klass) 2)))))

(defn pC|F
  "Given a classifier, class(name), and a coll of features, return the
  probability that the object with those features is in that class"
  [classifier klass-name feature-set]
  (let [klass (-> classifier :classes (get klass-name))
        feature-set (sets/intersection (:features klass) feature-set)
        pC (/ (:observations klass)           ; p that anything is in C before
              (:observations classifier))     ; evidence is examiled

        pF|C (-<> (prob-of-X klass)           ; p of these features occuring
                  (map feature-set)           ; a member known to be in C
                  (reduce + 1 <>))

        pF (-<> (prob-of-X classifier)        ; p of these features occuring
                (map feature-set)             ; at all
                (reduce * 1 <>))]

    ; ( prior * likelihood ) / evidence
    ; http://en.wikipedia.org/wiki/Naive_Bayes_classifier#Probabilistic_model
    ;(println (format "klass: %s\n pC: %f\n pF|C: %f\n pF: %f"
    ;                 klass-name (Math/log pC) (Math/log pF|C) (Math/log pF)))
    (/ (* pC pF|C) pF)))

(defn prob-dist
  "Given a classifier and a coll of features returns a map of possible classes
  and the probabibility of an object with the given feature set being a member
  of that class (per the NB model)"
  [classifier feature-set]
  (->> (keys (:classes classifier))
       (map (fn [klass] [klass (double (pC|F classifier klass feature-set))]))
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

(defn benchmark-classifier
  "Takes a file path (string) to training data (json in the format of:
    {className: [exampleString1, exampleString2]}
  ) and uses 80% of the data as training data and tests for correct
  classification of the remaining 20%. Returns the percent correctly classified"
  [data-source]
  (let [data (->> data-source
                  slurp
                  json/read-str
                  (map (fn [[k v]] [k (filter (comp not empty?) v)]))
                  (into {}))
        training-data (->> data
                           (map (fn [[k v]]
                                  [k (take (* (count (data k)) 4/5) v)]))
                           (into {}))
        testing-data (->> data
                          (map (fn [[k v]]
                                 [k (take (* (count (data k)) 1/5) v)]))
                           (into {}))
        csfr (classifier-from-data training-data)]
    (-<>> testing-data
          (map (fn [[klass examples]]
                 (println (format "--- %s : %d" klass (count examples)))
                 (map (fn [example]
                        (if (->> example
                                 tokenize
                                 (most-likely-class @movie-classifier)
                                 first
                                 (= klass))
                          1 0))
                      examples)))
          doall
          pnr
          (map #(/ (reduce + %) (count %)))
          (reduce +)
          (/ <> (count (keys testing-data)))
          float)))
