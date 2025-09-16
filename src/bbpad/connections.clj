(ns bbpad.connections
  "Helper functions for using saved database connections in BBPad scripts"
  (:require [babashka.pods :as pods]
            [clojure.string :as str]))

;; This namespace is automatically available in BBPad scripts
;; It provides easy access to saved database connections

(def ^:dynamic *connections* {})

(defn list-connections
  "List all available saved connections"
  []
  (keys *connections*))

(defn get-connection
  "Get a connection by name or ID. Returns connection config."
  [name-or-id]
  (or (get *connections* name-or-id)
      (get *connections* (str name-or-id))
      (some #(when (= (:name (val %)) name-or-id) (val %))
            *connections*)))

(defn with-connection
  "Execute a function with a database connection.
  The connection is automatically loaded with the appropriate pod.

  Example:
    (with-connection \"My PostgreSQL\"
      (fn [conn]
        (query conn \"SELECT * FROM users\")))"
  [connection-name f]
  (if-let [conn-config (get-connection connection-name)]
    (let [{:keys [type config]} conn-config]
      ;; Load the appropriate pod based on type
      (case (keyword type)
        :sqlite (do
                  (pods/load-pod 'org.babashka/go-sqlite3 "0.1.0")
                  (require '[pod.babashka.go-sqlite3 :as sqlite])
                  (f {:db (:database config)
                      :type :sqlite
                      :execute! (fn [query & params]
                                  ((resolve 'pod.babashka.go-sqlite3/execute!)
                                   (:database config)
                                   (if params
                                     (into [query] params)
                                     [query])))
                      :query (fn [query & params]
                               ((resolve 'pod.babashka.go-sqlite3/query)
                                (:database config)
                                (if params
                                  (into [query] params)
                                  query)))}))

        :postgresql (do
                      (pods/load-pod 'org.babashka/postgresql "0.1.4")
                      (require '[pod.babashka.postgresql :as pg])
                      (f {:db config
                          :type :postgresql
                          :execute! (fn [query & params]
                                      ((resolve 'pod.babashka.postgresql/execute!)
                                       {:dbtype "postgresql"
                                        :host (:host config)
                                        :port (:port config)
                                        :dbname (:database config)
                                        :user (:user config)
                                        :password (:password config)}
                                       (if params
                                         (into [query] params)
                                         [query])))
                          :query (fn [query & params]
                                   ((resolve 'pod.babashka.postgresql/execute!)
                                    {:dbtype "postgresql"
                                     :host (:host config)
                                     :port (:port config)
                                     :dbname (:database config)
                                     :user (:user config)
                                     :password (:password config)}
                                    (if params
                                      (into [query] params)
                                      [query])))}))

        :mysql (do
                 (pods/load-pod 'org.babashka/mysql "0.1.4")
                 (require '[pod.babashka.mysql :as mysql])
                 (f {:db config
                     :type :mysql
                     :execute! (fn [query & params]
                                 ((resolve 'pod.babashka.mysql/execute!)
                                  {:dbtype "mysql"
                                   :host (:host config)
                                   :port (:port config)
                                   :dbname (:database config)
                                   :user (:user config)
                                   :password (:password config)}
                                  (if params
                                    (into [query] params)
                                    [query])))
                     :query (fn [query & params]
                              ((resolve 'pod.babashka.mysql/execute!)
                               {:dbtype "mysql"
                                :host (:host config)
                                :port (:port config)
                                :dbname (:database config)
                                :user (:user config)
                                :password (:password config)}
                               (if params
                                 (into [query] params)
                                 [query])))}))

        :datalevin (do
                     (pods/load-pod 'huahaiy/datalevin "0.9.22")
                     (require '[pod.huahaiy.datalevin :as d])
                     (let [conn ((resolve 'pod.huahaiy.datalevin/get-conn)
                                 (:database config))
                           db-fn (resolve 'pod.huahaiy.datalevin/db)
                           q-fn (resolve 'pod.huahaiy.datalevin/q)
                           pull-fn (resolve 'pod.huahaiy.datalevin/pull)
                           entity-fn (resolve 'pod.huahaiy.datalevin/entity)
                           transact-fn (resolve 'pod.huahaiy.datalevin/transact!)
                           close-fn (resolve 'pod.huahaiy.datalevin/close)]
                       (f {:conn conn
                           :type :datalevin
                           :db (fn [] (db-fn conn))
                           :transact! (fn [tx-data]
                                        (transact-fn conn tx-data))
                           :q (fn [query & inputs]
                                (let [db (db-fn conn)]
                                  (apply q-fn query db inputs)))
                           :pull (fn [pattern eid]
                                   (let [db (db-fn conn)]
                                     (pull-fn db pattern eid)))
                           :entity (fn [eid]
                                     (let [db (db-fn conn)]
                                       (entity-fn db eid)))
                           :close (fn []
                                    (close-fn conn))})))

        (throw (ex-info "Unsupported database type" {:type type}))))
    (throw (ex-info "Connection not found" {:name connection-name}))))

(defn query
  "Execute a query on a saved connection by name.
  Returns query results.

  Example:
    (query \"My Database\" \"SELECT * FROM users WHERE age > ?\" 18)"
  [connection-name sql & params]
  (with-connection connection-name
    (fn [conn]
      (apply (:query conn) sql params))))

(defn execute!
  "Execute a statement on a saved connection by name.
  Returns affected rows or nil.

  Example:
    (execute! \"My Database\" \"INSERT INTO users (name, age) VALUES (?, ?)\" \"John\" 30)"
  [connection-name sql & params]
  (with-connection connection-name
    (fn [conn]
      (apply (:execute! conn) sql params))))

(defn query-one
  "Execute a query and return only the first result.

  Example:
    (query-one \"My Database\" \"SELECT * FROM users WHERE id = ?\" 1)"
  [connection-name sql & params]
  (first (apply query connection-name sql params)))

(defn query-value
  "Execute a query and return a single value from the first result.

  Example:
    (query-value \"My Database\" \"SELECT COUNT(*) as count FROM users\")
    ; Returns just the count number"
  [connection-name sql & params]
  (let [result (apply query-one connection-name sql params)]
    (when result
      (if (= 1 (count result))
        (first (vals result))
        result))))

;; Transaction helpers for databases that support them
(defn with-transaction
  "Execute a function within a database transaction.
  Note: Not all database types support transactions.

  Example:
    (with-transaction \"My Database\"
      (fn []
        (execute! \"My Database\" \"INSERT INTO users (name) VALUES (?)\" \"Alice\")
        (execute! \"My Database\" \"INSERT INTO users (name) VALUES (?)\" \"Bob\")))"
  [connection-name f]
  (try
    (execute! connection-name "BEGIN")
    (let [result (f)]
      (execute! connection-name "COMMIT")
      result)
    (catch Exception e
      (try
        (execute! connection-name "ROLLBACK")
        (catch Exception _))
      (throw e))))

;; Helper to create tables
(defn create-table!
  "Helper to create a table if it doesn't exist.

  Example:
    (create-table! \"My Database\" :users
      {:id \"INTEGER PRIMARY KEY\"
       :name \"TEXT NOT NULL\"
       :email \"TEXT UNIQUE\"
       :created_at \"TIMESTAMP DEFAULT CURRENT_TIMESTAMP\"})"
  [connection-name table-name columns]
  (let [col-defs (str/join ", "
                          (map (fn [[col-name col-def]]
                                 (str (name col-name) " " col-def))
                               columns))
        sql (format "CREATE TABLE IF NOT EXISTS %s (%s)"
                   (name table-name)
                   col-defs)]
    (execute! connection-name sql)))

;; Helper to insert data
(defn insert!
  "Helper to insert data into a table.

  Example:
    (insert! \"My Database\" :users
      {:name \"Alice\" :email \"alice@example.com\"})"
  [connection-name table-name data]
  (let [columns (keys data)
        values (vals data)
        placeholders (str/join ", " (repeat (count values) "?"))
        col-names (str/join ", " (map name columns))
        sql (format "INSERT INTO %s (%s) VALUES (%s)"
                   (name table-name)
                   col-names
                   placeholders)]
    (apply execute! connection-name sql values)))

;; Helper for batch inserts
(defn insert-batch!
  "Helper to insert multiple rows at once.

  Example:
    (insert-batch! \"My Database\" :users
      [{:name \"Alice\" :email \"alice@example.com\"}
       {:name \"Bob\" :email \"bob@example.com\"}])"
  [connection-name table-name rows]
  (doseq [row rows]
    (insert! connection-name table-name row)))

;; Datalevin-specific helpers
(defn transact!
  "Execute a Datalevin transaction on a saved connection.

  Example:
    (transact! \"My Datalevin\"
      [{:person/name \"Alice\" :person/age 30}])"
  [connection-name tx-data]
  (with-connection connection-name
    (fn [conn]
      (if (= (:type conn) :datalevin)
        ((:transact! conn) tx-data)
        (throw (ex-info "Not a Datalevin connection" {:name connection-name :type (:type conn)}))))))

(defn datalevin-q
  "Execute a Datalog query on a Datalevin connection.

  Example:
    (datalevin-q \"My Datalevin\"
      '[:find ?name ?age
        :where
        [?e :person/name ?name]
        [?e :person/age ?age]])"
  [connection-name query & inputs]
  (with-connection connection-name
    (fn [conn]
      (if (= (:type conn) :datalevin)
        (apply (:q conn) query inputs)
        (throw (ex-info "Not a Datalevin connection" {:name connection-name :type (:type conn)}))))))

(defn datalevin-pull
  "Pull entity data from a Datalevin connection.

  Example:
    (datalevin-pull \"My Datalevin\" '[*] entity-id)"
  [connection-name pattern eid]
  (with-connection connection-name
    (fn [conn]
      (if (= (:type conn) :datalevin)
        ((:pull conn) pattern eid)
        (throw (ex-info "Not a Datalevin connection" {:name connection-name :type (:type conn)}))))))

(defn datalevin-entity
  "Get an entity from a Datalevin connection.

  Example:
    (datalevin-entity \"My Datalevin\" entity-id)"
  [connection-name eid]
  (with-connection connection-name
    (fn [conn]
      (if (= (:type conn) :datalevin)
        ((:entity conn) eid)
        (throw (ex-info "Not a Datalevin connection" {:name connection-name :type (:type conn)}))))))

(defn with-datalevin
  "Execute a function with a Datalevin connection and its database.

  Example:
    (with-datalevin \"My Datalevin\"
      (fn [conn db]
        (d/q '[:find ?e :where [?e :person/name]] db)))"
  [connection-name f]
  (with-connection connection-name
    (fn [conn]
      (if (= (:type conn) :datalevin)
        (f conn ((:db conn)))
        (throw (ex-info "Not a Datalevin connection" {:name connection-name :type (:type conn)}))))))