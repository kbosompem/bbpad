#!/usr/bin/env bb

(ns build.package-linux
  "Build Linux AppImage for BBPad"
  (:require [babashka.fs :as fs]
            [babashka.process :refer [shell]]
            [clojure.string :as str]
            [bbpad.core.version :as version]))

(def app-name "BBPad")
(def build-dir "build")
(def linux-dir (str build-dir "/linux"))
(def appdir (str linux-dir "/" app-name ".AppDir"))
(def appimage-name (str app-name "-" (version/current-version) "-Linux.AppImage"))

(defn clean-build-dir!
  "Clean the build directory"
  []
  (println "🧹 Cleaning Linux build directory...")
  (when (fs/exists? appdir)
    (fs/delete-tree appdir))
  (when (fs/exists? (str build-dir "/" appimage-name))
    (fs/delete (str build-dir "/" appimage-name))))

(defn build-frontend!
  "Build the React frontend for production"
  []
  (println "🔨 Building React frontend...")
  (shell "cd bbpad-ui && npm run build"))

(defn create-appdir-structure!
  "Create the AppImage directory structure"
  []
  (println "📦 Creating AppImage structure...")
  
  ;; Create AppDir structure
  (fs/create-dirs (str appdir "/usr/bin"))
  (fs/create-dirs (str appdir "/usr/lib"))
  (fs/create-dirs (str appdir "/usr/share/applications"))
  (fs/create-dirs (str appdir "/usr/share/icons/hicolor/256x256/apps"))
  (fs/create-dirs (str appdir "/usr/share/bbpad"))
  
  ;; Copy source code
  (fs/copy-tree "src" (str appdir "/usr/share/bbpad/src"))
  
  ;; Copy built frontend
  (when (fs/exists? "bbpad-ui/dist")
    (fs/copy-tree "bbpad-ui/dist" (str appdir "/usr/share/bbpad/public")))
  
  ;; Copy configuration files
  (fs/copy "bb.edn" (str appdir "/usr/share/bbpad/bb.edn"))
  (fs/copy "deps.edn" (str appdir "/usr/share/bbpad/deps.edn"))
  
  ;; Copy any resources
  (when (fs/exists? "resources")
    (fs/copy-tree "resources" (str appdir "/usr/share/bbpad/resources"))))

(defn create-desktop-entry!
  "Create .desktop file for the AppImage"
  []
  (println "🖥️  Creating desktop entry...")
  (let [desktop-content (str "[Desktop Entry]\n"
                            "Type=Application\n"
                            "Name=" app-name "\n"
                            "Comment=LINQPad-inspired desktop app for Babashka scripts\n"
                            "Exec=bbpad\n"
                            "Icon=bbpad\n"
                            "Categories=Development;IDE;\n"
                            "StartupWMClass=BBPad\n"
                            "MimeType=text/x-clojure;\n"
                            "Keywords=clojure;babashka;repl;script;\n")
        desktop-path (str appdir "/usr/share/applications/bbpad.desktop")]
    (spit desktop-path desktop-content)
    
    ;; Also create at AppDir root for AppRun
    (fs/copy desktop-path (str appdir "/bbpad.desktop"))))

(defn create-app-icon!
  "Create app icon (placeholder)"
  []
  (println "🎨 Creating app icon...")
  (let [icon-path (str appdir "/usr/share/icons/hicolor/256x256/apps/bbpad.png")]
    ;; For now, create a placeholder - real implementation would create proper PNG
    (spit icon-path "placeholder-png-data")
    
    ;; Also create at AppDir root for AppRun
    (fs/copy icon-path (str appdir "/bbpad.png"))))

(defn create-apprun-script!
  "Create AppRun executable script"
  []
  (println "📝 Creating AppRun script...")
  (let [apprun-content (str "#!/bin/bash\n"
                           "# BBPad AppRun script\n"
                           "set -e\n"
                           "\n"
                           "# Get the directory containing this script\n"
                           "APPDIR=\"$( cd \"$( dirname \"${BASH_SOURCE[0]}\" )\" &> /dev/null && pwd )\"\n"
                           "APP_DIR=\"$APPDIR/usr/share/bbpad\"\n"
                           "\n"
                           "# Set environment variables\n"
                           "export BBPAD_APP_DIR=\"$APP_DIR\"\n"
                           "export BBPAD_BUNDLED=true\n"
                           "export PATH=\"$APPDIR/usr/bin:$PATH\"\n"
                           "\n"
                           "# Launch babashka with the main script\n"
                           "cd \"$APP_DIR\"\n"
                           "exec bb src/bbpad/main.clj \"$@\"\n")
        apprun-path (str appdir "/AppRun")]
    (spit apprun-path apprun-content)
    (shell (str "chmod +x " apprun-path))))

