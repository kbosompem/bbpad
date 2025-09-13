(ns bbpad.server.babashka-http
  "HTTP server using http-kit with custom routing for BBPad"
  (:require [org.httpkit.server :as httpkit]
            [bbpad.server.handlers :as handlers]
            [bbpad.core.config :as config]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.java.io :as io]))

(def ^:dynamic *server* (atom nil))

(defn serve-static-file
  "Serve static files from bbpad-ui/dist"
  [path]
  (let [file-path (str "bbpad-ui/dist/" path)
        file (io/file file-path)]
    (when (.exists file)
      {:status 200
       :headers {"Content-Type" (handlers/get-content-type path)
                 "Cache-Control" "public, max-age=31536000"}
       :body (slurp file)})))

(defn route-request
  "Simple routing function that handles all request routing"
  [request]
  (let [method (:request-method request)
        uri (:uri request)]
    (cond
      ;; Exact matches first
      (and (= method :get) (= uri "/"))
      (handlers/serve-index request)

      ;; API routes
      (and (= method :post) (= uri "/api/execute"))
      (handlers/execute-script request)

      (and (= method :post) (= uri "/api/script/load"))
      (handlers/load-script request)

      (and (= method :post) (= uri "/api/script/save"))
      (handlers/save-script request)

      (and (= method :get) (= uri "/api/script/list"))
      (handlers/list-scripts request)

      ;; Script storage operations
      (and (= method :post) (= uri "/api/scripts/save"))
      (handlers/save-script-handler request)

      (and (= method :get) (str/starts-with? uri "/api/scripts/get/"))
      (let [id (subs uri (count "/api/scripts/get/"))]
        (handlers/get-script-handler (assoc request :params {:id id})))

      (and (= method :get) (= uri "/api/scripts/list"))
      (handlers/list-scripts-handler request)

      (and (= method :post) (= uri "/api/scripts/delete"))
      (handlers/delete-script-handler request)

      ;; Database operations
      (and (= method :get) (= uri "/api/connections"))
      (handlers/list-connections request)

      (and (= method :post) (= uri "/api/connections"))
      (handlers/create-connection request)

      (and (= method :post) (= uri "/api/connections/test"))
      (handlers/test-connection request)

      (and (= method :post) (= uri "/api/connections/remove"))
      (handlers/remove-connection request)

      (and (= method :post) (= uri "/api/connections/update"))
      (handlers/update-connection request)

      (and (= method :post) (= uri "/api/query"))
      (handlers/execute-query request)

      (and (= method :post) (= uri "/api/schema"))
      (handlers/get-schema request)

      (and (= method :post) (= uri "/api/table-info"))
      (handlers/get-table-info request)

      ;; Datalevin operations
      (and (= method :post) (= uri "/api/datalevin/transact"))
      (handlers/transact-datalevin request)

      (and (= method :post) (= uri "/api/datalevin/stats"))
      (handlers/get-datalevin-stats request)

      ;; Tab session management
      (and (= method :post) (= uri "/api/tabs/save"))
      (handlers/save-tab-session-handler request)

      (and (= method :get) (= uri "/api/tabs/load"))
      (handlers/load-tab-session-handler request)

      ;; Health check
      (and (= method :get) (= uri "/api/health"))
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body (json/write-str {:status "ok" :version (config/get-version)})}

      ;; Static assets - check if file exists in dist folder
      (and (= method :get) (str/starts-with? uri "/assets/"))
      (let [path (subs uri 1)]
        (or (serve-static-file path)
            {:status 404
             :headers {"Content-Type" "text/plain"}
             :body "File not found"}))

      ;; Check for any other static files (JS, CSS with hashes)
      (and (= method :get)
           (or (str/ends-with? uri ".js")
               (str/ends-with? uri ".css")
               (str/ends-with? uri ".svg")
               (str/ends-with? uri ".png")
               (str/ends-with? uri ".jpg")
               (str/ends-with? uri ".ico")))
      (let [path (subs uri 1)]
        (or (serve-static-file path)
            {:status 404
             :headers {"Content-Type" "text/plain"}
             :body "File not found"}))

      ;; Catch-all for SPA routing - serve index.html for any other GET request
      (= method :get)
      (handlers/serve-index request)

      ;; Default 404 for non-GET requests
      :else
      {:status 404
       :headers {"Content-Type" "text/plain"}
       :body "Not found"})))

(defn create-app
  "Create the main application handler"
  []
  route-request)

(defn wrap-json
  "Middleware to parse JSON request bodies and encode JSON responses"
  [handler]
  (fn [request]
    (let [request' (if (and (:body request)
                           (str/includes? (get-in request [:headers "content-type"] "") "application/json"))
                     (try
                       (let [body-str (cond
                                        (string? (:body request)) (:body request)
                                        (instance? java.io.Reader (:body request)) (slurp (:body request))
                                        (instance? java.io.InputStream (:body request)) (slurp (:body request))
                                        :else (str (:body request)))
                             parsed-body (json/read-str body-str :key-fn keyword)]
                         (assoc request :body parsed-body))
                       (catch Exception e
                         (println (str "JSON parse error: " (.getMessage e)))
                         request))
                     request)
          response (handler request')]

      ;; Ensure JSON response body is string
      (if (and (map? (:body response))
               (not (string? (:body response))))
        (assoc response :body (json/write-str (:body response)))
        response))))

(defn wrap-cors
  "Add CORS headers for development"
  [handler]
  (fn [request]
    (if (and (config/dev-mode?) (= :options (:request-method request)))
      ;; Handle preflight OPTIONS requests
      {:status 200
       :headers {"Access-Control-Allow-Origin" "*"
                 "Access-Control-Allow-Methods" "GET, POST, PUT, DELETE, OPTIONS"
                 "Access-Control-Allow-Headers" "Content-Type, Authorization"
                 "Access-Control-Max-Age" "86400"}
       :body ""}
      ;; Normal request processing
      (let [response (handler request)]
        (if (config/dev-mode?)
          (-> response
              (assoc-in [:headers "Access-Control-Allow-Origin"] "*")
              (assoc-in [:headers "Access-Control-Allow-Methods"] "GET, POST, PUT, DELETE, OPTIONS")
              (assoc-in [:headers "Access-Control-Allow-Headers"] "Content-Type, Authorization"))
          response)))))

(defn find-free-port
  "Find a free port starting from the given port"
  [start-port]
  (loop [port start-port]
    (let [available? (try
                       (let [socket (java.net.ServerSocket. port)]
                         (.close socket)
                         true)
                       (catch java.net.BindException _
                         false))]
      (if available?
        port
        (recur (inc port))))))

(defn start-server!
  "Start the HTTP-Kit server"
  [{:keys [port dev-mode] :as options}]
  (when @*server*
    (println "⚠️  Server already running, stopping...")
    (@*server*))

  (let [actual-port (if (zero? port) (find-free-port 8080) port)
        app (create-app)
        handler (-> app
                    wrap-json
                    wrap-cors)]

    (reset! *server* (httpkit/run-server handler {:port actual-port}))

    (when dev-mode
      (println (str "🔧 HTTP-Kit server running at http://localhost:" actual-port)))

    actual-port))

(defn stop-server!
  "Stop the HTTP-Kit server"
  []
  (when @*server*
    (println "🛑 Stopping HTTP-Kit server...")
    (@*server*)
    (reset! *server* nil)))

(defn restart-server! [options]
  "Restart the server with new options"
  (stop-server!)
  (start-server! options))