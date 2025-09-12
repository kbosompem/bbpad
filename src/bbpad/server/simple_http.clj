(ns bbpad.server.simple-http
  "Simple HTTP server implementation for Babashka - no external dependencies"
  (:require [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.java.io :as io])
  (:import [java.net ServerSocket Socket InetSocketAddress]
           [java.io BufferedReader InputStreamReader PrintWriter]
           [java.util.concurrent Executors ThreadPoolExecutor]))

(def ^:dynamic *server-socket* (atom nil))
(def ^:dynamic *thread-pool* (atom nil))
(def ^:dynamic *running* (atom false))

(defn parse-http-request
  "Parse raw HTTP request into a map"
  [reader]
  (try
    (when-let [request-line (.readLine reader)]
      (let [[method path protocol] (str/split request-line #" ")
            headers (loop [headers {}
                          line (.readLine reader)]
                     (if (or (nil? line) (str/blank? line))
                       headers
                       (let [[key value] (str/split line #": " 2)]
                         (recur (assoc headers (str/lower-case key) value)
                                (.readLine reader)))))
            
            ;; Parse query parameters
            [path-part query-string] (str/split path #"\?" 2)
            query-params (when query-string
                          (->> (str/split query-string #"&")
                               (map #(str/split % #"=" 2))
                               (into {})))
            
            ;; Read body if present
            content-length (when-let [cl (get headers "content-length")]
                            (try (Integer/parseInt cl) (catch Exception _ nil)))
            body (when (and content-length (pos? content-length))
                   (let [chars (char-array content-length)]
                     (.read reader chars)
                     (String. chars)))]
        
        {:method (str/upper-case method)
         :path path-part
         :query-params query-params
         :headers headers
         :body body}))
    (catch Exception e
      (println (str "Error parsing request: " (.getMessage e)))
      nil)))

(defn build-http-response
  "Build HTTP response string from response map"
  [{:keys [status headers body] :or {status 200 headers {} body ""}}]
  (let [status-text (case status
                      200 "OK"
                      404 "Not Found"
                      500 "Internal Server Error"
                      "Unknown")
        
        default-headers {"Content-Type" "text/html"
                        "Connection" "close"
                        "Server" "BBPad/1.0"}
        
        all-headers (merge default-headers headers)
        
        body-str (if (string? body) body (json/write-str body))
        content-length (count (.getBytes body-str "UTF-8"))
        
        response-headers (assoc all-headers "Content-Length" (str content-length))
        
        header-lines (map (fn [[k v]] (str k ": " v)) response-headers)]
    
    (str "HTTP/1.1 " status " " status-text "\r\n"
         (str/join "\r\n" header-lines) "\r\n"
         "\r\n"
         body-str)))

(defn handle-client
  "Handle a single client connection"
  [socket handler-fn]
  (try
    (with-open [reader (BufferedReader. (InputStreamReader. (.getInputStream socket)))
                writer (PrintWriter. (.getOutputStream socket) true)]
      
      (when-let [request (parse-http-request reader)]
        (let [response (try
                        (handler-fn request)
                        (catch Exception e
                          (println (str "Handler error: " (.getMessage e)))
                          {:status 500
                           :headers {"Content-Type" "application/json"}
                           :body {:error "Internal server error"}}))
              
              response-str (build-http-response response)]
          
          (.print writer response-str)
          (.flush writer))))
    
    (catch Exception e
      (println (str "Client handling error: " (.getMessage e))))
    
    (finally
      (try
        (.close socket)
        (catch Exception _)))))

(defn start-server!
  "Start the simple HTTP server"
  [handler-fn {:keys [port host] :or {port 8080 host "localhost"}}]
  (when @*running*
    (throw (ex-info "Server already running" {})))
  
  (let [server-socket (ServerSocket.)
        thread-pool (Executors/newFixedThreadPool 10)]
    
    ;; Bind to address
    (.bind server-socket (InetSocketAddress. host port))
    
    (reset! *server-socket* server-socket)
    (reset! *thread-pool* thread-pool)
    (reset! *running* true)
    
    ;; Start accepting connections in background
    (.submit thread-pool
      (fn []
        (try
          (println (str "🌐 Simple HTTP server started on " host ":" (.getLocalPort server-socket)))
          
          (while @*running*
            (try
              (let [client-socket (.accept server-socket)]
                (.submit thread-pool
                  (fn []
                    (handle-client client-socket handler-fn))))
              (catch Exception e
                (when @*running*
                  (println (str "Accept error: " (.getMessage e)))))))
          
          (catch Exception e
            (println (str "Server error: " (.getMessage e))))
          
          (finally
            (println "🛑 Server thread ended"))))
    
    (.getLocalPort server-socket)))

(defn stop-server!
  "Stop the simple HTTP server"
  []
  (when @*running*
    (reset! *running* false)
    
    (when @*server-socket*
      (try
        (.close @*server-socket*)
        (catch Exception _))
      (reset! *server-socket* nil))
    
    (when @*thread-pool*
      (.shutdown @*thread-pool*)
      (reset! *thread-pool* nil))
    
    (println "🛑 Simple HTTP server stopped")))

(defn create-router
  "Create a simple router function"
  [routes]
  (fn [request]
    (let [method (:method request)
          path (:path request)]
      
      (if-let [handler (some (fn [[route-method route-path handler-fn]]
                              (when (and (= method route-method)
                                        (= path route-path))
                                handler-fn))
                            routes)]
        (handler request)
        
        ;; 404 response
        {:status 404
         :headers {"Content-Type" "application/json"}
         :body {:error "Not found" :path path :method method}}))))

(defn serve-static
  "Serve static files from resources"
  [request resource-path]
  (let [path (:path request)
        resource-file (str resource-path path)]
    
    (if-let [resource (io/resource resource-file)]
      (let [content (slurp resource)
            content-type (cond
                          (str/ends-with? path ".html") "text/html"
                          (str/ends-with? path ".css") "text/css"
                          (str/ends-with? path ".js") "application/javascript"
                          (str/ends-with? path ".json") "application/json"
                          :else "text/plain")]
        {:status 200
         :headers {"Content-Type" content-type}
         :body content})
      
      {:status 404
       :headers {"Content-Type" "text/plain"}
       :body "File not found"}))))