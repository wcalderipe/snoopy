(ns snoopy.slack-rtm-test
  (:require [snoopy.slack-rtm :refer [auth! disconnect! make-connection ws-connect]]
            [clojure.core.async :refer [chan go go-loop timeout <! >! >!! <!!]]
            [clojure.test :refer :all]
            [clj-http.fake :refer [with-fake-routes]]
            [gniazdo.core :as ws]
            [cheshire.core :as json]))

(deftest auth!-test
  (let [token "xoxb-foo-barbaz"]
    (testing "when response body is ok"
      (with-fake-routes {{:address "https://slack.com/api/rtm.connect"
                          :query-params {:token token}} (fn [req] {:status 200
                                                                   :body (json/generate-string {:ok true
                                                                                                :url "wss://foo"})})}
        (is (= (auth! token) "wss://foo"))))

    (testing "when response body is not ok"
      (with-fake-routes {{:address "https://slack.com/api/rtm.connect"
                          :query-params {:token token}} (fn [req] {:status 200
                                                                   :body (json/generate-string {:ok false})})}
        (is (= (auth! token) nil))))))
