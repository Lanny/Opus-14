(ns opus-14.utils)

(defn parse-int [value]
  "Returns the integer represented in base 10 as a string argument, or nil if
  no integer is represented."
  (try
    (Integer/parseInt value)
    (catch NumberFormatException e nil)))

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
