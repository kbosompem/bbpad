(ns bbpad.examples.datalevin-transactions
  "Examples of transacting data into Datalevin database using babashka pods"
  (:require [babashka.pods :as pods]
            [clojure.pprint :as pp]))

;; Load the Datalevin pod
(pods/load-pod 'huahaiy/datalevin "0.9.22")
(require '[pod.huahaiy.datalevin :as d])

(defn example-datalevin-transactions
  "Comprehensive example of Datalevin transactions"
  [db-path]
  (println "🔗 Connecting to Datalevin database at:" db-path)
  
  ;; Get a connection to the database
  (let [conn (d/get-conn db-path)]
    (try
      (println "\n📋 Step 1: Define schema")
      ;; Define schema with attributes
      (let [schema {:user/id       {:db/valueType :db.type/long
                                    :db/unique    :db.unique/identity}
                    :user/name     {:db/valueType :db.type/string}
                    :user/email    {:db/valueType :db.type/string
                                    :db/unique    :db.unique/value}
                    :user/age      {:db/valueType :db.type/long}
                    :user/friends  {:db/valueType   :db.type/ref
                                    :db/cardinality :db.cardinality/many}
                    :post/id       {:db/valueType :db.type/long
                                    :db/unique    :db.unique/identity}
                    :post/title    {:db/valueType :db.type/string}
                    :post/content  {:db/valueType :db.type/string}
                    :post/author   {:db/valueType :db.type/ref}
                    :post/tags     {:db/valueType   :db.type/string
                                    :db/cardinality :db.cardinality/many}}]
        
        ;; Transact the schema
        (d/transact! conn schema)
        (println "✅ Schema transacted successfully"))

      (println "\n📋 Step 2: Insert users")
      ;; Insert some users
      (let [users-tx [{:user/id 1
                       :user/name "Alice Johnson"
                       :user/email "alice@example.com"
                       :user/age 28}
                      {:user/id 2
                       :user/name "Bob Smith"
                       :user/email "bob@example.com"
                       :user/age 32}
                      {:user/id 3
                       :user/name "Carol Brown"
                       :user/email "carol@example.com"
                       :user/age 25}]]
        (d/transact! conn users-tx)
        (println "✅ Users transacted successfully"))

      (println "\n📋 Step 3: Insert posts with relationships")
      ;; Insert posts that reference users
      (let [posts-tx [{:post/id 101
                       :post/title "Getting Started with Datalevin"
                       :post/content "Datalevin is a great database for Clojure applications..."
                       :post/author [:user/id 1]  ; Reference to Alice
                       :post/tags ["datalevin" "clojure" "tutorial"]}
                      {:post/id 102
                       :post/title "Advanced Datalog Queries"
                       :post/content "Learn how to write complex queries..."
                       :post/author [:user/id 2]  ; Reference to Bob
                       :post/tags ["datalog" "queries" "advanced"]}
                      {:post/id 103
                       :post/title "Database Design Patterns"
                       :post/content "Best practices for designing your schema..."
                       :post/author [:user/id 3]  ; Reference to Carol
                       :post/tags ["design" "patterns" "database"]}]]
        (d/transact! conn posts-tx)
        (println "✅ Posts transacted successfully"))

      (println "\n📋 Step 4: Add friendships (many-to-many relationships)")
      ;; Add friendship relationships
      (let [friendship-tx [{:user/id 1 :user/friends [[:user/id 2] [:user/id 3]]}
                           {:user/id 2 :user/friends [[:user/id 1]]}
                           {:user/id 3 :user/friends [[:user/id 1]]}]]
        (d/transact! conn friendship-tx)
        (println "✅ Friendships transacted successfully"))

      (println "\n📋 Step 5: Query the data")
      ;; Query 1: Get all users with their posts
      (println "\n🔍 Query 1: Users with their posts")
      (let [users-with-posts (d/q '[:find ?user-name ?post-title
                                    :where
                                    [?user :user/name ?user-name]
                                    [?post :post/author ?user]
                                    [?post :post/title ?post-title]]
                                  (d/db conn))]
        (pp/pprint users-with-posts))

      ;; Query 2: Get users and their friends
      (println "\n🔍 Query 2: Users and their friends")
      (let [friendships (d/q '[:find ?user-name ?friend-name
                               :where
                               [?user :user/name ?user-name]
                               [?user :user/friends ?friend]
                               [?friend :user/name ?friend-name]]
                             (d/db conn))]
        (pp/pprint friendships))

      ;; Query 3: Posts with specific tags
      (println "\n🔍 Query 3: Posts tagged with 'clojure'")
      (let [clojure-posts (d/q '[:find ?title ?author-name
                                 :where
                                 [?post :post/tags "clojure"]
                                 [?post :post/title ?title]
                                 [?post :post/author ?author]
                                 [?author :user/name ?author-name]]
                               (d/db conn))]
        (pp/pprint clojure-posts))

      (println "\n📋 Step 6: Update existing data")
      ;; Update Alice's age
      (d/transact! conn [{:user/id 1 :user/age 29}])
      (println "✅ Updated Alice's age")

      ;; Add a new tag to an existing post
      (d/transact! conn [{:post/id 101 :post/tags "beginner"}])
      (println "✅ Added tag to existing post")

      (println "\n📋 Step 7: Retract data")
      ;; Remove a specific tag from a post
      (d/transact! conn [[:db/retract [:post/id 101] :post/tags "beginner"]])
      (println "✅ Removed tag from post")

      (println "\n📊 Step 8: Database statistics")
      (let [stats (d/stat conn)]
        (println "Database statistics:")
        (pp/pprint stats))

      (finally
        ;; Always close the connection
        (d/close conn)
        (println "\n🔒 Connection closed")))))