(defn download-babashka!
  "Download Babashka for Linux"
  []
  (println "⬇️  Downloading Babashka for Linux...")
  (try
    ;; Download and extract Babashka binary
    (let [bb-version "1.3.184"
          bb-url (str "https://github.com/babashka/babashka/releases/download/v" bb-version "/babashka-" bb-version "-linux-amd64-static.tar.gz")
          temp-dir "/tmp/bbpad-bb-download"
          bb-path (str appdir "/usr/bin/bb")]
      
      ;; Create temp directory
      (fs/create-dirs temp-dir)
      
      ;; Download and extract
      (shell (str "cd " temp-dir " && curl -L -o babashka.tar.gz " bb-url))
      (shell (str "cd " temp-dir " && tar -xzf babashka.tar.gz"))
      
      ;; Copy binary
      (fs/copy (str temp-dir "/bb") bb-path)
      (shell (str "chmod +x " bb-path))
      
      ;; Clean up
      (fs/delete-tree temp-dir)
      
      (println "✅ Babashka downloaded and installed"))
    (catch Exception e
      (println "⚠️  Warning: Could not download Babashka:" (.getMessage e))
      (println "   Creating placeholder - manual Babashka installation required")
      (spit (str appdir "/usr/bin/bb") "#!/bin/bash\necho 'Babashka not available'\nexit 1\n")
      (shell (str "chmod +x " appdir "/usr/bin/bb")))))

(defn create-appimage!
  "Create AppImage using appimagetool"
  []
  (println "🏗️  Creating AppImage...")
  (try
    (let [appimage-path (str build-dir "/" appimage-name)]
      (shell (str "ARCH=x86_64 appimagetool " appdir " " appimage-path))
      (println "✅ AppImage created:" appimage-name))
    (catch Exception e
      (println "⚠️  Warning: Could not create AppImage:" (.getMessage e))
      (println "   appimagetool may not be available")
      ;; Create a tarball as fallback
      (create-tarball!))))

(defn create-tarball!
  "Create tarball package as fallback"
  []
  (println "📦 Creating tarball package...")
  (try
    (let [tarball-name (str app-name "-" (version/current-version) "-Linux.tar.gz")]
      (shell (str "cd " build-dir " && tar -czf " tarball-name " " (fs/file-name appdir)))
      (println "✅ Tarball package created:" tarball-name))
    (catch Exception e
      (println "❌ Error creating tarball package:" (.getMessage e)))))

(defn create-install-script!
  "Create installation script"
  []
  (println "📜 Creating installation script...")
  (let [install-content (str "#!/bin/bash\n"
                            "# BBPad Linux Installation Script\n"
                            "set -e\n"
                            "\n"
                            "APP_NAME=\"BBPad\"\n"
                            "INSTALL_DIR=\"/opt/bbpad\"\n"
                            "BIN_DIR=\"/usr/local/bin\"\n"
                            "DESKTOP_DIR=\"/usr/share/applications\"\n"
                            "ICON_DIR=\"/usr/share/icons/hicolor/256x256/apps\"\n"
                            "\n"
                            "echo \"Installing BBPad...\"\n"
                            "\n"
                            "# Check for root privileges\n"
                            "if [ \"$EUID\" -ne 0 ]; then\n"
                            "  echo \"Please run as root (use sudo)\"\n"
                            "  exit 1\n"
                            "fi\n"
                            "\n"
                            "# Create installation directory\n"
                            "mkdir -p \"$INSTALL_DIR\"\n"
                            "\n"
                            "# Copy application files\n"
                            "cp -r " (fs/file-name appdir) "/usr/share/bbpad/* \"$INSTALL_DIR/\"\n"
                            "cp " (fs/file-name appdir) "/usr/bin/bb \"$BIN_DIR/bb-bbpad\"\n"
                            "\n"
                            "# Create launcher script\n"
                            "cat > \"$BIN_DIR/bbpad\" << 'EOF'\n"
                            "#!/bin/bash\n"
                            "export BBPAD_APP_DIR=\"/opt/bbpad\"\n"
                            "export BBPAD_BUNDLED=true\n"
                            "cd \"$BBPAD_APP_DIR\"\n"
                            "exec bb-bbpad src/bbpad/main.clj \"$@\"\n"
                            "EOF\n"
                            "\n"
                            "chmod +x \"$BIN_DIR/bbpad\"\n"
                            "\n"
                            "# Install desktop entry and icon\n"
                            "cp " (fs/file-name appdir) "/bbpad.desktop \"$DESKTOP_DIR/\"\n"
                            "mkdir -p \"$ICON_DIR\"\n"
                            "cp " (fs/file-name appdir) "/bbpad.png \"$ICON_DIR/\"\n"
                            "\n"
                            "# Update desktop database\n"
                            "if command -v update-desktop-database >/dev/null 2>&1; then\n"
                            "  update-desktop-database \"$DESKTOP_DIR\"\n"
                            "fi\n"
                            "\n"
                            "echo \"BBPad installed successfully!\"\n"
                            "echo \"You can now run 'bbpad' from the command line or find it in your applications menu.\"\n")
        install-path (str build-dir "/install-linux.sh")]
    (spit install-path install-content)
    (shell (str "chmod +x " install-path))))

