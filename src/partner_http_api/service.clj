(ns partner-http-api.service
  (:require [io.pedestal.http :as bootstrap]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [io.pedestal.interceptor :refer [interceptor]]
            [ring.util.response :as ring-resp]
            [turbovote.resource-config :refer [config]]
            [pedestal-toolbox.cors :as cors]
            [pedestal-toolbox.params :refer :all]
            [pedestal-toolbox.content-negotiation :refer :all]
            [clojure.core.async :refer [go alt! timeout]]
            [bifrost.core :as bifrost]
            [bifrost.interceptors :as bifrost.i]
            [partner-http-api.channels :as channels]))

(def ping
  (interceptor
   {:enter
    (fn [ctx]
      (assoc ctx :response (ring-resp/response "OK")))}))

(defroutes routes
  [[["/"
     ^:interceptors [(body-params)
                     (negotiate-response-content-type ["application/edn"
                                                       "application/transit+json"
                                                       "application/transit+msgpack"
                                                       "application/json"
                                                       "text/plain"])]
     ["/sites"
      {:get [:partner-site-list
             (bifrost/interceptor channels/partner-site-list)]}
      ^:interceptors [(bifrost.i/update-in-response [:body :partner-sites]
                                                    [:body] identity)]]
     ["/ping" {:get [:ping ping]}]]]])

(defn service []
  {::env :prod
   ::bootstrap/router :linear-search
   ::bootstrap/routes routes
   ::bootstrap/resource-path "/public"
   ::bootstrap/allowed-origins (cors/domain-matcher-fn
                                (map re-pattern
                                     (config [:server :allowed-origins])))
   ::bootstrap/host (config [:server :hostname])
   ::bootstrap/type :immutant
   ::bootstrap/port (config [:server :port])})
