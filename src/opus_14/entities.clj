(ns opus-14.entities
  (:require
    (korma [db :refer :all]
           [core :refer :all])))

(def base-db 
  {:classname "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname "fourteen.db"})

(defdb korma-db base-db)
(declare actors films credits)

(defentity credits
  (belongs-to actors)
  (belongs-to films))

(defentity actors
  (has-many credits))

(defentity films
  (has-many credits))