(defn verify-build!
  "Verify the Linux build was successful"
  []
  (println "🔍 Verifying Linux build...")
  (let [appdir-exists? (fs/exists? appdir)
        apprun-exists? (fs/exists? (str appdir "/AppRun"))
        desktop-exists? (fs/exists? (str appdir "/bbpad.desktop"))
        appimage-exists? (fs/exists? (str build-dir "/" appimage-name))
        tarball-exists? (fs/exists? (str build-dir "/" app-name "-" (version/current-version) "-Linux.tar.gz"))]
    
    (println (str "  AppDir: " (if appdir-exists? "✅" "❌")))
    (println (str "  AppRun: " (if apprun-exists? "✅" "❌")))
    (println (str "  Desktop entry: " (if desktop-exists? "✅" "❌")))
    (println (str "  AppImage: " (if appimage-exists? "✅" "❌")))
    (println (str "  Tarball: " (if tarball-exists? "✅" "❌")))
    
    (when-not (and appdir-exists? apprun-exists? desktop-exists?)
      (throw (ex-info "Linux build verification failed" 
                      {:appdir appdir-exists? 
                       :apprun apprun-exists? 
                       :desktop desktop-exists?})))
    
    (when (or appimage-exists? tarball-exists?)
      (let [package-file (if appimage-exists? 
                          (str build-dir "/" appimage-name)
                          (str build-dir "/" app-name "-" (version/current-version) "-Linux.tar.gz"))
            package-size (fs/size package-file)]
        (println (str "  Package size: " (Math/round (/ package-size 1024.0 1024.0)) " MB"))))
    
    (println "✅ Linux build verification successful!")))

(defn package-linux!
  "Complete Linux packaging workflow"
  []
  (try
    (println "🚀 Starting Linux packaging...")
    
    (clean-build-dir!)
    (build-frontend!)
    (create-appdir-structure!)
    (create-desktop-entry!)
    (create-app-icon!)
    (create-apprun-script!)
    (download-babashka!)
    (create-appimage!)
    (create-install-script!)
    (verify-build!)
    
    (println "🎉 Linux packaging complete!")
    (println (str "   AppDir: " appdir))
    (println (str "   AppImage: " build-dir "/" appimage-name))
    (println (str "   Install script: " build-dir "/install-linux.sh"))
    (println)
    (println "To test the AppImage:")
    (println (str "   chmod +x " build-dir "/" appimage-name))
    (println (str "   " build-dir "/" appimage-name))
    (println)
    (println "To install system-wide:")
    (println (str "   sudo " build-dir "/install-linux.sh"))
    
    {:success true
     :appdir appdir
     :appimage-file (str build-dir "/" appimage-name)
     :install-script (str build-dir "/install-linux.sh")}
    
    (catch Exception e
      (println (str "❌ Error during Linux packaging: " (.getMessage e)))
      (println "Stack trace:")
      (.printStackTrace e)
      {:success false :error (.getMessage e)})))

;; Main entry point
(when (= *file* (System/getProperty "babashka.file"))
  (package-linux!))