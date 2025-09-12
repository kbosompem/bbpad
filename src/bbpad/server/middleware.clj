(ns bbpad.server.middleware
  "Custom middleware for BBPad - Babashka compatible"
  (:require [clojure.data.json :as json]
            [clojure.string :as str]))

(defn wrap-json-response
  "Middleware to encode response body as JSON - Babashka compatible"
  [handler]
  (fn [request]
    (let [response (handler request)]
      (if (and (map? response) 
               (not (string? (:body response)))
               (not (nil? (:body response))))
        (-> response
            (assoc :body (json/write-str (:body response)))
            (assoc-in [:headers "Content-Type"] "application/json"))
        response))))

(defn wrap-json-body
  "Middleware to parse JSON request body - Babashka compatible"
  [handler {:keys [keywords?] :or {keywords? false}}]
  (fn [request]
    (if (and (some-> (:content-type request) (str/includes? "application/json"))
             (:body request))
      (try
        (let [body-str (slurp (:body request))
              parsed-body (json/read-str body-str keywords?)]
          (handler (assoc request :body parsed-body)))
        (catch Exception e
          {:status 400
           :body {:error "Invalid JSON in request body"
                  :message (.getMessage e)}}))
      (handler request))))

(defn wrap-cors
  "Simple CORS middleware for development - Babashka compatible"
  [handler]
  (fn [request]
    (let [response (handler request)]
      (if (map? response)
        (-> response
            (assoc-in [:headers "Access-Control-Allow-Origin"] "*")
            (assoc-in [:headers "Access-Control-Allow-Methods"] "GET, POST, PUT, DELETE, OPTIONS")
            (assoc-in [:headers "Access-Control-Allow-Headers"] "Content-Type, Authorization"))
        response))))

(defn wrap-exception-handling
  "Middleware to handle exceptions gracefully"
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (println (str "Request error: " (.getMessage e)))
        {:status 500
         :body {:error "Internal server error"
                :message (.getMessage e)}}))))