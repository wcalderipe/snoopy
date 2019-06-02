(ns snoopy.command.ping
  (:require [snoopy.core :refer [add-cmd!]]))

(defn ping-cmd []
  "...pong :table_tennis_paddle_and_ball:")

(add-cmd! "ping" ping-cmd)
