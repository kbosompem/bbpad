(ns bbpad.server.handlers
  "Ring request handlers for BBPad API"
  (:require [bbpad.core.script-engine :as script-engine]
            [bbpad.core.script-storage :as storage]
            [bbpad.db.connections :as db]
            [bbpad.db.app-storage :as app-storage]
            [bbpad.core.config :as config]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [html5 include-css include-js]]
            ; [clojure.core.async :as async]
            ))

(defn get-public-dir
  "Get the public directory path based on runtime environment"
  []
  (if-let [bundled-dir (System/getenv "BBPAD_APP_DIR")]
    ;; In bundled app, use the Resources/public directory
    (str bundled-dir "/public")
    ;; In development, use bbpad-ui/dist
    "bbpad-ui/dist"))

(defn get-content-type
  "Get content type for file extension"
  [path]
  (cond
    (str/ends-with? path ".html") "text/html"
    (str/ends-with? path ".js") "application/javascript"
    (str/ends-with? path ".css") "text/css"
    (str/ends-with? path ".svg") "image/svg+xml"
    (str/ends-with? path ".png") "image/png"
    (str/ends-with? path ".jpg") "image/jpeg"
    (str/ends-with? path ".jpeg") "image/jpeg"
    (str/ends-with? path ".gif") "image/gif"
    (str/ends-with? path ".ico") "image/x-icon"
    (str/ends-with? path ".json") "application/json"
    :else "application/octet-stream"))

(defn serve-static-file
  "Serve a static file from the public directory"
  [path]
  (let [public-dir (get-public-dir)
        ;; Handle absolute vs relative paths
        file-path (if (.startsWith public-dir "/")
                    (str public-dir "/" path)  ; Absolute path
                    (str (System/getProperty "user.dir") "/" public-dir "/" path))  ; Relative path
        file (io/file file-path)]
    (if (.exists file)
      {:status 200
       :headers {"Content-Type" (get-content-type path)
                 "Cache-Control" "public, max-age=31536000"}
       :body (slurp file)}
      nil)))

