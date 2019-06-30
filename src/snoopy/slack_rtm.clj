(ns snoopy.slack-rtm
  (:require [clojure.core.async :refer [chan close! go go-loop pub sub unsub-all >! <!]]
            [clj-http.client :as client]
            [gniazdo.core :as ws]
            [cheshire.core :as json]))

(defn auth!
  "Authenticate the given token against the Slack RTM API, returning
  the full qualified WebSocket URL otherwise nil."
  ([token]
   (auth! token "https://slack.com/api/rtm.connect"))
  ([token url]
   (let [body (:body (client/get url {:query-params {:token token}
                                      :as :json}))]
     (when (:ok body)
       (:url body)))))

(defn- msg->event
  "Parse a socket message to a Slack event. If the JSON parse fail,
  create a new custom exception event with the same schema of Slack
  event."
  [m]
  (try
    (json/parse-string m true)
    (catch Throwable e
      {:type :exception :e e})))

(def ^:const excluded-event-types #{:desktop_notification :user_typing})

(defn on-receive [incoming-ch]
  (fn [msg]
    (let [event (update (msg->event msg) :type keyword)]
      (when-not (contains? excluded-event-types (:type event))
        #_(println "[INFO] Incoming event:" event)
        (go (>! incoming-ch event))))))

(defn ws-connect
  "Establish a socket connection with the given URL and publish all
  incoming events into a channel."
  [ws-url incoming-ch]
  (ws/connect ws-url
    ;; TODO: Investigate if :on-connect and :on-binary callbacks have
    ;;       some use case for us.
    :on-receive (on-receive incoming-ch)

    ;; TODO: Add optional callbacks for :on-close and :on-error
    ;;       :on-close (fn [^Integer status ^String reason] ...)
    ;;       :on-error (fn [^Throwable throwable] ...)
    ))

(defn dispatch-outgoing-events [socket ch]
  (go-loop []
    (when-let [event (<! ch)]
      #_(println "[INFO] Outgoing event:" event)
      (ws/send-msg socket (json/generate-string event))
      (recur))))

(defrecord Connection [socket incoming-ch outgoing-ch publication])

;; TODO: Add test case
(defn make-connection [ws-url]
  (let [incoming-ch (chan)
        outgoing-ch (chan)
        socket      (ws-connect ws-url incoming-ch)
        publication (pub incoming-ch #(:type %))]
    (dispatch-outgoing-events socket outgoing-ch)
    (->Connection socket incoming-ch outgoing-ch publication)))

(defn connect! [token]
  (when-let [ws-url (auth! token)]
    (make-connection ws-url)))

(defn disconnect! [conn]
  (ws/close  (:socket conn))
  (unsub-all (:publication conn))
  (close!    (:incoming-ch conn))
  (close!    (:outgoing-ch conn)))

(defn subscribe [publication event-type ch]
  (sub publication event-type ch))

(defn publish [outgoing-ch event]
  (go (>! outgoing-ch event)))

(comment
  (def token "xoxb-foo-bar-baz")

  (defn build-print-channel [prefix]
    (let [ch (chan)]
      (go-loop []
        (when-let [message (<! ch)]
          (println (str prefix ":") message)
          (recur)))
      ch))

  (def conn (connect! token))
  (disconnect! conn)

  (subscribe (:publication conn) :message (build-print-channel "message"))
  (subscribe (:publication conn) :pong (build-print-channel "pong"))

  (publish (:outgoing-ch conn) {:type :ping})
  (publish (:outgoing-ch conn) {:type :message
                                :channel "CHANNEL_ID"
                                :text "lorem ipsum"}))
