(ns snoopy.core-test
  (:require [clojure.test :refer :all]
            [snoopy.core :refer [add-cmd! commands]]))

(defn foo-cmd [_]
  "I am foo!")

(defn reset []
  (reset! commands {}))

(deftest add-cmd!-test
  (testing "lockdown string prefix"
    (reset)
    (add-cmd! "foo" foo-cmd)
    (is (not (nil? (get @commands "^foo$"))))))
