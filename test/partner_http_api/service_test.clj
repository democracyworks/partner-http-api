(ns partner-http-api.service-test
  (:require [clj-http.client :as http]
            [clojure.core.async :as async :refer [<! >!]]
            [clojure.edn :as edn]
            [clojure.test :refer [deftest testing is use-fixtures]]
            [io.pedestal.http :as pedestal.http]
            [partner-http-api.channels :as channels]
            [partner-http-api.server :as server]))

(def test-server-port 56081)

(def root-url (str "http://localhost:" test-server-port))

(defn- test-url
  "Given `path`, returns the full testing URL."
  [path]
  (str root-url path))

(defn wrap-test-server
  "Fixture to start a test HTTP server before tests `t`, and stop it afterward."
  [t]
  (let [service-map
        (server/start-http-server {::pedestal.http/port test-server-port})]
    (t)
    (pedestal.http/stop service-map)))

(use-fixtures :once wrap-test-server)

(deftest ping-test
  (testing "ping responds with 'OK'"
    (let [response (http/get (test-url "/ping")
                             {:headers {:accept "text/plain"}})]
      (is (= 200 (:status response)))
      (is (= "OK" (:body response))))))

(deftest sites-test
  (testing "/sites happy path"
    (let [payload ["https://www.whitehouse.gov"
                   "https://www.democrats.org"
                   "https://www.gop.com"]]
      (async/go
        (when-let [[ch req] (<! channels/partner-site-list)]
          (is (= {} req))
          (>! ch {:status :ok
                  :partner-sites payload})))
      (let [response (http/get (test-url "/sites"))]
        (is (= 200 (:status response)))
        (is (= payload
               (edn/read-string (:body response))))))))

(deftest sites-domain-test
  (testing "/sites/:domain happy path"
    (let [payload {:site-info "Breaking News, Latest News, and Videos"}]
      (async/go
        (when-let [[ch req] (<! channels/partner-site-read)]
          (is (= {:domain "www.cnn.com"} req))
          (>! ch {:status :ok
                  :partner-site payload})))
      (let [response (http/get (test-url "/sites/www.cnn.com"))]
        (is (= 200 (:status response)))
        (is (= payload
               (edn/read-string (:body response))))))))

(deftest campus-addresses-domain-test
  (testing "/campus-addresses/:domain happy path"
    (let [payload {:addresses [{:address "123 Front St"
                                :city "Test Town"
                                :zip "90210"}]}]
      (async/go
        (when-let [[ch req] (<! channels/campus-addresses-chan)]
          (is (= {:domain "mit.edu"} req))
          (>! ch {:status :ok
                  :campus-addresses payload})))
      (let [response (http/get (test-url "/campus-addresses/mit.edu"))]
        (is (= 200 (:status response)))
        (is (= {:campus-addresses payload}
               (edn/read-string (:body response))))))))
