(ns partner-http-api.queue
  (:require [clojure.tools.logging :as log]
            [langohr.core :as rmq]
            [kehaar.core :as k]
            [kehaar.wire-up :as wire-up]
            [kehaar.rabbitmq]
            [partner-http-api.channels :as channels]
            [partner-http-api.handlers :as handlers]
            [turbovote.resource-config :refer [config]]))

(defn initialize []
  (let [max-retries 5
        rabbit-config (config [:rabbitmq :connection])
        connection (kehaar.rabbitmq/connect-with-retries rabbit-config max-retries)]
    (let [incoming-events []
          incoming-services [(wire-up/incoming-service
                              connection
                              "partner-http-api.ok"
                              (config [:rabbitmq :queues "partner-http-api.ok"])
                              channels/ok-requests
                              channels/ok-responses)]
          external-services [(wire-up/external-service
                              connection
                              ""
                              "partner-works.partner-site.list"
                              (config [:rabbitmq :queues "partner-works.partner-site.list"])
                              5000
                              channels/partner-site-list)]
          outgoing-events []]

      (wire-up/start-responder! channels/ok-requests
                                channels/ok-responses
                                handlers/ok)
      
      {:connections [connection]
       :channels (vec (concat
                       incoming-events
                       incoming-services
                       external-services
                       outgoing-events))})))

(defn close-resources! [resources]
  (doseq [resource resources]
    (when-not (rmq/closed? resource) (rmq/close resource))))

(defn close-all! [{:keys [connections channels]}]
  (close-resources! channels)
  (close-resources! connections))
