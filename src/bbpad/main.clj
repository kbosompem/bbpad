#!/usr/bin/env bb

(ns bbpad.main
  "BBPad main entry point - starts the Ring server and launches WebView"
  (:require [bbpad.server.core :as server]
            [bbpad.core.webview :as webview]
            [bbpad.core.config :as config]
            [bbpad.core.pod-manager :as pod-manager]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as str]))

(def cli-options
  [["-p" "--port PORT" "Port number for Ring server"
    :default 0
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   
   ["-d" "--dev" "Development mode - enables hot reload and debug features"]
   
   ["-h" "--help" "Show help"]
   
   ["-v" "--version" "Show version"]])

(defn usage [options-summary]
  (->> ["BBPad - A LINQPad-inspired desktop app for Babashka scripts"
        ""
        "Usage: bbpad [options]"
        ""
        "Options:"
        options-summary
        ""
        "Examples:"
        "  bbpad                    # Start BBPad on random port"
        "  bbpad --port 8080        # Start on specific port"  
        "  bbpad --dev              # Start in development mode"
        ""
        "For more information, visit: https://github.com/kbosompem/bbpad"]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn version []
  (str "BBPad version " (config/get-version)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn start-bbpad! 
  "Start BBPad server and WebView"
  [{:keys [port dev] :as options}]
  (try
    (println "🚀 Starting BBPad...")
    (println (str "Version: " (config/get-version)))
    
    ;; Load required pods
    (pod-manager/load-required-pods)
    
    ;; Start Ring server
    (let [server-port (server/start-server! {:port port :dev-mode dev})
          webview-url (str "http://localhost:" server-port)]
      
      (println (str "📡 Server started on port " server-port))
      (when dev
        (println "🔧 Development mode enabled"))
      
      ;; Launch WebView
      (println "🌐 Launching WebView...")
      (webview/launch-webview! webview-url {:dev-mode dev})
      
      (println "✅ BBPad is ready!")
      
      ;; Keep main thread alive
      (loop []
        (Thread/sleep 1000)
        (recur)))
    
    (catch Exception e
      (println "❌ Error starting BBPad:")
      (println (.getMessage e))
      (when (:dev options)
        (.printStackTrace e))
      (System/exit 1))))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) (exit 0 (usage summary))
      (:version options) (exit 0 (version))
      errors (exit 1 (error-msg errors)))
    
    ;; Initialize configuration
    (config/init! options)
    
    ;; Start BBPad
    (start-bbpad! options)))

;; Allow direct execution with bb
(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))