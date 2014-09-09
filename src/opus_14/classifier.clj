(ns opus-14.classifier
  "A simple naive bayes classifier and word based feature extractor."
  (:require
    (clojure [set :as sets]
             [string :as string])
    (clojure.data [json :as json])
    [swiss.arrows :refer :all]))

(def movie-classifier
  (delay
    (let [training-data (-> "resources/training_data.json"
                            slurp
                            json/read-str)]
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
           (into {} <>)))))

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
    "you'll" "you're" "you've" "your" "yours" "yourself" "yourselves"})

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
        (filter #(not (contains? stop-words %)))))

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
  (let [klass (-> classifier :classes (get klass-name))
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

(defn get-movie-classifier
  []
  )
