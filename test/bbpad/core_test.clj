(ns bbpad.core-test
  "Core functionality tests for BBPad"
  (:require [clojure.test :refer [deftest testing is]]
            [bbpad.core.config :as config]
            [bbpad.core.webview :as webview]
            [bbpad.server.core :as server]
            [ring.mock.request :as mock]
            [clojure.string :as str]))

(deftest config-test
  (testing "Configuration initialization"
    (config/init! {:dev true :port 8080})
    
    (is (= true (config/get-config [:server :dev-mode])))
    (is (= 8080 (config/get-config [:server :port])))
    (is (= "BBPad" (config/get-config [:webview :title]))))
  
  (testing "Version information"
    (is (string? (config/get-version)))
    (is (not (str/blank? (config/get-version)))))
  
  (testing "Platform-specific config directory"
    (let [config-dir (config/get-config-dir)]
      (is (string? config-dir))
      (is (not (str/blank? config-dir))))))

(deftest webview-test
  (testing "Platform detection"
    (let [platform (webview/detect-platform)]
      (is (contains? #{:windows :macos :linux :unknown} platform))))
  
  (testing "WebView executable detection"
    ;; This may return nil if no WebView is bundled, which is fine
    (let [exe (webview/find-webview-executable)]
      (when exe
        (is (string? exe))
        (is (.exists (java.io.File. exe)))))))

(deftest server-test  
  (testing "Server startup and shutdown"
    (let [port (server/start-server! {:port 0 :dev-mode true})]
      (is (integer? port))
      (is (> port 0))
      
      ;; Test health endpoint
      (let [app (server/create-app)
            response (app (mock/request :get "/api/health"))]
        (is (= 200 (:status response)))
        (is (= "ok" (get-in response [:body :status]))))
      
      (server/stop-server!)))
  
  (testing "Static resource serving"
    (let [app (server/create-app)
          response (app (mock/request :get "/"))]
      (is (= 200 (:status response)))
      (is (str/includes? (:body response) "BBPad"))))
  
  (testing "API error handling"
    (let [app (server/create-app)
          response (app (mock/request :get "/api/nonexistent"))]
      (is (= 404 (:status response))))))

(deftest integration-test
  (testing "Full startup sequence"
    ;; This is a basic integration test
    ;; More comprehensive tests would be in integration test suite
    (config/init! {:dev true :port 0})
    (let [port (server/start-server! (config/get-server-config))]
      (is (integer? port))
      (is (> port 0))
      
      ;; Verify server is responding
      (let [health-url (str "http://localhost:" port "/api/health")]
        ;; In a real test, we'd make an HTTP request here
        ;; For now, just verify the server started
        (is (not (nil? @server/*server*))))
      
      (server/stop-server!)
      (is (nil? @server/*server*)))))