(defn simple-datalevin-example
  "Simple example for quick testing"
  [db-path]
  (println "🚀 Simple Datalevin example")
  (let [conn (d/get-conn db-path)]
    (try
      ;; Simple schema
      (d/transact! conn {:person/name {:db/valueType :db.type/string}
                         :person/age  {:db/valueType :db.type/long}})
      
      ;; Insert data
      (d/transact! conn [{:person/name "John Doe"
                          :person/age 30}
                         {:person/name "Jane Smith"  
                          :person/age 25}])
      
      ;; Query data
      (let [people (d/q '[:find ?name ?age
                          :where
                          [?e :person/name ?name]
                          [?e :person/age ?age]]
                        (d/db conn))]
        (println "People in database:")
        (pp/pprint people))
      
      (finally
        (d/close conn)))))

(defn transaction-patterns
  "Common transaction patterns in Datalevin"
  [db-path]
  (println "📝 Common Datalevin transaction patterns")
  (let [conn (d/get-conn db-path)]
    (try
      ;; Schema
      (d/transact! conn {:item/id    {:db/valueType :db.type/long
                                      :db/unique    :db.unique/identity}
                         :item/name  {:db/valueType :db.type/string}
                         :item/price {:db/valueType :db.type/double}
                         :item/tags  {:db/valueType   :db.type/string
                                      :db/cardinality :db.cardinality/many}})

      (println "\n1. Batch insert:")
      (d/transact! conn [{:item/id 1 :item/name "Laptop" :item/price 999.99 :item/tags ["electronics" "computer"]}
                         {:item/id 2 :item/name "Book" :item/price 19.99 :item/tags ["education" "paperback"]}
                         {:item/id 3 :item/name "Coffee" :item/price 4.50 :item/tags ["beverage" "hot"]}])
      (println "✅ Batch insert completed")

      (println "\n2. Conditional transaction (upsert):")
      ;; This will update if exists, insert if not
      (d/transact! conn [{:item/id 1 :item/price 899.99}]) ; Update existing
      (d/transact! conn [{:item/id 4 :item/name "Tea" :item/price 3.50}]) ; Insert new
      (println "✅ Upsert completed")

      (println "\n3. Add to multi-valued attribute:")
      (d/transact! conn [{:item/id 1 :item/tags "gaming"}]) ; Add tag to laptop
      (println "✅ Added tag")

      (println "\n4. Retract specific values:")
      (d/transact! conn [[:db/retract [:item/id 1] :item/tags "computer"]])
      (println "✅ Retracted specific tag")

      (println "\n5. Retract entire entity:")
      (d/transact! conn [[:db/retractEntity [:item/id 4]]])
      (println "✅ Retracted entire entity")

      ;; Show final state
      (println "\nFinal items:")
      (let [items (d/q '[:find ?name ?price ?tags
                         :where
                         [?e :item/name ?name]
                         [?e :item/price ?price]
                         [?e :item/tags ?tags]]
                       (d/db conn))]
        (pp/pprint items))

      (finally
        (d/close conn)))))

(comment
  ;; Example usage:
  
  ;; Run comprehensive example
  (example-datalevin-transactions "/tmp/example-db")
  
  ;; Run simple example  
  (simple-datalevin-example "/tmp/simple-db")
  
  ;; Run transaction patterns
  (transaction-patterns "/tmp/patterns-db"))