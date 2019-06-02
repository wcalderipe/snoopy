(ns snoopy.slack-processor
  (:require [clojure.core.async :refer [>! <! chan go go-loop]]
            [snoopy.core :as bot]
            [snoopy.slack :as slack]))

;; Slack Processor Component
;;
;; This component purpose is establish a connection with Slack RTM API
;; and process all incoming messages. It will be guided by the
;; following rules:
;;
;; - If the incoming message is a command (starts with a bang and is
;;   valid command)
;; - Parse the message
;; - Evaluate and get the result of the command
;; - Send the result to the same channel

(def conn (atom nil))

(defn build-incoming-message-channel []
  (let [ch (chan)]
    (go-loop []
      (when-let [message (<! ch)]
        (when-let [parsed-cmd (bot/cmd-parse (:text message))]
          (println "***" (bot/cmd-resolve parsed-cmd))
          (slack/pub-event (:outcoming-ch @conn) {:type :message
                                                  :channel (:channel message)
                                                  :text (bot/cmd-resolve parsed-cmd)}))
        (recur)))
    ch))

(defn start [token]
  (bot/cmd-load-all!)
  (let [c           (slack/start! token)
        publication (:publication c)]
    (reset! conn c)
    (slack/sub-to-event publication "message" (build-incoming-message-channel))))

(defn stop []
  (slack/close-connection @conn)
  (reset! conn nil))
