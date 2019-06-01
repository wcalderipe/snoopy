(ns snoopy.core
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

(defn lockdown-str-prefix
  "Lockdown a str-prefix envolving it with `^` and `$` to generate a
  strict regex to prevent collisions."
  [str-prefix]
  (let [re-str (str str-prefix)]
    (str "^" re-str "$")))

(defn add-cmd!
  "Store a new command str-prefix and handler."
  [str-prefix handler]
  (swap! commands conj {(lockdown-str-prefix str-prefix) handler}))
