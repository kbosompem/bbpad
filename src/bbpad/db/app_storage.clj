(ns bbpad.db.app-storage
  "SQLite database for storing BBPad scripts and connections"
  (:require [babashka.pods :as pods]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [bbpad.core.config :as config]))

(pods/load-pod 'org.babashka/go-sqlite3 "0.1.0")

(require '[pod.babashka.go-sqlite3 :as sqlite])

(def ^:private db-path (str (config/get-config-dir) "/bbpad.db"))

(defn- init-db!
  "Initialize the database with required tables"
  []
  ;; Enhanced scripts table with collections support
  (sqlite/execute! db-path "
    CREATE TABLE IF NOT EXISTS scripts (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      content TEXT NOT NULL,
      language TEXT DEFAULT 'clojure',
      tags TEXT,
      collection_id TEXT,
      description TEXT,
      is_favorite INTEGER DEFAULT 0,
      created_at TEXT DEFAULT CURRENT_TIMESTAMP,
      updated_at TEXT DEFAULT CURRENT_TIMESTAMP,
      last_run_at TEXT,
      run_count INTEGER DEFAULT 0,
      version INTEGER DEFAULT 1,
      FOREIGN KEY (collection_id) REFERENCES collections(id)
    )
  ")
  
  ;; Collections table for organizing scripts
  (sqlite/execute! db-path "
    CREATE TABLE IF NOT EXISTS collections (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      description TEXT,
      color TEXT DEFAULT '#3b82f6',
      icon TEXT DEFAULT 'folder',
      parent_id TEXT,
      sort_order INTEGER DEFAULT 0,
      created_at TEXT DEFAULT CURRENT_TIMESTAMP,
      updated_at TEXT DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (parent_id) REFERENCES collections(id)
    )
  ")
  
  ;; Script versions for history tracking
  (sqlite/execute! db-path "
    CREATE TABLE IF NOT EXISTS script_versions (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      script_id TEXT NOT NULL,
      version INTEGER NOT NULL,
      name TEXT NOT NULL,
      content TEXT NOT NULL,
      description TEXT,
      created_at TEXT DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (script_id) REFERENCES scripts(id),
      UNIQUE(script_id, version)
    )
  ")
  
  ;; Enhanced script results with more metadata
  (sqlite/execute! db-path "
    CREATE TABLE IF NOT EXISTS script_results (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      script_id TEXT NOT NULL,
      script_version INTEGER DEFAULT 1,
      result TEXT,
      error TEXT,
      execution_time INTEGER,
      memory_usage INTEGER,
      exit_code INTEGER DEFAULT 0,
      parameters TEXT,
      created_at TEXT DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (script_id) REFERENCES scripts(id)
    )
  ")
  
  ;; Execution history summary
  (sqlite/execute! db-path "
    CREATE TABLE IF NOT EXISTS execution_history (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      script_id TEXT NOT NULL,
      execution_count INTEGER DEFAULT 1,
      last_execution TEXT DEFAULT CURRENT_TIMESTAMP,
      success_count INTEGER DEFAULT 0,
      error_count INTEGER DEFAULT 0,
      avg_execution_time REAL DEFAULT 0,
      total_execution_time INTEGER DEFAULT 0,
      date TEXT NOT NULL,
      FOREIGN KEY (script_id) REFERENCES scripts(id),
      UNIQUE(script_id, date)
    )
  ")
  
  (sqlite/execute! db-path "
    CREATE TABLE IF NOT EXISTS connections (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      type TEXT NOT NULL,
      config TEXT NOT NULL,
      created_at TEXT DEFAULT CURRENT_TIMESTAMP,
      updated_at TEXT DEFAULT CURRENT_TIMESTAMP
    )
  ")
  
  (sqlite/execute! db-path "
    CREATE TABLE IF NOT EXISTS tab_sessions (
      id INTEGER PRIMARY KEY,
      tab_data TEXT NOT NULL,
      active_tab TEXT,
      created_at TEXT DEFAULT CURRENT_TIMESTAMP,
      updated_at TEXT DEFAULT CURRENT_TIMESTAMP
    )
  ")
  
  ;; App preferences and settings
  (sqlite/execute! db-path "
    CREATE TABLE IF NOT EXISTS app_settings (
      key TEXT PRIMARY KEY,
      value TEXT NOT NULL,
      updated_at TEXT DEFAULT CURRENT_TIMESTAMP
    )
  ")
  
  ;; Recent scripts for quick access
  (sqlite/execute! db-path "
    CREATE TABLE IF NOT EXISTS recent_scripts (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      script_id TEXT NOT NULL,
      accessed_at TEXT DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (script_id) REFERENCES scripts(id)
    )
  ")
  
  ;; Create default collection if none exists
  (let [existing (sqlite/query db-path ["SELECT COUNT(*) as count FROM collections"])]
    (when (= 0 (:count (first existing)))
      (sqlite/execute! db-path 
                      ["INSERT INTO collections (id, name, description, icon) VALUES (?, ?, ?, ?)"
                       "default" "My Scripts" "Default collection for scripts" "folder"]))))

(defn save-script!
  "Save a script to the database with versioning support"
  [{:keys [id name content language tags collection-id description is-favorite] :as script}]
  (init-db!)
  (let [script-id (or id (str "script-" (System/currentTimeMillis)))
        now (str (java.time.Instant/now))]
    (try
      ;; Check if script exists
      (if id
        ;; Update existing script and create new version
        (let [existing (sqlite/query db-path ["SELECT version FROM scripts WHERE id = ?" id])
              current-version (or (:version (first existing)) 1)
              new-version (inc current-version)]
          ;; Save current version to script_versions
          (let [current-script (sqlite/query db-path ["SELECT name, content FROM scripts WHERE id = ?" id])]
            (when-let [current (first current-script)]
              (sqlite/execute! db-path 
                              ["INSERT INTO script_versions (script_id, version, name, content, created_at) VALUES (?, ?, ?, ?, ?)"
                               id current-version (:name current) (:content current) now])))
          ;; Update main script record
          (sqlite/execute! db-path 
                          ["UPDATE scripts SET name = ?, content = ?, language = ?, tags = ?, collection_id = ?, description = ?, is_favorite = ?, updated_at = ?, version = ? WHERE id = ?"
                           name content (or language "clojure") (or tags "") collection-id description (if is-favorite 1 0) now new-version id]))
        ;; Insert new script
        (let [coll-id (or collection-id "default")]
          (sqlite/execute! db-path 
                          ["INSERT INTO scripts (id, name, content, language, tags, collection_id, description, is_favorite, created_at, updated_at, version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                           script-id name content (or language "clojure") (or tags "") coll-id description (if is-favorite 1 0) now now 1])
          ;; Save initial version
          (sqlite/execute! db-path 
                          ["INSERT INTO script_versions (script_id, version, name, content, created_at) VALUES (?, ?, ?, ?, ?)"
                           script-id 1 name content now])))
      {:success true :id (or id script-id)}
      (catch Exception e
        {:success false :error (.getMessage e)}))))

(defn get-script
  "Get a script by ID"
  [id]
  (init-db!)
  (try
    (let [results (sqlite/query db-path 
                               ["SELECT * FROM scripts WHERE id = ?" id])]
      (first results))
    (catch Exception e
      (println "Error getting script:" (.getMessage e))
      nil)))

(defn list-scripts
  "List all scripts with optional filters"
  [& [options]]
  (init-db!)
  (try
    (println "Listing scripts from:" db-path)
    (let [results (sqlite/query db-path ["SELECT * FROM scripts ORDER BY updated_at DESC"])]
      (println "Found" (count results) "scripts:")
      (doseq [script results]
        (println "  -" (:name script) "(" (:id script) ")"))
      results)
    (catch Exception e
      (println "Error listing scripts:" (.getMessage e))
      [])))

(defn delete-script!
  "Delete a script"
  [id]
  (init-db!)
  (try
    (sqlite/execute! db-path ["DELETE FROM scripts WHERE id = ?" id])
    (sqlite/execute! db-path ["DELETE FROM script_results WHERE script_id = ?" id])
    {:success true}
    (catch Exception e
      {:success false :error (.getMessage e)})))


(defn get-script-results
  "Get execution results for a script"
  [script-id & {:keys [limit] :or {limit 10}}]
  (init-db!)
  (try
    (sqlite/query db-path 
                 "SELECT * FROM script_results WHERE script_id = ? ORDER BY created_at DESC LIMIT ?"
                 [script-id limit])
    (catch Exception e
      (println "Error getting script results:" (.getMessage e))
      [])))

;; Connection storage functions
(defn save-connection!
  "Save a database connection"
  [{:keys [id name type config] :as connection}]
  (init-db!)
  (let [conn-id (or id (str "conn-" (System/currentTimeMillis)))
        now (str (java.time.Instant/now))]
    (try
      (let [existing (sqlite/query db-path
                                  ["SELECT id FROM connections WHERE id = ?" conn-id])]
        (if (seq existing)
          ;; Update existing connection
          (do
            (sqlite/execute! db-path
                            ["UPDATE connections SET name = ?, type = ?, config = ?, updated_at = ? WHERE id = ?"
                             name type (json/write-str config) now conn-id]))
          ;; Insert new connection
          (do
            (sqlite/execute! db-path
                            ["INSERT INTO connections (id, name, type, config, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)"
                             conn-id name type (json/write-str config) now now]))))
      {:success true :id conn-id}
      (catch Exception e
        {:success false :error (.getMessage e)}))))

(defn get-connection
  "Get a connection by ID"
  [id]
  (init-db!)
  (try
    (let [results (sqlite/query db-path 
                               "SELECT * FROM connections WHERE id = ?" 
                               [id])]
      (when-let [conn (first results)]
        (update conn :config #(json/read-str % :key-fn keyword))))
    (catch Exception e
      (println "Error getting connection:" (.getMessage e))
      nil)))

(defn list-connections
  "List all connections"
  []
  (init-db!)
  (try
    (let [results (sqlite/query db-path ["SELECT * FROM connections ORDER BY name"])]
      (map #(update % :config (fn [config] (json/read-str config :key-fn keyword))) results))
    (catch Exception e
      (println "Error listing connections:" (.getMessage e))
      [])))

(defn delete-connection!
  "Delete a connection"
  [id]
  (init-db!)
  (try
    (sqlite/execute! db-path ["DELETE FROM connections WHERE id = ?" id])
    {:success true}
    (catch Exception e
      {:success false :error (.getMessage e)})))

;; Tab session management
(defn save-tab-session!
  "Save the current tab session"
  [{:keys [tabs active-tab]}]
  (init-db!)
  (let [now (str (java.time.Instant/now))]
    (try
      ;; Clear existing session (only keep one session for now)
      (sqlite/execute! db-path "DELETE FROM tab_sessions")
      
      ;; Save new session
      (sqlite/execute! db-path 
                      ["INSERT INTO tab_sessions (tab_data, active_tab, created_at, updated_at) VALUES (?, ?, ?, ?)"
                       (json/write-str tabs) active-tab now now])
      {:success true}
      (catch Exception e
        {:success false :error (.getMessage e)}))))

(defn load-tab-session
  "Load the saved tab session"
  []
  (init-db!)
  (try
    (let [results (sqlite/query db-path ["SELECT * FROM tab_sessions ORDER BY updated_at DESC LIMIT 1"])]
      (when-let [session (first results)]
        {:success true
         :tabs (json/read-str (:tab_data session) :key-fn keyword)
         :active-tab (:active_tab session)}))
    (catch Exception e
      (println "Error loading tab session:" (.getMessage e))
      {:success false :error (.getMessage e)})))

;; Collections management functions
(defn create-collection!
  "Create a new collection"
  [{:keys [id name description color icon parent-id sort-order]}]
  (init-db!)
  (let [collection-id (or id (str "collection-" (System/currentTimeMillis)))
        now (str (java.time.Instant/now))]
    (try
      (sqlite/execute! db-path 
                      ["INSERT INTO collections (id, name, description, color, icon, parent_id, sort_order, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
                       collection-id name description (or color "#3b82f6") (or icon "folder") parent-id (or sort-order 0) now now])
      {:success true :id collection-id}
      (catch Exception e
        {:success false :error (.getMessage e)}))))

(defn list-collections
  "List all collections with hierarchy"
  []
  (init-db!)
  (try
    (sqlite/query db-path ["SELECT * FROM collections ORDER BY parent_id, sort_order, name"])
    (catch Exception e
      (println "Error listing collections:" (.getMessage e))
      [])))

(defn get-collection
  "Get a collection by ID"
  [id]
  (init-db!)
  (try
    (first (sqlite/query db-path ["SELECT * FROM collections WHERE id = ?" id]))
    (catch Exception e
      (println "Error getting collection:" (.getMessage e))
      nil)))

(defn update-collection!
  "Update a collection"
  [{:keys [id name description color icon parent-id sort-order]}]
  (init-db!)
  (let [now (str (java.time.Instant/now))]
    (try
      (sqlite/execute! db-path 
                      ["UPDATE collections SET name = ?, description = ?, color = ?, icon = ?, parent_id = ?, sort_order = ?, updated_at = ? WHERE id = ?"
                       name description color icon parent-id sort-order now id])
      {:success true}
      (catch Exception e
        {:success false :error (.getMessage e)}))))

(defn delete-collection!
  "Delete a collection and move scripts to default collection"
  [id]
  (init-db!)
  (try
    ;; Move scripts to default collection
    (sqlite/execute! db-path ["UPDATE scripts SET collection_id = 'default' WHERE collection_id = ?" id])
    ;; Delete the collection
    (sqlite/execute! db-path ["DELETE FROM collections WHERE id = ?" id])
    {:success true}
    (catch Exception e
      {:success false :error (.getMessage e)})))

;; Script versioning functions
(defn get-script-versions
  "Get all versions of a script"
  [script-id]
  (init-db!)
  (try
    (sqlite/query db-path 
                 ["SELECT * FROM script_versions WHERE script_id = ? ORDER BY version DESC" script-id])
    (catch Exception e
      (println "Error getting script versions:" (.getMessage e))
      [])))

(defn get-script-version
  "Get a specific version of a script"
  [script-id version]
  (init-db!)
  (try
    (first (sqlite/query db-path 
                        ["SELECT * FROM script_versions WHERE script_id = ? AND version = ?" script-id version]))
    (catch Exception e
      (println "Error getting script version:" (.getMessage e))
      nil)))

(defn restore-script-version!
  "Restore a script to a specific version"
  [script-id version]
  (init-db!)
  (try
    (let [version-data (get-script-version script-id version)
          now (str (java.time.Instant/now))]
      (when version-data
        ;; Save current as new version first
        (let [current (get-script script-id)
              next-version (inc (:version current))]
          (sqlite/execute! db-path 
                          ["INSERT INTO script_versions (script_id, version, name, content, created_at) VALUES (?, ?, ?, ?, ?)"
                           script-id (:version current) (:name current) (:content current) now])
          ;; Update script to restored version
          (sqlite/execute! db-path 
                          ["UPDATE scripts SET name = ?, content = ?, updated_at = ?, version = ? WHERE id = ?"
                           (:name version-data) (:content version-data) now next-version script-id]))
        {:success true}))
    (catch Exception e
      {:success false :error (.getMessage e)})))

;; Enhanced execution tracking
(defn save-script-result!
  "Save a script execution result with enhanced tracking"
  [{:keys [script-id result error execution-time memory-usage exit-code parameters script-version]}]
  (init-db!)
  (try
    ;; Update script run count and last run time
    (let [now (str (java.time.Instant/now))
          today (str (.toLocalDate (java.time.Instant/parse now)))]
      (sqlite/execute! db-path 
                      ["UPDATE scripts SET last_run_at = ?, run_count = run_count + 1 WHERE id = ?"
                       now script-id])
      
      ;; Save detailed result
      (sqlite/execute! db-path 
                      ["INSERT INTO script_results (script_id, script_version, result, error, execution_time, memory_usage, exit_code, parameters, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
                       script-id 
                       (or script-version 1)
                       (when result (json/write-str result))
                       error 
                       execution-time
                       memory-usage
                       (or exit-code 0)
                       (when parameters (json/write-str parameters))
                       now])
      
      ;; Update execution history summary
      (let [success? (nil? error)
            existing (first (sqlite/query db-path 
                                         ["SELECT * FROM execution_history WHERE script_id = ? AND date = ?" script-id today]))]
        (if existing
          ;; Update existing record
          (sqlite/execute! db-path 
                          ["UPDATE execution_history SET 
                            execution_count = execution_count + 1,
                            last_execution = ?,
                            success_count = success_count + ?,
                            error_count = error_count + ?,
                            total_execution_time = total_execution_time + ?,
                            avg_execution_time = (total_execution_time + ?) / (execution_count + 1)
                            WHERE script_id = ? AND date = ?"
                           now 
                           (if success? 1 0)
                           (if success? 0 1)
                           (or execution-time 0)
                           (or execution-time 0)
                           script-id today])
          ;; Create new record
          (sqlite/execute! db-path 
                          ["INSERT INTO execution_history (script_id, execution_count, last_execution, success_count, error_count, avg_execution_time, total_execution_time, date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                           script-id 1 now 
                           (if success? 1 0)
                           (if success? 0 1)
                           (or execution-time 0)
                           (or execution-time 0)
                           today])))
      {:success true})
    (catch Exception e
      {:success false :error (.getMessage e)})))

;; Recent scripts tracking
(defn add-recent-script!
  "Add a script to recent scripts list"
  [script-id]
  (init-db!)
  (try
    (let [now (str (java.time.Instant/now))]
      ;; Remove existing entry if present
      (sqlite/execute! db-path ["DELETE FROM recent_scripts WHERE script_id = ?" script-id])
      ;; Add new entry
      (sqlite/execute! db-path 
                      ["INSERT INTO recent_scripts (script_id, accessed_at) VALUES (?, ?)" script-id now])
      ;; Keep only last 20 entries
      (sqlite/execute! db-path 
                      "DELETE FROM recent_scripts WHERE id NOT IN (SELECT id FROM recent_scripts ORDER BY accessed_at DESC LIMIT 20)")
      {:success true})
    (catch Exception e
      {:success false :error (.getMessage e)})))

(defn get-recent-scripts
  "Get recently accessed scripts"
  [& {:keys [limit] :or {limit 10}}]
  (init-db!)
  (try
    (sqlite/query db-path 
                 ["SELECT s.*, rs.accessed_at 
                   FROM scripts s 
                   JOIN recent_scripts rs ON s.id = rs.script_id 
                   ORDER BY rs.accessed_at DESC 
                   LIMIT ?" limit])
    (catch Exception e
      (println "Error getting recent scripts:" (.getMessage e))
      [])))

;; Favorites management
(defn toggle-script-favorite!
  "Toggle favorite status of a script"
  [script-id]
  (init-db!)
  (try
    (sqlite/execute! db-path 
                    ["UPDATE scripts SET is_favorite = 1 - is_favorite WHERE id = ?" script-id])
    {:success true}
    (catch Exception e
      {:success false :error (.getMessage e)})))

(defn get-favorite-scripts
  "Get all favorite scripts"
  []
  (init-db!)
  (try
    (sqlite/query db-path ["SELECT * FROM scripts WHERE is_favorite = 1 ORDER BY name"])
    (catch Exception e
      (println "Error getting favorite scripts:" (.getMessage e))
      [])))

;; App settings management
(defn save-setting!
  "Save an application setting"
  [key value]
  (init-db!)
  (try
    (let [now (str (java.time.Instant/now))]
      (sqlite/execute! db-path 
                      ["INSERT OR REPLACE INTO app_settings (key, value, updated_at) VALUES (?, ?, ?)"
                       key (json/write-str value) now])
      {:success true})
    (catch Exception e
      {:success false :error (.getMessage e)})))

(defn get-setting
  "Get an application setting"
  [key default-value]
  (init-db!)
  (try
    (let [result (first (sqlite/query db-path ["SELECT value FROM app_settings WHERE key = ?" key]))]
      (if result
        (json/read-str (:value result) :key-fn keyword)
        default-value))
    (catch Exception e
      (println "Error getting setting:" (.getMessage e))
      default-value)))

;; Enhanced script listing with collections
(defn list-scripts-by-collection
  "List scripts organized by collection"
  [& [collection-id]]
  (init-db!)
  (try
    (let [query (if collection-id
                  ["SELECT s.*, c.name as collection_name, c.color as collection_color 
                    FROM scripts s 
                    LEFT JOIN collections c ON s.collection_id = c.id 
                    WHERE s.collection_id = ? 
                    ORDER BY s.name" collection-id]
                  ["SELECT s.*, c.name as collection_name, c.color as collection_color 
                    FROM scripts s 
                    LEFT JOIN collections c ON s.collection_id = c.id 
                    ORDER BY c.name, s.name"])]
      (sqlite/query db-path query))
    (catch Exception e
      (println "Error listing scripts by collection:" (.getMessage e))
      [])))

;; Execution history and statistics
(defn get-execution-statistics
  "Get execution statistics for scripts"
  [& {:keys [script-id days] :or {days 30}}]
  (init-db!)
  (try
    (let [since-date (str (.minusDays (java.time.LocalDate/now) days))
          base-query "SELECT script_id, s.name as script_name,
                             SUM(execution_count) as total_executions,
                             SUM(success_count) as total_success,
                             SUM(error_count) as total_errors,
                             AVG(avg_execution_time) as avg_time,
                             MAX(last_execution) as last_run
                      FROM execution_history eh
                      JOIN scripts s ON eh.script_id = s.id
                      WHERE eh.date >= ?"
          query (if script-id
                  (str base-query " AND eh.script_id = ? GROUP BY script_id, s.name ORDER BY total_executions DESC")
                  (str base-query " GROUP BY script_id, s.name ORDER BY total_executions DESC"))]
      (if script-id
        (sqlite/query db-path [query since-date script-id])
        (sqlite/query db-path [query since-date])))
    (catch Exception e
      (println "Error getting execution statistics:" (.getMessage e))
      [])))