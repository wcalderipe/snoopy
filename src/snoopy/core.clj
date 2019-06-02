(ns snoopy.core
  (:require [clojure.string :as str])
  (:gen-class))

;; Idea
;;
;; Command:
;; They represent a single user action identified by a prefix regex
;; (i.e. ping).  The commands will be stored into an atom map where
;; the key is the lockdown prefix and the value is the handler.
;;
;; Before store the command, we should lockdown the regex to prevent
;; collisions. The lockdown mechanism will prefix and suffix the given
;; regex with `^` and `$`. For instance, the prefix regex `#"ping"` is
;; stored as `#"^ping$"`.
;;
;; Inside a chat, like Slack, commands are identified by the prefix
;; `!`. The ping example will only be triggered when a user say
;; `!ping`.
;;
;; Commands are ment to be used exclusive by Slack RTM API which means
;; we can only send strings. That said, the expected return for any
;; command should be a string that will be send to the chat
;; application. If enconding is required, it should be done in above
;; layers.
;;
;; TODO: command arguments (use argv parse as base)

(def commands (atom {}))

(defn add-cmd!
  "Store a new command str-prefix and handler."
  [str-prefix handler]
  (swap! commands conj {str-prefix handler}))

(defn cmd-lookup [str-cmd]
  ;; TODO: Strict lookup. Given the command foo:
  ;;       - `!foo` is a valid string
  ;;       - `!foo bar` is invalid
  (let [str-prefix (str/replace str-cmd #"^!" "")]
    (get @commands str-prefix)))

(defn str-cmd?
  "Return true if a string starts with a bang, which indicates it's a
  command instruction."
  [s]
  ;; TODO: We need to optmize the regex since it will be applied on
  ;;       all Slack incoming messages (suggestion: use char at 0)
  (boolean (re-matches #"^!.*" s)))

(defrecord ParsedCommand [raw-str handler])

(defn cmd-parse
  "Parse a command string into a ParsedCommand, otherwise return nil."
  [s]
  (let [cmd-handler (cmd-lookup s)]
    (when (and (str-cmd? s) cmd-handler)
      (->ParsedCommand s cmd-handler))))

(defn cmd-resolve
  "Evaluate the result of a ParsedCommand."
  [^ParsedCommand parsed-cmd]
  ((:handler parsed-cmd)))

(defn ns-sym->classpath [ns-sym]
  (-> ns-sym
      name
      (.replace "snoopy." "")
      (.replace "." "/")))

(defn cmd-classpaths
  "Return all commands classpaths in resources."
  []
  (->> (all-ns)
       (map ns-name)
       (filter #(str/starts-with? % "snoopy.command"))
       (map ns-sym->classpath)))

(defn cmd-load-all!
  "Dynamically load all commands inside the directory `command/` in
  order to invoke the fn cmd-add! which is placed individually in the
  bottom of each command namespace."
  []
  (doseq [cmd-cp (cmd-classpaths)]
    (println "Loading" cmd-cp)
    (load cmd-cp)))
