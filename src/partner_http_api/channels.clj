(ns partner-http-api.channels
  (:require [clojure.core.async :as async]))


(defonce ok-requests (async/chan))
(defonce ok-responses (async/chan))

(defonce partner-site-list (async/chan 100))

(defn close-all! []
  (doseq [c [ok-requests ok-responses]]
    (async/close! c)))
