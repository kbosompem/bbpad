(ns bbpad.server.handlers
  "Ring request handlers for BBPad API"
  (:require [bbpad.core.script-engine :as script-engine]
            [bbpad.core.script-storage :as storage]
            [bbpad.db.connections :as db]
            [bbpad.db.app-storage :as app-storage]
            [bbpad.core.config :as config]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [html5 include-css include-js]]
            ; [clojure.core.async :as async]
            ))

(defn serve-index
  "Serve the main HTML page with embedded ClojureScript app"
  [request]
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
                  <p>Babashka-powered LINQPad alternative</p>
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
           "]])})

(defn execute-script
  "Execute a Babashka script"
  [{:keys [body] :as request}]
  (try
    (let [{:keys [code parameters context]} (:body request)
          result (script-engine/execute-script code {:parameters parameters
                                                      :context context})]
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
    (let [connections (db/list-connections)]
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
    (let [{:keys [id config]} (:body request)
          conn-id (or id (:id config) (str "conn-" (System/currentTimeMillis)))
          result (db/add-connection! conn-id config)]
      (if (:success result)
        {:status 200
         :headers {"Content-Type" "application/json"}
         :body {:success true :id (or (:id result) conn-id)}}
        {:status 400
         :headers {"Content-Type" "application/json"}
         :body {:success false :error (:error result)}}))
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
          result (db/remove-connection! id)]
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
          result (db/update-connection! id config)]
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
          result (db/get-schema connection-id)]
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body result})
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
    (let [{:keys [id name content language tags]} (:body request)
          result (app-storage/save-script! 
                  {:id id
                   :name name
                   :content content
                   :language (or language "clojure")
                   :tags tags})]
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
  [{:keys [params] :as request}]
  (try
    (let [id (:id params)
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
          :execution-time execution-time}))
      
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