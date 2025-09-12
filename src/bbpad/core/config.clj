(ns bbpad.core.config
  "Configuration management for BBPad"
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [cheshire.core :as json]))

(def ^:dynamic *config* (atom {}))

(def default-config
  {:version "0.1.0-SNAPSHOT"
   :server {:port 0
            :host "localhost"
            :dev-mode false}
   :webview {:width 1200
             :height 800
             :title "BBPad"
             :dev-tools false}
   :security {:sandbox-mode true
              :allowed-paths #{}}
   :ui {:theme "dark"
        :font-size 14
        :enable-vim-mode false}
   :database {:connection-timeout 5000
              :max-connections 10}})

(defn get-version []
  (or (:version @*config*)
      (:version default-config)))

(defn get-config
  "Get configuration value by path (e.g. [:server :port])"
  ([path]
   (get-in @*config* path))
  ([path default]
   (get-in @*config* path default)))

(defn set-config!
  "Set configuration value by path"
  [path value]
  (swap! *config* assoc-in path value))

(defn merge-config!
  "Merge configuration map"
  [config-map]
  (swap! *config* merge config-map))

(defn load-config-file
  "Load configuration from JSON file"
  [file-path]
  (try
    (when (.exists (io/file file-path))
      (-> file-path
          slurp
          (json/parse-string true)))
    (catch Exception e
      (println (str "Warning: Could not load config file " file-path ": " (.getMessage e)))
      {})))

(defn save-config-file!
  "Save current configuration to JSON file"
  [file-path]
  (try
    (spit file-path (json/generate-string @*config* {:pretty true}))
    (catch Exception e
      (println (str "Warning: Could not save config file " file-path ": " (.getMessage e))))))

(defn get-config-dir
  "Get platform-specific configuration directory"
  []
  (let [os-name (str/lower-case (System/getProperty "os.name"))
        home (System/getProperty "user.home")]
    (cond
      (str/includes? os-name "windows") 
      (str (System/getenv "APPDATA") "\\BBPad")
      
      (str/includes? os-name "mac")
      (str home "/Library/Application Support/BBPad")
      
      :else ; Linux/Unix
      (str home "/.config/bbpad"))))

(defn ensure-config-dir!
  "Ensure configuration directory exists"
  []
  (let [config-dir (get-config-dir)]
    (.mkdirs (io/file config-dir))
    config-dir))

(defn init!
  "Initialize configuration system"
  [cli-options]
  (let [config-dir (ensure-config-dir!)
        config-file (str config-dir "/config.json")
        file-config (load-config-file config-file)]
    
    ;; Merge configurations: default < file < CLI options
    (reset! *config* default-config)
    (merge-config! file-config)
    
    ;; Apply CLI options
    (when (:port cli-options)
      (set-config! [:server :port] (:port cli-options)))
    
    (when (:dev cli-options)
      (set-config! [:server :dev-mode] true)
      (set-config! [:webview :dev-tools] true))
    
    ;; Log configuration
    (when (get-config [:server :dev-mode])
      (println (str "📁 Config directory: " config-dir))
      (println (str "⚙️  Configuration loaded")))))

(defn dev-mode? []
  (get-config [:server :dev-mode] false))

(defn get-server-config []
  (get-config [:server]))

(defn get-webview-config []
  (get-config [:webview]))

(defn get-security-config []
  (get-config [:security]))