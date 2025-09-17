;; Test script for cljdump visualization
;; This script returns various data structures to test the cljdump tab

;; Test 1: Simple map
{:name "John Doe"
 :age 30
 :email "john@example.com"
 :active true}

;; Test 2: Nested data structure (uncomment to test)
#_
{:user {:id 1
        :name "Alice"
        :roles [:admin :user]
        :settings {:theme "dark"
                   :notifications true}}
 :posts [{:id 1 :title "First Post" :likes 42}
         {:id 2 :title "Second Post" :likes 17}]
 :metadata {:created #inst "2024-01-01"
            :version 2.1
            :tags #{"clojure" "babashka" "bbpad"}}}

;; Test 3: Vector of maps (uncomment to test)
#_
[{:id 1 :name "Item 1" :price 19.99}
 {:id 2 :name "Item 2" :price 29.99}
 {:id 3 :name "Item 3" :price 39.99}]

;; Test 4: Complex nested structure (uncomment to test)
#_
{:company "Acme Corp"
 :departments
 [{:name "Engineering"
   :employees [{:name "Bob" :role "Developer" :skills ["Clojure" "Java"]}
               {:name "Carol" :role "Designer" :skills ["UI" "UX"]}]}
  {:name "Sales"
   :employees [{:name "Dave" :role "Manager" :targets {:q1 100000 :q2 150000}}
               {:name "Eve" :role "Rep" :targets {:q1 50000 :q2 75000}}]}]
 :financials {:revenue 1000000
              :expenses 750000
              :profit 250000}}

;; Test 5: Set and list (uncomment to test)
#_
{:unique-values #{1 2 3 4 5}
 :ordered-list '(first second third fourth)
 :mixed-types [1 "two" :three 'four nil true]}

;; Instructions:
;; 1. Copy this script into BBPad
;; 2. Execute to see the simple map result
;; 3. Uncomment different test cases (remove #_) to test various data structures
;; 4. Switch to the "cljdump" tab to see the rich visualization