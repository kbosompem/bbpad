(ns bbpad.core.changelog
  "Release notes and changelog management for BBPad"
  (:require [clojure.string :as str]
            [clojure.data.json :as json]
            [babashka.fs :as fs]
            [bbpad.core.version :as version]))

(def changelog-file "CHANGELOG.md")
(def release-notes-dir "release-notes")

(defn parse-version-string
  "Parse semantic version string"
  [version-str]
  (when version-str
    (let [[major minor patch] (str/split version-str #"\.")]
      {:major (Integer/parseInt major)
       :minor (Integer/parseInt minor)
       :patch (Integer/parseInt patch)
       :full version-str})))

(defn version-compare
  "Compare two version maps (-1 = v1 < v2, 0 = equal, 1 = v1 > v2)"
  [v1 v2]
  (let [major-cmp (compare (:major v1) (:major v2))]
    (if (= 0 major-cmp)
      (let [minor-cmp (compare (:minor v1) (:minor v2))]
        (if (= 0 minor-cmp)
          (compare (:patch v1) (:patch v2))
          minor-cmp))
      major-cmp)))

(defn read-changelog
  "Read and parse CHANGELOG.md file"
  []
  (try
    (when (fs/exists? changelog-file)
      (let [content (slurp changelog-file)
            lines (str/split-lines content)
            releases (atom [])
            current-release (atom nil)
            current-section (atom nil)
            current-items (atom [])]
        
        (doseq [line lines]
          (cond
            ;; Version header (## [Version] - Date)
            (re-matches #"^## \[([^\]]+)\] - (.+)$" line)
            (let [[_ version date] (re-matches #"^## \[([^\]]+)\] - (.+)$" line)]
              (when @current-release
                (swap! releases conj (assoc @current-release @current-section @current-items)))
              (reset! current-release {:version version 
                                      :date date
                                      :parsed-version (parse-version-string version)})
              (reset! current-section nil)
              (reset! current-items []))
            
            ;; Section header (### Added, ### Changed, etc.)
            (re-matches #"^### (.+)$" line)
            (let [[_ section] (re-matches #"^### (.+)$" line)]
              (when (and @current-release @current-section)
                (reset! current-release (assoc @current-release @current-section @current-items)))
              (reset! current-section (str/lower-case section))
              (reset! current-items []))
            
            ;; List item
            (re-matches #"^- (.+)$" line)
            (let [[_ item] (re-matches #"^- (.+)$" line)]
              (when @current-section
                (swap! current-items conj item)))))
        
        ;; Don't forget the last release
        (when @current-release
          (when @current-section
            (reset! current-release (assoc @current-release @current-section @current-items)))
          (swap! releases conj @current-release))
        
        ;; Sort releases by version (newest first)
        (->> @releases
             (sort-by :parsed-version #(version-compare %2 %1))
             vec)))
    (catch Exception e
      (println "Error reading changelog:" (.getMessage e))
      [])))

(defn format-changelog-entry
  "Format a single changelog entry"
  [{:keys [version date] :as release}]
  (let [sections ["added" "changed" "fixed" "deprecated" "removed" "security"]
        section-titles {"added" "Added"
                       "changed" "Changed"
                       "fixed" "Fixed"
                       "deprecated" "Deprecated"
                       "removed" "Removed"
                       "security" "Security"}]
    (str "## [" version "] - " date "\n"
         (->> sections
              (filter #(seq (get release %)))
              (map (fn [section]
                     (str "\n### " (section-titles section) "\n"
                          (->> (get release section)
                               (map #(str "- " %))
                               (str/join "\n")))))
              (str/join "\n"))
         "\n")))

(defn write-changelog
  "Write releases to CHANGELOG.md"
  [releases]
  (try
    (let [header (str "# Changelog\n\n"
                     "All notable changes to BBPad will be documented in this file.\n\n"
                     "The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),\n"
                     "and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).\n\n")
          content (->> releases
                       (sort-by :parsed-version #(version-compare %2 %1))
                       (map format-changelog-entry)
                       (str/join "\n"))]
      (spit changelog-file (str header content))
      {:success true :file changelog-file})
    (catch Exception e
      {:success false :error (.getMessage e)})))

(defn add-changelog-entry
  "Add a new entry to the changelog"
  [{:keys [version date added changed fixed deprecated removed security]}]
  (try
    (let [releases (read-changelog)
          new-release {:version version
                      :date (or date (str (java.time.LocalDate/now)))
                      :parsed-version (parse-version-string version)
                      :added (or added [])
                      :changed (or changed [])
                      :fixed (or fixed [])
                      :deprecated (or deprecated [])
                      :removed (or removed [])
                      :security (or security [])}
          ;; Remove existing entry for same version if it exists
          filtered-releases (remove #(= (:version %) version) releases)
          updated-releases (conj filtered-releases new-release)]
      
      (write-changelog updated-releases))
    (catch Exception e
      {:success false :error (.getMessage e)})))

(defn generate-release-notes
  "Generate release notes for a specific version"
  [version]
  (try
    (let [releases (read-changelog)
          release (first (filter #(= (:version %) version) releases))]
      (if release
        (let [notes (str "# BBPad " version " Release Notes\n\n"
                        "Released on " (:date release) "\n\n"
                        (format-changelog-entry release)
                        "\n## Download\n\n"
                        "- [macOS DMG](https://github.com/kbosompem/bbpad/releases/download/v" version "/BBPad-" version "-macOS.dmg)\n"
                        "- [Windows Installer](https://github.com/kbosompem/bbpad/releases/download/v" version "/BBPad-" version "-Windows.exe)\n"
                        "- [Linux AppImage](https://github.com/kbosompem/bbpad/releases/download/v" version "/BBPad-" version "-Linux.AppImage)\n\n"
                        "## Installation\n\n"
                        "### macOS\n"
                        "1. Download the DMG file\n"
                        "2. Open the DMG and drag BBPad to Applications\n"
                        "3. On first launch, you may need to right-click and select \"Open\" due to Gatekeeper\n\n"
                        "### Windows\n"
                        "1. Download the installer EXE\n"
                        "2. Run the installer with administrator privileges\n"
                        "3. Follow the installation wizard\n\n"
                        "### Linux\n"
                        "1. Download the AppImage\n"
                        "2. Make it executable: `chmod +x BBPad-" version "-Linux.AppImage`\n"
                        "3. Run it directly or integrate with your desktop environment\n\n"
                        "## System Requirements\n\n"
                        "- **macOS**: 10.15 (Catalina) or later\n"
                        "- **Windows**: Windows 10 or later\n"
                        "- **Linux**: Any modern distribution with GLIBC 2.17+ (Ubuntu 18.04+, etc.)\n\n"
                        "All platforms require internet access for script execution and package management.\n")]
          
          ;; Ensure release notes directory exists
          (fs/create-dirs release-notes-dir)
          
          ;; Write release notes file
          (let [notes-file (str release-notes-dir "/v" version ".md")]
            (spit notes-file notes)
            {:success true 
             :notes notes
             :file notes-file}))
        {:success false :error (str "Version " version " not found in changelog")}))
    (catch Exception e
      {:success false :error (.getMessage e)})))

(defn get-latest-release
  "Get the latest release from changelog"
  []
  (let [releases (read-changelog)]
    (first releases)))

(defn get-release-summary
  "Get a brief summary of recent releases"
  [& {:keys [count] :or {count 5}}]
  (let [releases (read-changelog)]
    (->> releases
         (take count)
         (map (fn [{:keys [version date added changed fixed]}]
                {:version version
                 :date date
                 :highlights (concat
                            (map #(str "Added: " %) (take 2 added))
                            (map #(str "Changed: " %) (take 2 changed))
                            (map #(str "Fixed: " %) (take 2 fixed)))}))
         vec)))

(defn create-version-changelog
  "Create changelog entry for current version with git commit analysis"
  [version]
  (try
    (let [;; Get git commits since last tag
          last-tag-result (try
                          (:out (babashka.process/shell {:out :string} "git" "describe" "--tags" "--abbrev=0" "HEAD~1"))
                          (catch Exception _ nil))
          commits (if last-tag-result
                   (let [last-tag (str/trim last-tag-result)]
                     (:out (babashka.process/shell {:out :string} "git" "log" "--oneline" (str last-tag "..HEAD"))))
                   (:out (babashka.process/shell {:out :string} "git" "log" "--oneline" "-10")))
          
          commit-lines (-> commits str/trim str/split-lines)
          
          ;; Categorize commits based on keywords
          categorized (reduce
                      (fn [acc line]
                        (cond
                          (re-find #"(?i)(add|new|implement|create)" line)
                          (update acc :added conj (str/replace line #"^[a-f0-9]+ " ""))
                          
                          (re-find #"(?i)(fix|bug|issue|error)" line)
                          (update acc :fixed conj (str/replace line #"^[a-f0-9]+ " ""))
                          
                          (re-find #"(?i)(update|change|modify|refactor|improve)" line)
                          (update acc :changed conj (str/replace line #"^[a-f0-9]+ " ""))
                          
                          :else
                          (update acc :changed conj (str/replace line #"^[a-f0-9]+ " ""))))
                      {:added [] :changed [] :fixed [] :deprecated [] :removed [] :security []}
                      commit-lines)]
      
      (add-changelog-entry
       (assoc categorized
              :version version
              :date (str (java.time.LocalDate/now)))))
    
    (catch Exception e
      {:success false :error (.getMessage e)})))

;; Development utilities
(defn init-changelog!
  "Initialize changelog with current version"
  []
  (let [current-version (version/current-version)]
    (add-changelog-entry
     {:version current-version
      :date (str (java.time.LocalDate/now))
      :added ["Initial release of BBPad"
              "React + TypeScript frontend with Monaco Editor"
              "Babashka backend with HTTP server"
              "Script execution and result display"
              "Database integration with SQLite"
              "Tab-based interface with session persistence"
              "Script saving and loading functionality"]
      :changed []
      :fixed []})))

(defn release-workflow!
  "Complete release workflow: version bump, changelog, and release notes"
  [version-type]
  (try
    (println "🚀 Starting release workflow...")
    
    ;; Bump version
    (println "📈 Bumping version...")
    (let [bump-result (version/bump-version! version-type)
          new-version (:new-version bump-result)]
      
      (if (:success bump-result)
        (do
          (println (str "✅ Version bumped to " new-version))
          
          ;; Create changelog entry
          (println "📝 Creating changelog entry...")
          (let [changelog-result (create-version-changelog new-version)]
            (if (:success changelog-result)
              (do
                (println "✅ Changelog updated")
                
                ;; Generate release notes
                (println "📋 Generating release notes...")
                (let [notes-result (generate-release-notes new-version)]
                  (if (:success notes-result)
                    (do
                      (println "✅ Release notes generated")
                      (println (str "🎉 Release workflow complete for version " new-version))
                      {:success true
                       :version new-version
                       :changelog-file changelog-file
                       :release-notes-file (:file notes-result)})
                    (do
                      (println "❌ Failed to generate release notes:" (:error notes-result))
                      notes-result))))
              (do
                (println "❌ Failed to update changelog:" (:error changelog-result))
                changelog-result))))
        (do
          (println "❌ Failed to bump version:" (:error bump-result))
          bump-result)))
    
    (catch Exception e
      (println "❌ Error in release workflow:" (.getMessage e))
      {:success false :error (.getMessage e)})))

;; CLI utilities for development
(defn -main [& args]
  (case (first args)
    "init" (init-changelog!)
    "add" (let [[_ version & items] args]
            (add-changelog-entry {:version version
                                 :added (vec items)}))
    "notes" (let [[_ version] args]
              (generate-release-notes (or version (version/current-version))))
    "release" (let [[_ version-type] args]
                (release-workflow! (keyword (or version-type "patch"))))
    "summary" (get-release-summary)
    (println "Usage: bb changelog.clj [init|add|notes|release|summary] [args...]")))