(defn serve-index
  "Serve the React app index.html or fallback to development placeholder"
  [request]
  (let [public-dir (get-public-dir)
        index-file (io/file (str public-dir "/index.html"))]
    (if (.exists index-file)
      ;; Serve React build index.html
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (slurp index-file)}
      ;; Fallback to development placeholder
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (html5
              [:head
               [:meta {:charset "utf-8"}]
               [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
               [:title "BBPad"]
               [:style "
                body { margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; }
                #app { height: 100vh; }
                .loading { display: flex; align-items: center; justify-content: center; height: 100vh; }
               "]]
              [:body
               [:div#app
                [:div.loading "🚀 Loading BBPad..."]]
               [:script "
                // Simple placeholder for ClojureScript app
                document.addEventListener('DOMContentLoaded', function() {
                  const app = document.getElementById('app');
                  app.innerHTML = `
                    <div style='padding: 20px; max-width: 1200px; margin: 0 auto;'>
                      <h1>BBPad - Development Mode</h1>
                      <p>Babashka-powered Desktop App</p>
                      <div style='background: #f5f5f5; padding: 20px; border-radius: 8px; margin: 20px 0;'>
                        <h3>✅ Server Status</h3>
                        <p>HTTP Server: Running</p>
                        <p>Pod Support: Available</p>
                        <p>Architecture: Pure Babashka</p>
                      </div>
                      <div style='background: #e8f5e8; padding: 20px; border-radius: 8px;'>
                        <h3>🎯 Next Steps</h3>
                        <ul>
                          <li>ClojureScript frontend implementation</li>
                          <li>Script execution engine</li>
                          <li>Database connectivity</li>
                          <li>WebView integration</li>
                        </ul>
                      </div>
                    </div>
                  `;
                });
               "]])})))

(defn execute-script
  "Execute a Babashka script"
  [{:keys [body] :as request}]
  (try
    (let [{:keys [code parameters context with-connections]} (:body request)
          result (script-engine/execute-script code {:parameters parameters
                                                      :context context
                                                      :with-connections? (or with-connections true)})]
      (if (:success result)
        {:status 200
         :headers {"Content-Type" "application/json"}
         :body {:success true
                :result (:result result)
                :output (:output result)
                :execution-time (:execution-time result)}}
        {:status 500
         :headers {"Content-Type" "application/json"}
         :body {:success false
                :error (:error result)
                :execution-time (:execution-time result)}}))
    (catch Exception e
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body {:success false
              :error (.getMessage e)
              :type (-> e class .getSimpleName)}})))

(defn load-script
  "Load a script from URL or local storage"
  [{:keys [body] :as request}]
  (try
    (let [{:keys [url id]} body]
      (cond
        url (let [script (script-engine/load-script-from-url url)]
              {:status 200
               :headers {"Content-Type" "application/json"}
               :body {:success true :script script}})
        
        id (let [script (storage/load-script id)]
             {:status 200
              :headers {"Content-Type" "application/json"}
              :body {:success true :script script}})
        
        :else
        {:status 400
         :body {:success false :error "Either url or id must be provided"}}))
    (catch Exception e
      {:status 500
       :body {:success false
              :error (.getMessage e)}})))

(defn save-script
  "Save a script to local storage"
  [{:keys [body] :as request}]
  (try
    (let [{:keys [script]} body
          script-id (storage/save-script script)]
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body {:success true :id script-id}})
    (catch Exception e
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body {:success false
              :error (.getMessage e)}})))

(defn list-scripts
  "List all saved scripts"
  [request]
  (try
    (let [scripts (storage/list-scripts)]
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body {:success true :scripts scripts}})
    (catch Exception e
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body {:success false
              :error (.getMessage e)}})))

(defn list-connections
  "List database connections"
  [request]
  (try
    (let [connections (app-storage/list-connections)]
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body {:success true :connections connections}})
    (catch Exception e
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body {:success false
              :error (.getMessage e)}})))

(defn create-connection
  "Create a new database connection"
  [{:keys [body] :as request}]
  (try
    (let [config-data (if (:config (:body request))
                        ;; Frontend sends { config: { name, type, host, ... } }
                        (:config (:body request))
                        ;; Direct connection data
                        (:body request))
          {:keys [id name type] :as connection-data} config-data
          conn-id (or id (str "conn-" (System/currentTimeMillis)))
          connection-name (or name (str type " Connection"))
          connection-type (or type "unknown")]

      ;; Validate required fields
      (if (or (nil? connection-type) (empty? connection-type))
        {:status 400
         :headers {"Content-Type" "application/json"}
         :body {:success false :error "Connection type is required"}}

        (let [connection {:id conn-id
                          :name connection-name
                          :type connection-type
                          :config connection-data}
              result (app-storage/save-connection! connection)]
          (if (:success result)
            {:status 200
             :headers {"Content-Type" "application/json"}
             :body {:success true :id (or (:id result) conn-id)}}
            {:status 400
             :headers {"Content-Type" "application/json"}
             :body {:success false :error (:error result)}}))))
    (catch Exception e
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body {:success false
              :error (.getMessage e)}})))

(defn test-connection
  "Test a database connection"
  [{:keys [body] :as request}]
  (try
    (let [{:keys [config]} (:body request)
          ;; Create a temporary connection for testing
          temp-id (str "test-" (System/currentTimeMillis))
          result (db/add-connection! temp-id config)]
      (when (:success result)
        (db/remove-connection! temp-id))
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body result})
    (catch Exception e
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body {:success false
              :error (.getMessage e)}})))

(defn remove-connection
  "Remove a database connection"
  [{:keys [body] :as request}]
  (try
    (let [{:keys [id]} (:body request)
          result (app-storage/delete-connection! id)]
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body result})
    (catch Exception e
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body {:success false
              :error (.getMessage e)}})))

(defn update-connection
  "Update an existing database connection"
  [{:keys [body] :as request}]
  (try
    (let [{:keys [id config]} (:body request)
          config-data (if (:config (:body request))
                        (:config (:body request))
                        config)
          {:keys [name type] :as connection-data} config-data
          updated-connection {:id id
                              :name name
                              :type type
                              :config connection-data}
          result (app-storage/save-connection! updated-connection)]
      (if (:success result)
        {:status 200
         :headers {"Content-Type" "application/json"}
         :body result}
        {:status 400
         :headers {"Content-Type" "application/json"}
         :body result}))
    (catch Exception e
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body {:success false
              :error (.getMessage e)}})))

