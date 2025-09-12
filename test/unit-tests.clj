#!/usr/bin/env bb

(ns unit-tests
  "Simple unit test runner for BBPad"
  (:require [clojure.test :as test]
            [bbpad.core.config :as config]
            [bbpad.core.webview :as webview]))

(defn test-config []
  (test/testing "Configuration system"
    (config/init! {:dev true :port 8080})
    (test/is (= true (config/get-config [:server :dev-mode])))
    (test/is (= 8080 (config/get-config [:server :port])))
    (test/is (string? (config/get-version)))))

(defn test-webview []
  (test/testing "WebView platform detection"
    (let [platform (webview/detect-platform)]
      (test/is (contains? #{:windows :macos :linux :unknown} platform)))))

(defn run-tests []
  (println "🧪 Running BBPad unit tests...")
  
  (let [results (test/run-tests 'unit-tests)]
    (println (str "\n📊 Results: " 
                  (:pass results) " passed, "
                  (:fail results) " failed, "
                  (:error results) " errors"))
    
    (if (and (zero? (:fail results)) (zero? (:error results)))
      (do (println "✅ All tests passed!") 0)
      (do (println "❌ Some tests failed") 1))))

;; Run the tests directly
(test-config)
(test-webview)

(when (= *file* (System/getProperty "babashka.file"))
  (System/exit (run-tests)))