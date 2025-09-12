(ns bbpad.core.pod-manager
  "Pod management for BBPad - handles loading and managing Babashka pods"
  (:require [babashka.pods :as pods]
            [bbpad.core.config :as config]
            [clojure.string :as str]))

(def ^:dynamic *loaded-pods* (atom #{}))

(defn pod-loaded? 
  "Check if a pod is already loaded"
  [pod-name]
  (contains? @*loaded-pods* pod-name))

(defn load-pod-safe
  "Safely load a pod with error handling"
  [pod-spec]
  (try
    (let [pod-name (if (map? pod-spec) 
                     (:name pod-spec) 
                     (str pod-spec))]
      (when-not (pod-loaded? pod-name)
        (when (config/dev-mode?)
          (println (str "🔌 Loading pod: " pod-name)))
        
        (cond
          ;; Load from registry with version
          (and (symbol? pod-spec) (map? pod-spec))
          (pods/load-pod (:name pod-spec) (:version pod-spec))
          
          ;; Load from registry 
          (symbol? pod-spec)
          (pods/load-pod pod-spec)
          
          ;; Load from local path
          (string? pod-spec)
          (pods/load-pod pod-spec)
          
          :else
          (throw (ex-info "Invalid pod specification" {:pod-spec pod-spec})))
        
        (swap! *loaded-pods* conj pod-name)
        (when (config/dev-mode?)
          (println (str "✅ Pod loaded successfully: " pod-name)))
        true))
    
    (catch Exception e
      (println (str "❌ Failed to load pod " pod-spec ": " (.getMessage e)))
      (when (config/dev-mode?)
        (.printStackTrace e))
      false)))

(defn load-required-pods
  "Load all pods required by BBPad"
  []
  (println "🔌 Pod support available (will load on-demand)")
  (println "Available pods: PostgreSQL, HSQLDB, SSH, and more")
  ; Commenting out automatic loading to avoid network timeouts during development
  ; (let [required-pods ['org.babashka/postgresql
  ;                      'org.babashka/hsqldb
  ;                      'epiccastle/bbssh]]
  ;   (println "🔌 Loading required pods...")
  ;   (doseq [pod required-pods]
  ;     (load-pod-safe pod))
  ;   (println (str "✅ Loaded " (count @*loaded-pods*) " pods")))
  )

(defn list-available-pods
  "List all available pods from registry"
  []
  ;; This would require fetching from pod registry
  ;; For now, return known pods
  {"Database" ["org.babashka/postgresql" "org.babashka/hsqldb" "org.babashka/sqlite"]
   "System" ["epiccastle/bbssh" "babashka/fswatcher"]
   "Development" ["borkdude/clj-kondo" "com.github.clojure-lsp/clojure-lsp-server"]
   "Web" ["babashka/http-client" "tzzh/mail"]
   "Data" ["babashka/excel" "babashka/html"]})

(defn pod-info
  "Get information about a loaded pod"
  [pod-name]
  (when (pod-loaded? pod-name)
    {:name pod-name
     :status :loaded
     :namespaces (try
                   ;; This would need to be implemented based on pod introspection
                   []
                   (catch Exception e []))}))

(defn unload-pod
  "Unload a pod (if supported by the pod)"
  [pod-name]
  ;; Most pods don't support unloading
  ;; This is mainly for bookkeeping
  (swap! *loaded-pods* disj pod-name)
  (when (config/dev-mode?)
    (println (str "🔌 Unloaded pod: " pod-name))))

(defn refresh-pods
  "Refresh/reload all pods"
  []
  (let [current-pods @*loaded-pods*]
    (reset! *loaded-pods* #{})
    (doseq [pod current-pods]
      (load-pod-safe pod))))

(defn get-pod-status
  "Get status of all loaded pods"
  []
  {:loaded-pods @*loaded-pods*
   :total-count (count @*loaded-pods*)
   :available-categories (keys (list-available-pods))})