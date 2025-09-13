#!/usr/bin/env bb

(ns build.package-macos
  (:require [babashka.fs :as fs]
            [babashka.process :refer [shell]]))

(def app-name "BBPad")
(def build-dir "build")
(def app-bundle (str build-dir "/" app-name ".app"))
(def dmg-name (str app-name "-0.1.0-macOS.dmg"))

(defn build-frontend! []
  (println "🔨 Building React frontend...")
  (shell "cd bbpad-ui && npm run build"))

(defn create-app-bundle! []
  (println "📦 Creating macOS app bundle...")
  
  ;; Create bundle structure
  (fs/create-dirs (str app-bundle "/Contents/MacOS"))
  (fs/create-dirs (str app-bundle "/Contents/Resources"))
  
  ;; Copy Info.plist
  (fs/copy "build/macos/Info.plist" (str app-bundle "/Contents/Info.plist"))
  
  ;; Copy source code and resources
  (fs/copy-tree "src" (str app-bundle "/Contents/Resources/src"))
  (when (fs/exists? "bbpad-ui/dist")
    (fs/copy-tree "bbpad-ui/dist" (str app-bundle "/Contents/Resources/public")))
  (fs/copy "bb.edn" (str app-bundle "/Contents/Resources/bb.edn"))
  (fs/copy "deps.edn" (str app-bundle "/Contents/Resources/deps.edn"))
  
  ;; Create launcher script
  (let [launcher (str app-bundle "/Contents/MacOS/" app-name)
        script "#!/bin/bash\nset -e\nSCRIPT_DIR=\"$( cd \"$( dirname \"${BASH_SOURCE[0]}\" )\" && pwd )\"\nAPP_DIR=\"$SCRIPT_DIR/../Resources\"\nexport BBPAD_APP_DIR=\"$APP_DIR\"\nexport BBPAD_BUNDLED=true\ncd \"$APP_DIR\"\nexec bb src/bbpad/main.clj \"$@\"\n"]
    (spit launcher script)
    (shell (str "chmod +x " launcher))))

(defn create-dmg! []
  (println "💿 Creating DMG...")
  (let [temp-dmg (str build-dir "/temp.dmg")
        final-dmg (str build-dir "/" dmg-name)]
    
    ;; Clean up existing files
    (when (fs/exists? temp-dmg) (fs/delete temp-dmg))
    (when (fs/exists? final-dmg) (fs/delete final-dmg))
    
    ;; Create DMG
    (shell (str "hdiutil create -srcfolder " app-bundle " -volname \"" app-name "\" -fs HFS+ -format UDZO -o " final-dmg))
    (println (str "✅ DMG created: " final-dmg))))

(defn package-macos! []
  (try
    (println "🚀 Starting macOS packaging...")
    (build-frontend!)
    (create-app-bundle!)  
    (create-dmg!)
    (println "🎉 macOS packaging complete!")
    (println (str "   App bundle: " app-bundle))
    (println (str "   DMG installer: " build-dir "/" dmg-name))
    {:success true}
    (catch Exception e
      (println (str "❌ Error: " (.getMessage e)))
      {:success false :error (.getMessage e)})))

(when (= *file* (System/getProperty "babashka.file"))
  (package-macos!))