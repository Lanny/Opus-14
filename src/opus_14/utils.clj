(ns opus-14.utils
  (:require
    (clojure [string :as string]
             [pprint :as pprint])
    (cemerick [url :refer (url)])
    [swiss.arrows :refer :all])
  (:import [java.net MalformedURLException]))

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

(defn safe-url
  "Tries to make a cemerick url out of eurl, returns nil if eurl is malformed."
  [eurl]
  (try
    (url eurl)
    (catch MalformedURLException e
      nil)))


(defn domain-of
  "Takes a cemerick url object and return the first and second level domains.
  (domain-of (url \"www.github.com\")) => \"github.com\""
  [eurl]
  (try
    (-<>> eurl
          url
          :host
          (string/split <> #"\.")
          (take-last 2)
          (string/join "."))
    (catch MalformedURLException e
      nil)))

(defn make-absolute
  "Takes a base url and a potentially relative url found at the first url.
  Returns the absolute representation of the second url."
  [base-url rel-url]
  (if (or (not= (type base-url) String)
          (not= (type base-url) String))
    (println "HALOOO" base-url rel-url))
  (try
    (url rel-url)
    (catch MalformedURLException e
      (url base-url rel-url))
    (catch Exception e
      nil)))

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

(defn suspected-document?
  "Takes a url and returns false if it seems likely to point to something other
  than an HTML page (e.x. the url ends in .jpg). true otherwise."
  [eurl]
  (let [working-url (url eurl)]
    (cond
      (and (not= (:protocol working-url) "http")
           (not= (:protocol working-url) "https"))
        false
      (re-matches #"^.*\.(jpeg|jpg|png|gif|css|js)$" (:path working-url))
        false
      :else
        true)))

(defn normalize-url
  [eurl]
  (let [working-url (url eurl)]
    (merge working-url
           {:protocol (if (= (:protocol working-url) "https")
                        "http" (:protocol working-url)) 
            :anchor nil})))

(defn unique-under-fn
  "Returns the first values of coll where no previous value under f are equal:
  user> (unique-under-fn [1 2 3 4 5 6 7] even?)
  (2 1)"
  [f coll]
  (loop [unique-domain '()
         unique-range #{}
         [x & xs] coll]
    (let [f-of-x (f x)
          unique (not (contains? unique-range f-of-x))
          unique-domain (if unique (conj unique-domain x) unique-domain)
          unique-range (if unique (conj unique-range f-of-x) unique-range)]
      (if (seq? xs)
        (recur unique-domain unique-range xs)
        unique-domain))))
