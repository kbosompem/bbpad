(ns bbpad.db.connections
  (:require [babashka.pods :as pods]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def ^:private connection-configs (atom {}))
(def ^:private loaded-pods (atom #{}))

;; Pod loading for different database types
(defn- load-database-pod!
  "Load the appropriate database pod based on type"
  [db-type]
  (when-not (contains? @loaded-pods db-type)
    (case db-type
      :postgresql (do
                    (pods/load-pod 'org.babashka/postgresql "0.1.4")
                    (require '[pod.babashka.postgresql :as pg]))
      :mysql (do
               (pods/load-pod 'org.babashka/mysql "0.1.4") 
               (require '[pod.babashka.mysql :as mysql]))
      :mssql (do
               (pods/load-pod 'org.babashka/mssql "0.1.4")
               (require '[pod.babashka.mssql :as mssql]))
      :sqlite (do
                (pods/load-pod 'org.babashka/go-sqlite3 "0.2.7")
                (require '[pod.babashka.go-sqlite3 :as sqlite]))
      :hsqldb (do
                (pods/load-pod 'org.babashka/hsqldb "0.1.4")
                (require '[pod.babashka.hsqldb :as hsqldb]))
      :h2 (do
            (pods/load-pod 'org.babashka/hsqldb "0.1.4")
            (require '[pod.babashka.hsqldb :as h2]))
      :datalevin (do
                   (pods/load-pod 'huahaiy/datalevin "0.9.22")
                   (require '[pod.huahaiy.datalevin :as d]))
      (throw (ex-info "Unsupported database type" {:type db-type})))
    (swap! loaded-pods conj db-type)))

(defn- build-connection-config
  "Build database connection config from connection info"
  [{:keys [type host port database user password ssl-mode]
    :or {ssl-mode "prefer"}}]
  (case type
    :postgresql {:dbtype "postgresql"
                 :host host
                 :port port
                 :dbname database
                 :user user
                 :password password
                 :ssl true
                 :sslfactory "org.postgresql.ssl.NonValidatingFactory"}
    :mysql {:dbtype "mysql"
            :host host
            :port port
            :dbname database
            :user user
            :password password
            :useSSL (= ssl-mode "require")}
    :mssql {:dbtype "mssql"
            :host host
            :port port
            :databaseName database
            :user user
            :password password
            :encrypt (= ssl-mode "require")}
    :sqlite database  ; SQLite just needs the database file path
    :hsqldb (if (str/includes? database "/")
              database  ; File-based HSQLDB 
              (str "jdbc:hsqldb:mem:" database))
    :h2 (if (str/includes? database "/")
          (str "jdbc:h2:" database)  ; File-based H2
          (str "jdbc:h2:mem:" database))  ; In-memory H2
    :datalevin database  ; Datalevin just needs the database path
    (throw (ex-info "Unsupported database type" {:type type}))))

(defn- test-connection
  "Test database connection"
  [db-config db-type]
  (try
    (load-database-pod! db-type)
    (case db-type
      :postgresql (let [pg (resolve 'pod.babashka.postgresql/execute!)]
                    (pg db-config ["SELECT 1"])
                    true)
      :mysql (let [mysql (resolve 'pod.babashka.mysql/execute!)]
               (mysql db-config ["SELECT 1"])
               true)
      :mssql (let [mssql (resolve 'pod.babashka.mssql/execute!)]
               (mssql db-config ["SELECT 1"])
               true)
      :sqlite (let [sqlite (resolve 'pod.babashka.go-sqlite3/query)]
                (sqlite db-config "SELECT 1")
                true)
      :hsqldb (let [hsqldb (resolve 'pod.babashka.hsqldb/execute!)]
                (hsqldb db-config ["SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS LIMIT 1"])
                true)
      :h2 (let [h2 (resolve 'pod.babashka.hsqldb/execute!)]
            (h2 db-config ["SELECT 1"])
            true)
      :datalevin (let [d (resolve 'pod.huahaiy.datalevin/get-conn)]
                   ;; Test by getting and closing a Datalevin connection
                   (let [conn (d db-config)]
                     (when conn
                       (let [close-fn (resolve 'pod.huahaiy.datalevin/close)]
                         (close-fn conn))
                       true)))
      (throw (ex-info "Unsupported database type for testing" {:type db-type})))
    (catch Exception e
      {:error (.getMessage e)})))

(defn add-connection!
  "Add a new database connection with optional custom ID"
  ([config] (add-connection! (str (gensym "conn-")) config))
  ([conn-id config]
   (try
     (let [db-type (keyword (:type config))
           db-config (build-connection-config (assoc config :type db-type))
           test-result (test-connection db-config db-type)]
       (if (map? test-result)
         test-result
         (let [final-id (if (get @connection-configs conn-id)
                          (str conn-id "-" (System/currentTimeMillis))
                          conn-id)]
           (swap! connection-configs assoc final-id (assoc config 
                                                          :type db-type
                                                          :db-config db-config))
           {:success true :message "Connection added successfully" :id final-id})))
     (catch Exception e
       {:error (.getMessage e)}))))

(defn remove-connection!
  "Remove a database connection"
  [conn-id]
  (swap! connection-configs dissoc conn-id)
  {:success true :message "Connection removed successfully"})

(defn update-connection!
  "Update an existing database connection"
  [conn-id config]
  (if (get @connection-configs conn-id)
    (let [db-type (keyword (:type config))
          db-config (build-connection-config (assoc config :type db-type))
          test-result (test-connection db-config db-type)]
      (if (map? test-result)
        test-result
        (do
          (swap! connection-configs assoc conn-id (assoc config 
                                                          :type db-type
                                                          :db-config db-config))
          {:success true :message "Connection updated successfully"})))
    {:error "Connection not found"}))

(defn list-connections
  "List all configured connections"
  []
  (map (fn [[id config]]
         (-> config
             (select-keys [:name :type :host :port :database :user])
             (assoc :id id :status :connected))) ; Always show as connected for simplicity
       @connection-configs))

(defn- resolve-connection-id
  "Resolve connection name to ID, or return ID if it's already an ID"
  [name-or-id]
  (if-let [config (get @connection-configs name-or-id)]
    ;; It's already a valid ID
    name-or-id
    ;; Try to find by name
    (some (fn [[id config]]
            (when (= (:name config) name-or-id)
              id))
          @connection-configs)))

(defn get-connection-config
  "Get connection configuration by name or ID"
  [name-or-id]
  (when-let [conn-id (resolve-connection-id name-or-id)]
    (get @connection-configs conn-id)))

(defn execute-query!
  "Execute a query against a connection"
  [connection-id query params]
  (if-let [{:keys [type db-config]} (get-connection-config connection-id)]
    (try
      (load-database-pod! type)
      (case type
        :postgresql (let [pg (resolve 'pod.babashka.postgresql/execute!)]
                      {:success true 
                       :result (pg db-config (if (empty? params) [query] (into [query] params)))})
        :mysql (let [mysql (resolve 'pod.babashka.mysql/execute!)]
                 {:success true 
                  :result (mysql db-config (if (empty? params) [query] (into [query] params)))})
        :mssql (let [mssql (resolve 'pod.babashka.mssql/execute!)]
                 {:success true 
                  :result (mssql db-config (if (empty? params) [query] (into [query] params)))})
        :sqlite (let [sqlite (resolve 'pod.babashka.go-sqlite3/query)]
                  {:success true 
                   :result (sqlite db-config query params)})
        :hsqldb (let [hsqldb (resolve 'pod.babashka.hsqldb/execute!)]
                  {:success true 
                   :result (hsqldb db-config (if (empty? params) [query] (into [query] params)))})
        :h2 (let [h2 (resolve 'pod.babashka.hsqldb/execute!)]
              {:success true 
               :result (h2 db-config (if (empty? params) [query] (into [query] params)))})
        :datalevin (let [d (resolve 'pod.huahaiy.datalevin/q)
                         conn-fn (resolve 'pod.huahaiy.datalevin/get-conn)]
                     (let [conn (conn-fn db-config)
                           result (d (edn/read-string query) (resolve 'pod.huahaiy.datalevin/db) conn)]
                       (let [close-fn (resolve 'pod.huahaiy.datalevin/close)]
                         (close-fn conn))
                       {:success true :result result}))
        (throw (ex-info "Unsupported database type" {:type type})))
      (catch Exception e
        {:success false :error (.getMessage e)}))
    {:success false :error "Connection not found"}))

(defn transact-data!
  "Transact data into a Datalevin database"
  [connection-id transaction-data]
  (if-let [{:keys [type db-config]} (get-connection-config connection-id)]
    (if (= type :datalevin)
      (try
        (load-database-pod! type)
        (let [conn-fn (resolve 'pod.huahaiy.datalevin/get-conn)
              transact-fn (resolve 'pod.huahaiy.datalevin/transact!)
              conn (conn-fn db-config)]
          (let [result (transact-fn conn (edn/read-string transaction-data))]
            (let [close-fn (resolve 'pod.huahaiy.datalevin/close)]
              (close-fn conn))
            {:success true :result result}))
        (catch Exception e
          {:success false :error (.getMessage e)}))
      {:success false :error "Transactions are only supported for Datalevin connections"})
    {:success false :error "Connection not found"}))

(defn get-datalevin-stats
  "Get statistics for a Datalevin database"
  [connection-id]
  (if-let [{:keys [type db-config]} (get-connection-config connection-id)]
    (if (= type :datalevin)
      (try
        (load-database-pod! type)
        (let [conn-fn (resolve 'pod.huahaiy.datalevin/get-conn)
              stat-fn (resolve 'pod.huahaiy.datalevin/stat)
              conn (conn-fn db-config)]
          (let [stats (stat-fn conn)]
            (let [close-fn (resolve 'pod.huahaiy.datalevin/close)]
              (close-fn conn))
            {:success true :stats stats}))
        (catch Exception e
          {:success false :error (.getMessage e)}))
      {:success false :error "Statistics are only available for Datalevin connections"})
    {:success false :error "Connection not found"}))

(defn get-schema
  "Get schema information (tables) for a connection"
  [connection-id]
  (if-let [{:keys [type db-config]} (get-connection-config connection-id)]
    (try
      (load-database-pod! type)
      (cond
        (#{:postgresql :mysql :mssql :hsqldb :h2} type)
        (let [execute-fn (case type
                           :postgresql (resolve 'pod.babashka.postgresql/execute!)
                           :mysql (resolve 'pod.babashka.mysql/execute!)
                           :mssql (resolve 'pod.babashka.mssql/execute!)
                           :hsqldb (resolve 'pod.babashka.hsqldb/execute!)
                           :h2 (resolve 'pod.babashka.hsqldb/execute!))
              query (case type
                      :postgresql "SELECT table_name, table_type FROM information_schema.tables WHERE table_schema = 'public'"
                      :mysql "SELECT table_name, table_type FROM information_schema.tables WHERE table_schema = DATABASE()"
                      :mssql "SELECT table_name, table_type FROM information_schema.tables WHERE table_catalog = DB_NAME()"
                      :hsqldb "SELECT table_name, table_type FROM information_schema.tables WHERE table_schema = 'PUBLIC'"
                      :h2 "SELECT table_name, table_type FROM information_schema.tables WHERE table_schema = 'PUBLIC'")
              tables (execute-fn db-config [query])]
          {:success true :tables tables})

        (= type :sqlite)
        (let [sqlite (resolve 'pod.babashka.go-sqlite3/query)
              tables (sqlite db-config "SELECT name as table_name, 'BASE TABLE' as table_type FROM sqlite_master WHERE type='table'" [])]
          {:success true :tables tables})

        (= type :datalevin)
        (let [conn-fn (resolve 'pod.huahaiy.datalevin/get-conn)
              stat-fn (resolve 'pod.huahaiy.datalevin/stat)]
          (let [conn (conn-fn db-config)
                stats (stat-fn conn)]
            (let [close-fn (resolve 'pod.huahaiy.datalevin/close)]
              (close-fn conn))
            {:success true
             :stats {:attributes (get stats :datoms 0)
                     :entities (get stats :entities 0)  
                     :values (get stats :values 0)}}))
        
        :else
        {:error "Schema retrieval not supported for this database type"})
      (catch Exception e
        {:error (.getMessage e)}))
    {:error "Connection not found"}))

(defn get-table-info
  "Get information about a specific table"
  [connection-id table-name]
  (if-let [{:keys [type db-config]} (get-connection-config connection-id)]
    (try
      (load-database-pod! type)
      (case type
        :postgresql (let [pg (resolve 'pod.babashka.postgresql/execute!)
                          columns (pg db-config ["SELECT column_name, data_type, is_nullable FROM information_schema.columns WHERE table_name = ?" table-name])]
                      {:success true :columns columns})
        :mysql (let [mysql (resolve 'pod.babashka.mysql/execute!)
                     columns (mysql db-config ["SELECT column_name, data_type, is_nullable FROM information_schema.columns WHERE table_name = ?" table-name])]
                 {:success true :columns columns})
        :mssql (let [mssql (resolve 'pod.babashka.mssql/execute!)
                     columns (mssql db-config ["SELECT column_name, data_type, is_nullable FROM information_schema.columns WHERE table_name = ?" table-name])]
                 {:success true :columns columns})
        :sqlite (let [sqlite (resolve 'pod.babashka.go-sqlite3/query)
                      columns (sqlite db-config (str "PRAGMA table_info(" table-name ")") [])]
                  {:success true :columns columns})
        :hsqldb (let [hsqldb (resolve 'pod.babashka.hsqldb/execute!)
                      columns (hsqldb db-config ["SELECT column_name, data_type, is_nullable FROM information_schema.columns WHERE table_name = ?" (str/upper-case table-name)])]
                  {:success true :columns columns})
        :h2 (let [h2 (resolve 'pod.babashka.hsqldb/execute!)
                  columns (h2 db-config ["SELECT column_name, data_type, is_nullable FROM information_schema.columns WHERE table_name = ?" (str/upper-case table-name)])]
              {:success true :columns columns})
        :datalevin {:success true :message "Datalevin is a document database - table info not applicable"}
        (throw (ex-info "Unsupported database type" {:type type})))
      (catch Exception e
        {:error (.getMessage e)}))
    {:error "Connection not found"}))