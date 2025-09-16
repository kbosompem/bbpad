;; Seamless Database Connection Usage in BBPad
;;
;; This script demonstrates the seamless way to use saved connections.
;; First save a connection through the UI, then reference it by name.

;; The connection helpers are automatically available
(require '[bbpad.connections :as conn])

;; List all your saved connections
(println "Your saved connections:")
(doseq [conn-id (conn/list-connections)]
  (let [connection (conn/get-connection conn-id)]
    (println (format "  - %s (%s)" (:name connection) (:type connection)))))

;; Example: Using a saved connection by name
;; Replace "My Database" with your actual connection name

(comment
  ;; Simple query
  (conn/query "My Database" "SELECT * FROM users")

  ;; Query with parameters
  (conn/query "My Database"
              "SELECT * FROM users WHERE age > ?"
              25)

  ;; Get a single value
  (conn/query-value "My Database"
                    "SELECT COUNT(*) FROM users")

  ;; Insert data
  (conn/execute! "My Database"
                 "INSERT INTO users (name, email) VALUES (?, ?)"
                 "John Doe" "john@example.com")

  ;; Using helper functions
  (conn/create-table! "My Database" :products
                      {:id "INTEGER PRIMARY KEY"
                       :name "TEXT NOT NULL"
                       :price "DECIMAL(10,2)"})

  (conn/insert! "My Database" :products
                {:name "Widget" :price 19.99})

  ;; Batch insert
  (conn/insert-batch! "My Database" :products
                      [{:name "Gadget" :price 29.99}
                       {:name "Gizmo" :price 39.99}])

  ;; Transaction example
  (conn/with-transaction "My Database"
    (fn []
      (conn/execute! "My Database" "DELETE FROM temp_data")
      (conn/insert! "My Database" :temp_data {:value 100}))))

;; Working example with SQLite
(let [db-file "demo.db"]
  ;; Create a local SQLite database for demonstration
  (require '[babashka.pods :as pods])
  (pods/load-pod 'org.babashka/go-sqlite3 "0.1.0")
  (require '[pod.babashka.go-sqlite3 :as sqlite])

  ;; Create demo table
  (sqlite/execute! db-file
    ["CREATE TABLE IF NOT EXISTS demo (
      id INTEGER PRIMARY KEY,
      message TEXT,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )"])

  ;; Insert sample data
  (sqlite/execute! db-file
    ["INSERT INTO demo (message) VALUES (?)"
     "Hello from BBPad!"])

  ;; Query and display
  (println "\nDemo table contents:")
  (let [results (sqlite/query db-file "SELECT * FROM demo")]
    (doseq [row results]
      (println "  -" (:message row) "at" (:created_at row))))

  ;; If you save this database as a connection named "demo.db"
  ;; you could then use:
  ;; (conn/query "demo.db" "SELECT * FROM demo")

  {:status "Demo complete"
   :tip "Save a connection in BBPad's UI to use it by name!"})