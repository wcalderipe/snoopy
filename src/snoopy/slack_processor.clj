(ns snoopy.slack-processor
  (:require [clojure.core.async :refer [>! <! chan go go-loop]]
            [snoopy.core :as bot]
            [snoopy.slack :as slack]))

(defonce conn (atom nil))

(defn ping-handler [_] "pong!")

(def commands (bot/commands
               (bot/make-command "!ping" ping-handler)))

(defn wrap-output-channel
  "Add to the reply map the proper Slack channel."
  [handler]
  (fn [message]
    (when-let [reply (handler message)]
      (assoc reply :output-channel-id (:input-channel-id message)))))

(def bot-handler (-> commands
                     (wrap-output-channel)))

(defn event->message
  "Maps a Slack event into a message, also enhance it with the
  channel."
  [e]
  {:body             (:text e)
   :input-channel-id (:channel e)})

(defn reply->event
  "Maps a reply to a valid Slack event."
  [r]
  {:type    :message
   :channel (:output-channel-id r)
   :text    (:body r)})

(defn build-incoming-event-channel []
  (let [ch (chan)]
    (go-loop []
      (when-let [event (<! ch)]
        (when-let [reply (bot-handler (event->message event))]
          (slack/pub-event (:outcoming-ch @conn) (reply->event reply)))
        (recur)))
    ch))

(defn start! [token]
  (let [c           (slack/start! token)
        publication (:publication c)]
    (reset! conn c)
    (slack/sub-to-event publication "message" (build-incoming-event-channel))))

(defn stop! []
  (slack/close-connection! @conn)
  (reset! conn nil))
