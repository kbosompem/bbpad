;; Datalevin with BBPad Saved Connections
;;
;; This example shows how to use Datalevin through BBPad's connection system.
;; First, save a Datalevin connection in BBPad:
;;   - Type: datalevin
;;   - Database: /tmp/my-datalevin-db (or any path)
;;   - Name: "My Datalevin" (or any name you prefer)

(require '[bbpad.connections :as conn])

;; List your saved connections
(println "Available connections:")
(doseq [conn-id (conn/list-connections)]
  (let [connection (conn/get-connection conn-id)]
    (println (format "  - %s (type: %s)" (:name connection) (:type connection)))))

(println "\n---\n")

;; Assuming you have saved a Datalevin connection named "My Datalevin"
;; Replace this with your actual connection name
(def datalevin-conn-name "My Datalevin")

;; For demo purposes, let's create a local Datalevin connection
;; In real use, you'd use your saved connection
(require '[babashka.pods :as pods])
(pods/load-pod 'huahaiy/datalevin "0.9.22")
(require '[pod.huahaiy.datalevin :as d])

(def demo-conn (d/get-conn "/tmp/bbpad-datalevin-demo"))

;; Define schema
(d/transact! demo-conn
  [{:db/ident :person/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :person/age
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident :person/email
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity}

   {:db/ident :person/skills
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/many}

   {:db/ident :project/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :project/language
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :project/owner
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}])

(println "Schema created!")

;; If you have a saved Datalevin connection, you would use it like this:
(comment
  ;; Using saved connection for transactions
  (conn/transact! "My Datalevin"
    [{:person/name "Alice Johnson"
      :person/age 28
      :person/email "alice@example.com"
      :person/skills ["Clojure" "Python" "SQL"]}

     {:person/name "Bob Smith"
      :person/age 35
      :person/email "bob@example.com"
      :person/skills ["Java" "Kubernetes"]}])

  ;; Query using saved connection
  (conn/datalevin-q "My Datalevin"
    '[:find ?name ?age
      :where
      [?e :person/name ?name]
      [?e :person/age ?age]])

  ;; Pull entity details
  (conn/datalevin-pull "My Datalevin"
    '[*]
    [:person/email "alice@example.com"])

  ;; More complex queries
  (conn/datalevin-q "My Datalevin"
    '[:find ?name ?skill
      :where
      [?e :person/name ?name]
      [?e :person/skills ?skill]
      [?e :person/age ?age]
      [(> ?age 30)]])

  ;; Using with-datalevin for direct access
  (conn/with-datalevin "My Datalevin"
    (fn [conn db]
      ;; You have direct access to conn and db here
      ;; Can use any Datalevin functions
      (d/q '[:find (count ?e)
             :where [?e :person/name]]
           db))))

;; Demo with direct connection (simulating saved connection behavior)
(println "\nDemo with local Datalevin:")

;; Add some data
(d/transact! demo-conn
  [{:db/id -1
    :person/name "Alice Johnson"
    :person/age 28
    :person/email "alice@example.com"
    :person/skills ["Clojure" "Python" "SQL"]}

   {:db/id -2
    :person/name "Bob Smith"
    :person/age 35
    :person/email "bob@example.com"
    :person/skills ["Java" "Kubernetes" "AWS"]}

   {:db/id -3
    :person/name "Carol White"
    :person/age 42
    :person/email "carol@example.com"
    :person/skills ["Clojure" "Datomic"]}])

;; Add projects with owners
(let [db (d/db demo-conn)
      alice-id (d/q '[:find ?e .
                      :where [?e :person/email "alice@example.com"]]
                    db)
      bob-id (d/q '[:find ?e .
                    :where [?e :person/email "bob@example.com"]]
                  db)]
  (d/transact! demo-conn
    [{:project/name "BBPad"
      :project/language "Clojure"
      :project/owner alice-id}

     {:project/name "CloudAPI"
      :project/language "Java"
      :project/owner bob-id}]))

(println "Sample data added!")

;; Query examples
(let [db (d/db demo-conn)]
  (println "\nAll people:")
  (let [people (d/q '[:find ?name ?age
                      :where
                      [?e :person/name ?name]
                      [?e :person/age ?age]]
                    db)]
    (doseq [[name age] people]
      (println (format "  - %s (age %d)" name age))))

  (println "\nProjects and owners:")
  (let [projects (d/q '[:find ?proj-name ?owner-name
                        :where
                        [?p :project/name ?proj-name]
                        [?p :project/owner ?o]
                        [?o :person/name ?owner-name]]
                      db)]
    (doseq [[proj owner] projects]
      (println (format "  - %s owned by %s" proj owner))))

  (println "\nClojure developers:")
  (let [clojure-devs (d/q '[:find ?name
                            :where
                            [?e :person/name ?name]
                            [?e :person/skills "Clojure"]]
                          db)]
    (doseq [[name] clojure-devs]
      (println "  -" name))))

;; Close demo connection
(d/close demo-conn)

{:status "Success!"
 :tip "Save a Datalevin connection in BBPad UI to use conn/transact! and conn/datalevin-q"
 :demo-db "/tmp/bbpad-datalevin-demo"
 :features ["Schema definition"
            "Transactions"
            "Datalog queries"
            "Entity references"
            "Pull API"]}