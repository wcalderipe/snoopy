(ns snoopy.slack
  (:require [clojure.core.async :as async :refer [>! <! close! chan pub sub go go-loop]]
            [clj-http.client :as client]
            [gniazdo.core :as ws]))

;; Stablish a connectin with Slack RTM via WebSocket, messages will have
;; two directions incoming and outcoming which will be handled internally
;; by channels.
;;
;; Goals
;; - Fetch a valid WebSocket URL from Slack API
;; - Provide a PubSub mechanism to internal consumers subscribe for
;;   Slack events by type
;; - Provide an abstraction to emit events to Slack API

(def excluded-event-types #{"desktop_notification" "user_typing"})

(defn- slack-connect
  [token]
  (:body (client/get "https://slack.com/api/rtm.connect" {:query-params {:token token}
                                                          :as
                                                          :json})))

(defn- receive-handler
  [incoming-ch]
  (fn [raw-message]
    (let [message (json/parse-string raw-message true)]
      (when-not (contains? excluded-event-types (:type message))
        (go (>! incoming-ch message))))))

(defn- ws-connect
  [url]
  (let [incoming-ch (chan)
        socket      (ws/connect url :on-receive (receive-handler incoming-ch))
        publication (pub incoming-ch #(:type %))]
    {:incoming-ch incoming-ch
     :socket socket
     :publication publication}))

(defn sub-to-event
  [publication type ch]
  (sub publication type ch))

(defn start!
  token
  (let [ws-url (:url (slack-connect token))
        conn   (ws-connect ws-url)]
    conn))

(defn close-connection
  [conn]
  (close! (:incoming-ch conn))
  (ws/close (:socket conn)))

(comment
  (def token "xoxb-foo-bar-baz")

  (defn build-message-channel
    []
    (let [ch (chan)]
      (go-loop []
        (when-let [message (<! ch)]
          (println "message" message)
          (recur)))
      ch))

  (def conn (start! token))
  (close-connection conn)
  (sub-to-event (:publication conn) "message" (build-message-channel)))
