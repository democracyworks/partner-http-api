(ns partner-http-api.channels
  (:require [clojure.core.async :as async]))

(defonce partner-site-list (async/chan 100))

(defn close-all! []
  (doseq [c [partner-site-list]]
    (async/close! c)))
