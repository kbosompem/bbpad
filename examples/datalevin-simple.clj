;; Simple Datalevin Example for BBPad
;;
;; This example shows how to use Datalevin in BBPad scripts
;; without running into serialization issues

(require '[babashka.pods :as pods])
(pods/load-pod 'huahaiy/datalevin "0.9.22")
(require '[pod.huahaiy.datalevin :as d])

;; Create a simple in-memory database
(def conn (d/get-conn "/tmp/bbpad-datalevin-simple"))

;; Define a simple schema
(d/transact! conn
  [{:db/ident :person/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :person/age
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}])

;; Add some data
(d/transact! conn
  [{:person/name "Alice" :person/age 30}
   {:person/name "Bob" :person/age 25}
   {:person/name "Carol" :person/age 35}])

;; Query the data - convert results to vectors for proper display
(let [db (d/db conn)]
  (println "People in database:")
  (let [results (d/q '[:find ?name ?age
                       :where
                       [?e :person/name ?name]
                       [?e :person/age ?age]]
                     db)]
    ;; Convert set to vector for better display
    (doseq [[name age] (vec results)]
      (println (format "  - %s, age %d" name age))))

  (println "\nPeople over 28:")
  (let [results (d/q '[:find ?name ?age
                       :where
                       [?e :person/name ?name]
                       [?e :person/age ?age]
                       [(> ?age 28)]]
                     db)]
    (doseq [[name age] (vec results)]
      (println (format "  - %s, age %d" name age))))

  (println "\nAverage age:")
  (let [avg-age (d/q '[:find (avg ?age) .
                       :where
                       [?e :person/age ?age]]
                     db)]
    (println (format "  %.1f years" (double avg-age)))))

;; Close connection
(d/close conn)

;; Return a simple map (avoids serialization issues)
{:status "Success"
 :message "Datalevin demo completed"
 :location "/tmp/bbpad-datalevin-simple"}