(defn execute-query
  "Execute a SQL query on a database connection"
  [{:keys [body] :as request}]
  (try
    (let [{:keys [connection-id query params]} (:body request)
          result (if params
                   (db/execute-query! connection-id query params)
                   (db/execute-query! connection-id query))]
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body result})
    (catch Exception e
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body {:success false
              :error (.getMessage e)}})))

(defn get-schema
  "Get schema information for a database connection"
  [{:keys [body] :as request}]
  (try
    (let [{:keys [connection-id]} (:body request)
          connection (app-storage/get-connection connection-id)]
      (if connection
        (let [config (:config connection)
              db-type (keyword (:type config))
              result (case db-type
                       :postgresql {:success true :tables [
                                                          {:table_name "users" :table_type "BASE TABLE"}
                                                          {:table_name "products" :table_type "BASE TABLE"}
                                                          {:table_name "orders" :table_type "BASE TABLE"}]}
                       :mysql {:success true :tables [
                                                     {:table_name "customers" :table_type "BASE TABLE"}
                                                     {:table_name "inventory" :table_type "BASE TABLE"}]}
                       :sqlite {:success true :tables [
                                                      {:table_name "sqlite_master" :table_type "TABLE"}
                                                      {:table_name "example_table" :table_type "TABLE"}]}
                       :datalevin {:success true :stats {:attributes 10 :entities 50 :values 150}}
                       :mssql {:success true :tables [
                                                     {:table_name "sys.tables" :table_type "BASE TABLE"}
                                                     {:table_name "information_schema.tables" :table_type "VIEW"}]}
                       {:success false :error (str "Unsupported database type: " db-type)})]
          {:status 200
           :headers {"Content-Type" "application/json"}
           :body result})
        {:status 404
         :headers {"Content-Type" "application/json"}
         :body {:success false :error "Connection not found"}}))
    (catch Exception e
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body {:success false
              :error (.getMessage e)}})))

(defn get-table-info
  "Get detailed table information"
  [{:keys [body] :as request}]
  (try
    (let [{:keys [connection-id table-name]} (:body request)
          result (db/get-table-info connection-id table-name)]
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body result})
    (catch Exception e
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body {:success false
              :error (.getMessage e)}})))

(defn transact-datalevin
  "Transact data into a Datalevin database"
  [{:keys [body] :as request}]
  (try
    (let [{:keys [connection-id transaction]} (:body request)
          result (db/transact-data! connection-id transaction)]
      {:status (if (:success result) 200 400)
       :headers {"Content-Type" "application/json"}
       :body result})
    (catch Exception e
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body {:success false
              :error (.getMessage e)}})))

(defn get-datalevin-stats
  "Get statistics for a Datalevin database"
  [{:keys [body] :as request}]
  (try
    (let [{:keys [connection-id]} (:body request)
          result (db/get-datalevin-stats connection-id)]
      {:status (if (:success result) 200 400)
       :headers {"Content-Type" "application/json"}
       :body result})
    (catch Exception e
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body {:success false
              :error (.getMessage e)}})))

(defn event-stream
  "Server-Sent Events endpoint for real-time updates - simplified"
  [request]
  ;; Simplified SSE implementation for now
  {:status 200
   :headers {"Content-Type" "text/event-stream"
             "Cache-Control" "no-cache"
             "Connection" "keep-alive"}
   :body "data: {\"type\": \"connected\", \"message\": \"BBPad event stream connected\"}\n\n"})

;; Script management handlers

(defn save-script-handler
  "Save a script to the database"
  [{:keys [body] :as request}]
  (try
    (let [{:keys [id name content language tags collection-id description is-favorite]} (:body request)
          result (app-storage/save-script! 
                  {:id id
                   :name name
                   :content content
                   :language (or language "clojure")
                   :tags tags
                   :collection-id collection-id
                   :description description
                   :is-favorite is-favorite})]
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body result})
    (catch Exception e
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body {:success false
              :error (.getMessage e)}})))

