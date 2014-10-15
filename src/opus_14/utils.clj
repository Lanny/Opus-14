(ns opus-14.utils
  (:require
    (clojure [string :as string]
             [pprint :as pprint])
    [swiss.arrows :refer :all]))

(defn parse-int [value]
  "Returns the integer represented in base 10 as a string argument, or nil if
  no integer is represented."
  (try
    (Integer/parseInt value)
    (catch NumberFormatException e nil)))

(defn pnr
  "Print 'n Return. Prints its single arg and returns it. Useful for
  understanding arrow chains"
  [x]
  (pprint/pprint x)
  x)

(defn field-to-field
  "Takes a base map and a 'f2f-map' which describes a transformation on the
  base map and returns a new map. The f2f map takes the form of: 
  {source-key [transform dest-key]} where source-key is a key on the base map,
  dest-key is a key on the returned map, and transform is a function that take
  the value at source-key on the base map and returns the future value of 
  dest-key on the returned map."
  [base f2f-map]
  (into {}
    (filter identity 
      (for [[source-key [transform dest-key]] f2f-map]
        (let [v (transform (base source-key))]
          (if v
            [dest-key v]
            nil))))))

(defn domain-of
  "Takes a cemerick url object and return the first and second level domains.
  (domain-of (url \"www.github.com\")) => \"github.com\""
  [cemerick-url]
  (-<>> cemerick-url
        :host
        (string/split <> #"\.")
        (take-last 2)
        (string/join ".")))

(defn first-with-content
  "Takes a sequence of enlive stlye XML nodes and returns the first where the
  only content is the supplied `content` arg."
  [content enlive-coll]
  (first
    (filter
      (fn [node]
        (and (= (count (:content node)) 1)
             (= (first (:content node)) content)))
      enlive-coll)))
