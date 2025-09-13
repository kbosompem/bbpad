(ns bbpad.server.babashka-http
  "Babashka HTTP server with Ruuter routing for BBPad"
  (:require [org.httpkit.server :as httpkit]
            [ruuter.core :as ruuter]
            [bbpad.server.handlers :as handlers]
            [bbpad.core.config :as config]
            [clojure.string :as str]
            [clojure.data.json :as json]))

(def ^:dynamic *server* (atom nil))

;; Define routes for Ruuter
(def routes
  [;; Main UI
   {:path "/"
    :method :get
    :response handlers/serve-index}
   
   ;; API routes
   {:path "/api/execute"
    :method :post
    :response handlers/execute-script}
   
   {:path "/api/script/load"
    :method :post
    :response handlers/load-script}
   
   {:path "/api/script/save"
    :method :post
    :response handlers/save-script}
   
   {:path "/api/script/list"
    :method :get
    :response handlers/list-scripts}
   
   ;; New script storage operations
   {:path "/api/scripts/save"
    :method :post
    :response handlers/save-script-handler}
   
   {:path "/api/scripts/get/:id"
    :method :get
    :response handlers/get-script-handler}
   
   {:path "/api/scripts/list"
    :method :get
    :response handlers/list-scripts-handler}
   
   {:path "/api/scripts/delete"
    :method :post
    :response handlers/delete-script-handler}
   
   ;; Database operations
   {:path "/api/connections"
    :method :get
    :response handlers/list-connections}
   
   {:path "/api/connections"
    :method :post
    :response handlers/create-connection}
   
   {:path "/api/connections/test"
    :method :post
    :response handlers/test-connection}
   
   {:path "/api/connections/remove"
    :method :post
    :response handlers/remove-connection}
   
   {:path "/api/connections/update"
    :method :post
    :response handlers/update-connection}
   
   {:path "/api/query"
    :method :post
    :response handlers/execute-query}
   
   {:path "/api/schema"
    :method :post
    :response handlers/get-schema}
   
   {:path "/api/table-info"
    :method :post
    :response handlers/get-table-info}
   
   ;; Datalevin-specific operations
   {:path "/api/datalevin/transact"
    :method :post
    :response handlers/transact-datalevin}
   
   {:path "/api/datalevin/stats"
    :method :post
    :response handlers/get-datalevin-stats}
   
   ;; Tab session management
   {:path "/api/tabs/save"
    :method :post
    :response handlers/save-tab-session-handler}
   
   {:path "/api/tabs/load"
    :method :get
    :response handlers/load-tab-session-handler}
   
   ;; Health check
   {:path "/api/health"
    :method :get
    :response (fn [request] 
                {:status 200 
                 :headers {"Content-Type" "application/json"}
                 :body (json/write-str {:status "ok" :version (config/get-version)})})}])

(defn create-router-handler
  "Create a handler function using Ruuter"
  []
  (fn [request]
    (try
      (let [response (ruuter/route routes request)]
        (cond
          ;; Successful route match
          response response
          
          ;; SPA catchall for GET requests
          (= :get (:request-method request))
          (handlers/serve-index request)
          
          ;; 404 for everything else
          :else
          {:status 404 
           :headers {"Content-Type" "application/json"}
           :body (json/write-str {:error "Not found" 
                                  :path (:uri request) 
                                  :method (:request-method request)})}))
      
      (catch Exception e
        (println (str "Request handler error: " (.getMessage e)))
        (when (config/dev-mode?)
          (.printStackTrace e))
        {:status 500 
         :headers {"Content-Type" "application/json"}
         :body (json/write-str {:error "Internal server error"})}))))

(defn add-json-middleware
  "Add JSON parsing middleware"
  [handler]
  (fn [request]
    ;; Parse JSON body if present
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
                         (println (str "Body type: " (type (:body request))))
                         (println (str "Body content: " (:body request)))
                         request))
                     request)
          
          ;; Call handler
          response (handler request')]
      
      ;; Ensure JSON response body is string
      (if (and (map? (:body response))
               (not (string? (:body response))))
        (assoc response :body (json/write-str (:body response)))
        response))))

(defn add-cors-middleware
  "Add CORS middleware for development"
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
  "Start the HTTP-Kit server with Ruuter routing"
  [{:keys [port dev-mode] :as options}]
  (when @*server*
    (println "⚠️  Server already running, stopping...")
    (@*server*))
  
  (let [actual-port (if (zero? port) (find-free-port 8080) port)
        handler (-> (create-router-handler)
                    add-json-middleware
                    add-cors-middleware)
        
        server (httpkit/run-server handler {:port actual-port})]
    
    (reset! *server* server)
    
    (when dev-mode
      (println (str "🔧 HTTP-Kit + Ruuter server running at http://localhost:" actual-port)))
    
    actual-port))

(defn stop-server!
  "Stop the HTTP-Kit server"
  []
  (when @*server*
    (println "🛑 Stopping HTTP-Kit server...")
    (@*server*)  ; HTTP-Kit server is a function to stop
    (reset! *server* nil)))

(defn restart-server! [options]
  "Restart the server with new options"
  (stop-server!)
  (start-server! options))