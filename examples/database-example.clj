;; Example BBPad script demonstrating database connection usage
;;
;; This script shows how to use saved database connections seamlessly in BBPad.
;; First, save a connection using the Connections dialog in BBPad, then run this script.

;; The bbpad.connections namespace is automatically available
(require '[bbpad.connections :as conn])

;; List all available saved connections
(println "Available connections:")
(doseq [conn-name (conn/list-connections)]
  (println " -" conn-name))

(println "\n---\n")

;; Example 1: Create a test database and table
;; This uses an in-memory SQLite database
(let [test-db "test.db"]
  ;; First, save a SQLite connection pointing to "test.db" in BBPad
  ;; Then you can use it by name

  ;; For this example, we'll use direct SQLite pod access
  (require '[babashka.pods :as pods])
  (pods/load-pod 'org.babashka/go-sqlite3 "0.1.0")
  (require '[pod.babashka.go-sqlite3 :as sqlite])

  ;; Create a users table
  (sqlite/execute! test-db ["CREATE TABLE IF NOT EXISTS users (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            name TEXT NOT NULL,
                            email TEXT UNIQUE NOT NULL,
                            age INTEGER,
                            department TEXT,
                            created_at DATETIME DEFAULT CURRENT_TIMESTAMP)"])

  ;; Insert sample data
  (sqlite/execute! test-db ["INSERT OR REPLACE INTO users (name, email, age, department) VALUES (?, ?, ?, ?)"
                           "Alice Johnson" "alice@example.com" 28 "Engineering"])
  (sqlite/execute! test-db ["INSERT OR REPLACE INTO users (name, email, age, department) VALUES (?, ?, ?, ?)"
                           "Bob Smith" "bob@example.com" 35 "Marketing"])
  (sqlite/execute! test-db ["INSERT OR REPLACE INTO users (name, email, age, department) VALUES (?, ?, ?, ?)"
                           "Carol White" "carol@example.com" 42 "Sales"])
  (sqlite/execute! test-db ["INSERT OR REPLACE INTO users (name, email, age, department) VALUES (?, ?, ?, ?)"
                           "David Brown" "david@example.com" 31 "Engineering"])
  (sqlite/execute! test-db ["INSERT OR REPLACE INTO users (name, email, age, department) VALUES (?, ?, ?, ?)"
                           "Eve Davis" "eve@example.com" 26 "HR"])

  (println "Created test database with sample users table"))

(println "\n---\n")

;; Example 2: If you have a saved connection named "test.db" or similar
;; Uncomment and modify the connection name to match your saved connection

#_(let [conn-name "test.db"] ; Replace with your actual connection name
  ;; Query all users
  (println "All users:")
  (let [users (conn/query conn-name "SELECT * FROM users ORDER BY name")]
    (doseq [user users]
      (println (format "  %s (%s) - %s, Age %d"
                      (:name user)
                      (:email user)
                      (:department user)
                      (:age user)))))

  (println "\n---\n")

  ;; Query with parameters
  (println "Users in Engineering department:")
  (let [engineers (conn/query conn-name
                              "SELECT * FROM users WHERE department = ?"
                              "Engineering")]
    (doseq [eng engineers]
      (println (format "  %s - Age %d" (:name eng) (:age eng)))))

  (println "\n---\n")

  ;; Get a single value
  (let [avg-age (conn/query-value conn-name
                                  "SELECT AVG(age) as avg_age FROM users")]
    (println (format "Average age: %.1f" (double avg-age))))

  (println "\n---\n")

  ;; Insert a new user
  (conn/execute! conn-name
                "INSERT INTO users (name, email, age, department) VALUES (?, ?, ?, ?)"
                "Frank Miller" "frank@example.com" 29 "Engineering")
  (println "Added new user: Frank Miller")

  ;; Using the helper functions
  (conn/insert! conn-name :users
               {:name "Grace Lee"
                :email "grace@example.com"
                :age 33
                :department "Marketing"})
  (println "Added new user: Grace Lee")

  ;; Count total users
  (let [total (conn/query-value conn-name "SELECT COUNT(*) FROM users")]
    (println (format "\nTotal users: %d" total))))

;; Example 3: Direct usage without saved connections
(println "\n---\n")
(println "Direct query example (without saved connection):")

(require '[babashka.pods :as pods])
(pods/load-pod 'org.babashka/go-sqlite3 "0.1.0")
(require '[pod.babashka.go-sqlite3 :as sqlite])

(let [results (sqlite/query "test.db" "SELECT name, department, age FROM users WHERE age > ?" [30])]
  (println "Users over 30:")
  (doseq [user results]
    (println (format "  %s (%s) - Age %d"
                    (:name user)
                    (:department user)
                    (:age user)))))

;; Return summary statistics
{:total-users (count (sqlite/query "test.db" "SELECT * FROM users"))
 :departments (distinct (map :department (sqlite/query "test.db" "SELECT DISTINCT department FROM users")))
 :age-range {:min (:min_age (first (sqlite/query "test.db" "SELECT MIN(age) as min_age FROM users")))
             :max (:max_age (first (sqlite/query "test.db" "SELECT MAX(age) as max_age FROM users")))}}