(ns bbpad.core.version
  "Version management for BBPad across all package files"
  (:require [clojure.string :as str]
            [clojure.data.json :as json]
            [babashka.fs :as fs]
            [babashka.process :refer [shell]]))

(def ^:private version-files
  "Files that contain version information that need to be kept in sync"
  [{:path "package.json" :type :json :key "version"}
   {:path "bbpad-ui/package.json" :type :json :key "version"}
   {:path "bb.edn" :type :edn :key :version}])

(defn current-version
  "Get the current version from package.json"
  []
  (try
    (let [package-json (-> "package.json"
                          slurp
                          (json/read-str :key-fn keyword))]
      (:version package-json))
    (catch Exception e
      (println "Error reading version:" (.getMessage e))
      "0.1.0")))

(defn semantic-version-map
  "Parse semantic version string into map"
  [version-str]
  (let [[major minor patch] (str/split version-str #"\.")]
    {:major (Integer/parseInt major)
     :minor (Integer/parseInt minor)
     :patch (Integer/parseInt patch)
     :full version-str}))

(defn increment-version
  "Increment version based on type (:major, :minor, :patch)"
  [current-version version-type]
  (let [{:keys [major minor patch]} (semantic-version-map current-version)]
    (case version-type
      :major (format "%d.0.0" (inc major))
      :minor (format "%d.%d.0" major (inc minor))
      :patch (format "%d.%d.%d" major minor (inc patch))
      current-version)))

(defn read-file-version
  "Read version from a specific file"
  [{:keys [path type key]}]
  (try
    (when (fs/exists? path)
      (case type
        :json (let [content (-> path slurp (json/read-str :key-fn keyword))]
                (get content key))
        :edn (let [content (-> path slurp read-string)]
               (get content key))))
    (catch Exception e
      (println (format "Error reading version from %s: %s" path (.getMessage e)))
      nil)))

(defn write-file-version
  "Write version to a specific file"
  [{:keys [path type key]} new-version]
  (try
    (case type
      :json (let [content (-> path slurp (json/read-str :key-fn keyword))
                  updated (assoc content key new-version)]
              (spit path (json/write-str updated :indent true)))
      :edn (let [content (-> path slurp read-string)
                 updated (assoc content key new-version)]
             (spit path (pr-str updated))))
    {:success true :file path}
    (catch Exception e
      {:success false :file path :error (.getMessage e)})))

(defn sync-versions!
  "Synchronize version across all package files"
  [new-version]
  (let [results (map #(write-file-version % new-version) version-files)
        successes (filter :success results)
        failures (filter #(not (:success %)) results)]
    {:success (empty? failures)
     :updated-files (map :file successes)
     :failed-files (map #(select-keys % [:file :error]) failures)
     :new-version new-version}))

(defn check-version-consistency
  "Check if all version files have the same version"
  []
  (let [versions (map (fn [file-config]
                       {:file (:path file-config)
                        :version (read-file-version file-config)})
                     version-files)
        version-values (map :version versions)
        unique-versions (distinct (remove nil? version-values))]
    {:consistent? (= 1 (count unique-versions))
     :current-version (first unique-versions)
     :versions versions
     :unique-versions unique-versions}))

(defn bump-version!
  "Bump version across all files"
  [version-type]
  (let [current (current-version)
        new-version (increment-version current version-type)
        sync-result (sync-versions! new-version)]
    (assoc sync-result
           :previous-version current
           :version-type version-type)))

(defn get-version-info
  "Get comprehensive version information"
  []
  (let [consistency-check (check-version-consistency)
        current (current-version)
        git-hash (try
                   (str/trim (:out (shell {:out :string} "git" "rev-parse" "--short" "HEAD")))
                   (catch Exception _ "unknown"))
        build-date (str (java.time.Instant/now))]
    {:version current
     :git-hash git-hash
     :build-date build-date
     :consistency consistency-check
     :version-files version-files}))

(defn format-version-info
  "Format version info for display"
  [version-info]
  (let [{:keys [version git-hash build-date consistency]} version-info
        {:keys [consistent? versions]} consistency]
    (str "BBPad v" version 
         (when git-hash (str " (" git-hash ")"))
         "\nBuild: " build-date
         (when-not consistent?
           (str "\n⚠️  Version inconsistency detected:\n"
                (str/join "\n" (map #(format "  %s: %s" (:file %) (:version %)) versions)))))))

;; Development utilities
(defn dev-version-report
  "Generate detailed version report for development"
  []
  (let [info (get-version-info)]
    (println "=== BBPad Version Report ===")
    (println (format-version-info info))
    (println "\n=== File Details ===")
    (doseq [{:keys [file version]} (:versions (:consistency info))]
      (println (format "%-25s %s" file (or version "NOT FOUND"))))
    info))