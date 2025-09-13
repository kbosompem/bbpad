(ns bbpad.db.app-storage
  "SQLite database for storing BBPad scripts and connections"
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [bbpad.core.config :as config]))

(def ^:private db-spec
  {:dbtype "sqlite"
   :dbname (str (config/get-config-dir) "/bbpad.db")})

(defn- get-connection []
  (jdbc/get-datasource db-spec))

(defn init-database!
  "Initialize the BBPad database with required tables"
  []
  (let [ds (get-connection)]
    ;; Create scripts table
    (jdbc/execute! ds
      ["CREATE TABLE IF NOT EXISTS scripts (
         id TEXT PRIMARY KEY,
         name TEXT NOT NULL,
         content TEXT NOT NULL,
         language TEXT DEFAULT 'clojure',
         tags TEXT,
         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
         last_run_at TIMESTAMP,
         run_count INTEGER DEFAULT 0
       )"])
    
    ;; Create connections table (for persistence)
    (jdbc/execute! ds
      ["CREATE TABLE IF NOT EXISTS saved_connections (
         id TEXT PRIMARY KEY,
         name TEXT NOT NULL,
         type TEXT NOT NULL,
         config TEXT NOT NULL,
         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
       )"])
    
    ;; Create script_results table for storing execution history
    (jdbc/execute! ds
      ["CREATE TABLE IF NOT EXISTS script_results (
         id TEXT PRIMARY KEY,
         script_id TEXT NOT NULL,
         result TEXT,
         error TEXT,
         execution_time INTEGER,
         executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
         FOREIGN KEY (script_id) REFERENCES scripts(id)
       )"])
    
    ;; Create snippets table for code snippets
    (jdbc/execute! ds
      ["CREATE TABLE IF NOT EXISTS snippets (
         id TEXT PRIMARY KEY,
         name TEXT NOT NULL,
         description TEXT,
         content TEXT NOT NULL,
         language TEXT DEFAULT 'clojure',
         category TEXT,
         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
       )"])
    
    (println "✅ BBPad database initialized")))

;; Script operations

(defn save-script!
  "Save or update a script"
  [{:keys [id name content language tags] :as script}]
  (let [ds (get-connection)
        script-id (or id (str "script-" (System/currentTimeMillis)))
        existing (jdbc/execute-one! ds
                   ["SELECT id FROM scripts WHERE id = ?" script-id]
                   {:builder-fn rs/as-unqualified-lower-maps})]
    (if existing
      ;; Update existing script
      (do
        (jdbc/execute! ds
          ["UPDATE scripts SET 
            name = ?, content = ?, language = ?, tags = ?, 
            updated_at = CURRENT_TIMESTAMP
            WHERE id = ?"
           name content (or language "clojure") tags script-id])
        {:success true :id script-id :action "updated"})
      ;; Insert new script
      (do
        (jdbc/execute! ds
          ["INSERT INTO scripts (id, name, content, language, tags) 
            VALUES (?, ?, ?, ?, ?)"
           script-id name content (or language "clojure") tags])
        {:success true :id script-id :action "created"}))))

(defn get-script
  "Get a script by ID"
  [id]
  (let [ds (get-connection)]
    (jdbc/execute-one! ds
      ["SELECT * FROM scripts WHERE id = ?" id]
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn list-scripts
  "List all scripts with optional filtering"
  ([]
   (list-scripts {}))
  ([{:keys [search tags limit offset]}]
   (let [ds (get-connection)
         base-query "SELECT id, name, language, tags, created_at, updated_at, last_run_at, run_count 
                     FROM scripts"
         where-clauses []
         params []
         
         where-clauses (if search
                        (conj where-clauses "name LIKE ? OR content LIKE ?")
                        where-clauses)
         params (if search
                 (concat params [(str "%" search "%") (str "%" search "%")])
                 params)
         
         where-clauses (if tags
                        (conj where-clauses "tags LIKE ?")
                        where-clauses)
         params (if tags
                 (conj params (str "%" tags "%"))
                 params)
         
         query (str base-query
                   (when (seq where-clauses)
                     (str " WHERE " (str/join " AND " where-clauses)))
                   " ORDER BY updated_at DESC"
                   (when limit (str " LIMIT " limit))
                   (when offset (str " OFFSET " offset)))]
     
     (jdbc/execute! ds (into [query] params)
                   {:builder-fn rs/as-unqualified-lower-maps}))))

(defn delete-script!
  "Delete a script by ID"
  [id]
  (let [ds (get-connection)]
    ;; First delete related results
    (jdbc/execute! ds ["DELETE FROM script_results WHERE script_id = ?" id])
    ;; Then delete the script
    (let [deleted (jdbc/execute! ds ["DELETE FROM scripts WHERE id = ?" id])]
      {:success (> (first deleted) 0)})))

(defn update-script-run-info!
  "Update script run information"
  [id]
  (let [ds (get-connection)]
    (jdbc/execute! ds
      ["UPDATE scripts SET 
        last_run_at = CURRENT_TIMESTAMP,
        run_count = run_count + 1
        WHERE id = ?" id])))

(defn save-script-result!
  "Save script execution result"
  [{:keys [script-id result error execution-time]}]
  (let [ds (get-connection)
        result-id (str "result-" (System/currentTimeMillis))]
    (jdbc/execute! ds
      ["INSERT INTO script_results (id, script_id, result, error, execution_time)
        VALUES (?, ?, ?, ?, ?)"
       result-id script-id 
       (when result (json/write-str result))
       error execution-time])
    (update-script-run-info! script-id)
    {:success true :id result-id}))

;; Connection operations

(defn save-connection!
  "Save or update a database connection configuration"
  [{:keys [id name type config] :as connection}]
  (let [ds (get-connection)
        conn-id (or id (str "conn-" (System/currentTimeMillis)))
        existing (jdbc/execute-one! ds
                   ["SELECT id FROM saved_connections WHERE id = ?" conn-id]
                   {:builder-fn rs/as-unqualified-lower-maps})]
    (if existing
      ;; Update existing connection
      (do
        (jdbc/execute! ds
          ["UPDATE saved_connections SET 
            name = ?, type = ?, config = ?, 
            updated_at = CURRENT_TIMESTAMP
            WHERE id = ?"
           name type (json/write-str config) conn-id])
        {:success true :id conn-id :action "updated"})
      ;; Insert new connection
      (do
        (jdbc/execute! ds
          ["INSERT INTO saved_connections (id, name, type, config) 
            VALUES (?, ?, ?, ?)"
           conn-id name type (json/write-str config)])
        {:success true :id conn-id :action "created"}))))

(defn list-saved-connections
  "List all saved connections"
  []
  (let [ds (get-connection)
        connections (jdbc/execute! ds
                     ["SELECT * FROM saved_connections ORDER BY name"]
                     {:builder-fn rs/as-unqualified-lower-maps})]
    (map (fn [conn]
           (update conn :config #(json/read-str % :key-fn keyword)))
         connections)))

(defn delete-connection!
  "Delete a saved connection"
  [id]
  (let [ds (get-connection)
        deleted (jdbc/execute! ds ["DELETE FROM saved_connections WHERE id = ?" id])]
    {:success (> (first deleted) 0)}))

;; Snippet operations

(defn save-snippet!
  "Save a code snippet"
  [{:keys [id name description content language category] :as snippet}]
  (let [ds (get-connection)
        snippet-id (or id (str "snippet-" (System/currentTimeMillis)))]
    (jdbc/execute! ds
      ["INSERT OR REPLACE INTO snippets (id, name, description, content, language, category) 
        VALUES (?, ?, ?, ?, ?, ?)"
       snippet-id name description content (or language "clojure") category])
    {:success true :id snippet-id}))

(defn list-snippets
  "List all snippets with optional category filter"
  ([]
   (list-snippets nil))
  ([category]
   (let [ds (get-connection)]
     (if category
       (jdbc/execute! ds
         ["SELECT * FROM snippets WHERE category = ? ORDER BY name" category]
         {:builder-fn rs/as-unqualified-lower-maps})
       (jdbc/execute! ds
         ["SELECT * FROM snippets ORDER BY category, name"]
         {:builder-fn rs/as-unqualified-lower-maps})))))

;; Initialize database on namespace load
(init-database!)