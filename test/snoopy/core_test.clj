(ns snoopy.core-test
  (:require [clojure.test :refer :all]
            [snoopy.core :refer [add-cmd! commands cmd-parse]]))

(defn foo-cmd [_]
  "I am foo!")

(defn reset []
  (reset! commands {}))

(deftest add-cmd!-test
  (reset)
  (add-cmd! "foo" foo-cmd)

  (is (not (nil? (get @commands "foo")))))

(deftest cmd-parse-test
  (reset)
  (add-cmd! "foo" foo-cmd)

  (testing "returns nil if the given string doesn't start with a bang"
    (is (nil? (cmd-parse "I am not a command!"))))

  (testing "returns nil if command is not found"
    (is (nil? (cmd-parse "!not-me")))))
