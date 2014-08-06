(ns opus-14.apio-test
  (:require [clojure.test :refer :all]
            [opus-14.apio :as apio]))

(deftest test-igg-fetchage
  ;; Just test we get the expected errors codes, don't actually hit the
  ;; foreign host
  (let [[res1 err1] (apio/indiegogo-url->film-record "http://google.com")]
    (is (not (nil? err1)))
    (is (= (:code err1) :wronghost))
    (is (string? (:reason err1)))))

;; WARNING: These tests are for *debugging* and *prototyping*, they hit a 
;; foreign API that may change. They expect certian responses from that API
;; that will change with time. You have been warned: these tests WILL break
;; as time goes on.

;; (deftest test-first-time-director?
;;   (is (= (apio/first-time-director? "Jennifer Siebel Newsom") true))
;;   (is (= (apio/first-time-director? "Steven Spielberg") false)))

(deftest test-igg-4-realz
  (let [[res1 err1] (apio/indiegogo-url->film-record 
                      (str "https://www.indiegogo.com/projects/"
                           "gray-area-wolves-of-the-southwest"))]
    (println res1)
    (println err1)))

(test-igg-4-realz)
