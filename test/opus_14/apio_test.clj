(ns opus-14.apio-test
  (:require [clojure.test :refer :all]
            [opus-14.apio :as apio]))

;; WARNING: These tests are for *debugging* and *prototyping*, they hit a 
;; foreign API that may change. They expect certian responses from that API
;; that will change with time. You have been warned: these tests WILL break
;; as time goes on.

(deftest test-first-time-director?
  (is (= (apio/first-time-director? "Jennifer Siebel Newsom") true))
  (is (= (apio/first-time-director? "Steven Spielberg") false)))