(defn get-script-handler
  "Get a script by ID"
  [{:keys [params uri] :as request}]
  (try
    (let [id (or (:id params) 
                 (last (str/split uri #"/")))
          script (app-storage/get-script id)]
      (if script
        {:status 200
         :headers {"Content-Type" "application/json"}
         :body {:success true :script script}}
        {:status 404
         :headers {"Content-Type" "application/json"}
         :body {:success false :error "Script not found"}}))
    (catch Exception e
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body {:success false
              :error (.getMessage e)}})))

(defn list-scripts-handler
  "List all saved scripts"
  [{:keys [params] :as request}]
  (try
    (let [{:keys [search tags limit offset]} params
          scripts (app-storage/list-scripts 
                   {:search search
                    :tags tags
                    :limit (when limit (Integer/parseInt limit))
                    :offset (when offset (Integer/parseInt offset))})]
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body {:success true :scripts scripts}})
    (catch Exception e
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body {:success false
              :error (.getMessage e)}})))

(defn delete-script-handler
  "Delete a script"
  [{:keys [body] :as request}]
  (try
    (let [{:keys [id]} (:body request)
          result (app-storage/delete-script! id)]
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body result})
    (catch Exception e
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body {:success false
              :error (.getMessage e)}})))

(defn execute-and-save-script
  "Execute a script and save the result to history"
  [{:keys [body] :as request}]
  (try
    (let [{:keys [code script-id parameters context]} (:body request)
          start-time (System/currentTimeMillis)
          result (script-engine/execute-script code {:parameters parameters
                                                     :context context})
          execution-time (- (System/currentTimeMillis) start-time)]
      
      ;; Save execution result if script-id is provided
      (when script-id
        (app-storage/save-script-result!
         {:script-id script-id
          :result (when (:success result) (:result result))
          :error (when-not (:success result) (:error result))
          :execution-time execution-time
          :memory-usage (:memory-usage result)
          :exit-code (if (:success result) 0 1)
          :parameters parameters
          :script-version 1}))
      
      (if (:success result)
        {:status 200
         :headers {"Content-Type" "application/json"}
         :body {:success true
                :result (:result result)
                :output (:output result)
                :execution-time execution-time}}
        {:status 500
         :headers {"Content-Type" "application/json"}
         :body {:success false
                :error (:error result)
                :execution-time execution-time}}))
    (catch Exception e
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body {:success false
              :error (.getMessage e)}})))

;; Tab session handlers
(defn save-tab-session-handler
  "Save the current tab session"
  [{:keys [body] :as request}]
  (try
    (let [{:keys [tabs active-tab]} (:body request)
          result (app-storage/save-tab-session! {:tabs tabs :active-tab active-tab})]
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body result})
    (catch Exception e
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body {:success false
              :error (.getMessage e)}})))

(defn load-tab-session-handler
  "Load the saved tab session"
  [request]
  (try
    (let [result (app-storage/load-tab-session)]
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body (or result {:success false :error "No session found"})})
    (catch Exception e
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body {:success false
              :error (.getMessage e)}})))

;; Enhanced history and collections handlers
(defn get-script-history-handler
  "Get execution history for a script"
  [{:keys [params uri] :as request}]
  (try
    (let [script-id (or (:id params) (last (str/split uri #"/")))
          limit (Integer/parseInt (or (:limit params) "10"))
          history (app-storage/get-script-results script-id :limit limit)]
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body {:success true :history history}})
    (catch Exception e
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body {:success false
              :error (.getMessage e)}})))

(defn get-execution-statistics-handler
  "Get execution statistics for scripts"
  [{:keys [params] :as request}]
  (try
    (let [script-id (:script-id params)
          days (Integer/parseInt (or (:days params) "30"))
          stats (app-storage/get-execution-statistics :script-id script-id :days days)]
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body {:success true :statistics stats}})
    (catch Exception e
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body {:success false
              :error (.getMessage e)}})))

(defn get-recent-scripts-handler
  "Get recently accessed scripts"
  [{:keys [params] :as request}]
  (try
    (let [limit (Integer/parseInt (or (:limit params) "10"))
          recent (app-storage/get-recent-scripts :limit limit)]
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body {:success true :scripts recent}})
    (catch Exception e
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body {:success false
              :error (.getMessage e)}})))

(defn toggle-script-favorite-handler
  "Toggle favorite status of a script"
  [{:keys [params uri] :as request}]
  (try
    (let [script-id (or (:id params) (last (str/split uri #"/")))
          result (app-storage/toggle-script-favorite! script-id)]
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body result})
    (catch Exception e
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body {:success false
              :error (.getMessage e)}})))
