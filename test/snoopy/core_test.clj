(ns snoopy.core-test
  (:require [clojure.test :refer :all]
            [snoopy.core :as bot]))

(deftest render-test
  (testing "with nil"
    (is (nil? (bot/render nil))))

  (testing "with string"
    (is (= (bot/render "foo") {:body "foo"})))

  (testing "with map"
    (is (= (bot/render {:body "I'm foo!"}) {:body "I'm foo!"}))))

(deftest commands-test
  (let [combined-handler (bot/commands (bot/make-command "!foo" (fn [_] "foo"))
                                       (bot/make-command "!bar" (fn [_] "bar")))]

    (testing "matches the proper handler to the given message"
      (is (= (combined-handler {:body "!foo"}) {:body "foo"}))
      (is (= (combined-handler {:body "!bar"}) {:body "bar"}))
      (is (= (combined-handler {:body "i don't have a match :("}) nil)))))
