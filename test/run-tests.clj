#!/usr/bin/env bb

(ns run-tests
  "Test runner for BBPad - runs all tests and reports results"
  (:require [clojure.test :as test]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn find-test-namespaces
  "Find all test namespaces in the test directory"
  []
  (let [test-dir (io/file "test")]
    (->> (file-seq test-dir)
         (filter #(.endsWith (.getName %) "_test.clj"))
         (map #(.getPath %))
         (map #(str/replace % #"test/" ""))
         (map #(str/replace % #"\.clj$" ""))
         (map #(str/replace % #"/" "."))
         (map symbol))))

(defn load-test-namespace
  "Load a test namespace and handle errors"
  [ns-sym]
  (try
    (require ns-sym)
    (println (str "✅ Loaded " ns-sym))
    true
    (catch Exception e
      (println (str "❌ Failed to load " ns-sym ": " (.getMessage e)))
      false)))

(defn run-tests-for-namespace
  "Run tests for a specific namespace"
  [ns-sym]
  (try
    (let [results (test/run-tests ns-sym)]
      (println (str "🧪 " ns-sym ": " 
                   (:pass results) " passed, " 
                   (:fail results) " failed, "
                   (:error results) " errors"))
      results)
    (catch Exception e
      (println (str "❌ Error running tests for " ns-sym ": " (.getMessage e)))
      {:pass 0 :fail 1 :error 1})))

(defn print-summary
  "Print test summary"
  [{:keys [pass fail error] :as results}]
  (let [total (+ pass fail error)]
    (println "\n" (str "=" 50))
    (println "📊 TEST SUMMARY")
    (println (str "=" 50))
    (println (str "Total tests: " total))
    (println (str "✅ Passed: " pass))
    (println (str "❌ Failed: " fail)) 
    (println (str "💥 Errors: " error))
    (println (str "Success rate: " (if (zero? total) 
                                      "N/A" 
                                      (format "%.1f%%" (* 100.0 (/ pass total))))))
    
    (if (and (zero? fail) (zero? error))
      (do
        (println "\n🎉 All tests passed!")
        0)
      (do
        (println "\n💔 Some tests failed")
        1))))

(defn -main [& args]
  (println "🚀 Starting BBPad test suite...")
  (println (str "Working directory: " (System/getProperty "user.dir")))
  (println)
  
  ;; Source paths are already configured in bb.edn
  (println "📁 Using source paths from bb.edn...")
  
  (let [test-namespaces (find-test-namespaces)]
    (println "Debug: Found files:" test-namespaces)
    (println (str "🔍 Found " (count test-namespaces) " test namespaces"))
    
    ;; Load all test namespaces
    (println "\n📚 Loading test namespaces...")
    (let [loaded-ns (->> test-namespaces
                         (filter load-test-namespace)
                         count)]
      
      (if (zero? loaded-ns)
        (do
          (println "❌ No test namespaces loaded successfully")
          (System/exit 1))
        (println (str "✅ Loaded " loaded-ns " test namespaces"))))
    
    ;; Run tests
    (println "\n🧪 Running tests...")
    (let [results (reduce (fn [acc ns-sym]
                            (let [ns-results (run-tests-for-namespace ns-sym)]
                              (-> acc
                                  (update :pass + (:pass ns-results 0))
                                  (update :fail + (:fail ns-results 0))
                                  (update :error + (:error ns-results 0)))))
                          {:pass 0 :fail 0 :error 0}
                          test-namespaces)]
      
      (System/exit (print-summary results)))))

;; Allow direct execution
(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))