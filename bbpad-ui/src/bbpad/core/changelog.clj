(ns bbpad.core.changelog
  "Release notes and changelog management for BBPad"
  (:require [clojure.string :as str]
            [babashka.fs :as fs]
            [babashka.process :refer [shell]]
            [bbpad.core.version :as version]))

(def changelog-file "CHANGELOG.md")

(defn init-changelog!
  "Initialize CHANGELOG.md with current version"
  []
  (let [current-version (version/current-version)
        content (str "# Changelog\n\n"
                    "All notable changes to BBPad will be documented in this file.\n\n"
                    "The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),\n"
                    "and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).\n\n"
                    "## [" current-version "] - " (str (java.time.LocalDate/now)) "\n\n"
                    "### Added\n"
                    "- Initial release of BBPad\n"
                    "- React + TypeScript frontend with Monaco Editor\n"
                    "- Babashka backend with HTTP server\n"
                    "- Script execution and result display\n"
                    "- Database integration with SQLite\n"
                    "- Tab-based interface with session persistence\n"
                    "- Script saving and loading functionality\n"
                    "- Collections and favorites system\n"
                    "- Script execution history tracking\n"
                    "- Multi-platform packaging (macOS, Windows, Linux)\n\n")]
    (spit changelog-file content)
    (println "✅ CHANGELOG.md initialized with version" current-version)
    {:success true :file changelog-file}))

(defn -main [& args]
  (case (first args)
    "init" (init-changelog!)
    (println "Usage: bb changelog.clj init")))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
