(ns snoopy.core
  (:require [clojure.string :as str])
  (:gen-class))

;; The bot consists of four components:
;; - Handler
;; - Message
;; - Reply
;; - Middleware
;;
;;
;; Handlers are functions that define your bot behavior in a
;; imperative way.
;;
;; (defn sync-handler [message]
;;   {:status :ok
;;    :body "Send me!"})
;;
;;
;; Messages are Clojure maps with standard keys.
;;
;; - :body A string representing the unmodified user text
;;
;;
;; Reply are also Clojure maps with standard keys which can be
;; translated to a valid text message downstream.
;;
;; - :status A keyword with two possible values :ok and :error
;; - :body   A representation of the message text. The body can be one
;;           of following types:
;;           - String the body is sent directly to the client
;;           - InputStream the content is sent to the client. When
;;             the stream is exhausted, it will be closed.
;;
;;
;; Middlewares are higher-level functions that add additional
;; functionality to handlers. The first argument of a middleware
;; should be a handler, and its return value should be a new handler
;; function that will call the original handler.
;;
;; (defn wrap-format-body-as-code [handler]
;;   (fn [message]
;;     (let [reply (handler message)
;;           body  (:body reply)]
;;       (assoc reply :body (str "```" body "```")))))
;;
;; The middleware wrap-format-body-as-code is an example on how to
;; change the reply map adding extra functionality into it.
;;
;; It's also possible to add new functionality to the message itself
;; before it lands the application handlers. The middleware
;; wrap-message-body-size adds a new information to the initial
;; message.
;;
;; (defn wrap-message-body-size [handler]
;;   (fn [message]
;;     (let [body        (:body message)
;;           new-message (assoc message :body-size (count body))]
;;       (handle new-message))))
;;
;; We may want have conditional executing handlers.
;;
;; (defn wrap-auth [handler]
;;   (fn [message]
;;     (if (authorized? message)
;;       (handler message)
;;       (handler {:status :error
;;                 :body "You don't have permissions for this command!"}))))

(defprotocol Reply
  (render [x]
    "Render `x` into a suitable reply map from the given type."))

(extend-protocol Reply
  nil
  (render [_] nil)

  String
  (render [body]
    {:body body})

  clojure.lang.IPersistentMap
  (render [reply]
    (merge {:body nil} reply)))

(defn wrap-reply [handler]
  (fn [message]
    (render (handler message))))

(defn wrap-command-matches [handler identifier]
  (fn [message]
    (when (= (:body message) identifier)
      (handler message))))

(defn make-command
  "Create a function that will only call the handler if it match the
  identifier. This adds the following middleware to your handler:

  - wrap-command-matches
  - wrap-reply"
  [identifier handler]
  (-> handler
      (wrap-reply)
      (wrap-command-matches identifier)))

(defn apply-handlers
  "Apply a list of handlers to a message map."
  [message & handlers]
  (some #(% message) handlers))

(defn commands
  "Create a handler by combining several handlers into one."
  [& handlers]
  (fn [message]
    (apply apply-handlers message handlers)))
