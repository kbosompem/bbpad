(ns bbpad.core.webview
  "Cross-platform WebView implementation for BBPad using platform-specific solutions"
  (:require [bbpad.core.config :as config]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as str]))

(defn detect-platform
  "Detect the current platform"
  []
  (let [os-name (str/lower-case (System/getProperty "os.name"))]
    (cond
      (str/includes? os-name "windows") :windows
      (str/includes? os-name "mac") :macos
      (str/includes? os-name "linux") :linux
      :else :unknown)))

(defn launch-macos-webview!
  "Launch Safari WebView"
  [url {:keys [dev-mode] :as options}]
  (try
    (println (str "🚀 Opening BBPad WebView at: " url))
    ;; Use open command to launch in Safari
    (let [result (sh "open" "-a" "Safari" url)]
      (if (zero? (:exit result))
        (do
          (println "🖥️  Safari WebView launched successfully")
          true)
        (do
          (println "❌ Failed to launch Safari WebView")
          (when dev-mode
            (println "Error:" (:err result)))
          false)))
    (catch Exception e
      (println "❌ Safari WebView launch failed:")
      (println (.getMessage e))
      (when dev-mode
        (.printStackTrace e))
      false)))

(defn launch-chrome-app-mode!
  "Launch Chrome in app mode (kiosk-like mode)"
  [url {:keys [dev-mode] :as options}]
  (let [config (config/get-webview-config)
        width (:width config 1200)
        height (:height config 800)
        title (:title config "BBPad")

        ;; Chrome flags for app mode
        chrome-flags ["--app=" url
                     "--disable-web-security"
                     "--disable-features=TranslateUI"
                     "--disable-extensions"
                     "--disable-plugins"
                     "--disable-default-apps"
                     "--disable-background-timer-throttling"
                     "--disable-background-networking"
                     (str "--window-size=" width "," height)
                     (str "--window-position=100,100")]]

    (try
      (println (str "🚀 Opening BBPad in Chrome app mode at: " url))

      ;; Try different Chrome executable paths
      (let [chrome-paths (case (detect-platform)
                          :macos ["/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
                                 "/Applications/Chromium.app/Contents/MacOS/Chromium"]
                          :windows ["chrome.exe" "chrome" "chromium"]
                          :linux ["google-chrome" "chrome" "chromium-browser" "chromium"])

            chrome-exe (first (filter #(try
                                        (let [result (sh % "--version")]
                                          (zero? (:exit result)))
                                        (catch Exception _ false))
                                      chrome-paths))]

        (if chrome-exe
          (let [result (apply sh chrome-exe chrome-flags)]
            (if (zero? (:exit result))
              (do
                (println "🖥️  Chrome app mode launched successfully")
                true)
              (do
                (println "❌ Failed to launch Chrome app mode")
                (when dev-mode
                  (println "Error:" (:err result)))
                false)))
          (do
            (println "❌ Chrome/Chromium not found")
            false)))

      (catch Exception e
        (println "❌ Chrome app mode launch failed:")
        (println (.getMessage e))
        (when dev-mode
          (.printStackTrace e))
        false))))

(defn launch-electron!
  "Launch Electron app"
  [url {:keys [dev-mode] :as options}]
  (try
    (println (str "🚀 Opening BBPad Electron app at: " url))

    ;; Launch Electron app which will handle starting the server
    (let [project-root (System/getProperty "user.dir")
          electron-cmd (if dev-mode "app:dev" "app")
          result (sh "npm" "run" electron-cmd :dir project-root)]
      (if (zero? (:exit result))
        (do
          (println "🖥️  Electron app launched successfully")
          true)
        (do
          (println "❌ Failed to launch Electron app")
          (when dev-mode
            (println "Error:" (:err result)))
          false)))
    (catch Exception e
      (println "❌ Electron app launch failed:")
      (println (.getMessage e))
      (when dev-mode
        (.printStackTrace e))
      false)))

(defn launch-webview!
  "Launch Electron app instead of browser WebView"
  [url options]
  (println (str "🚀 Starting BBPad Desktop App..."))

  ;; Don't launch Electron from within bb dev - that would create a loop
  ;; Instead, just show instructions for the user
  (println "💡 To run BBPad as a desktop app:")
  (println "  Run: npm run app")
  (println "  This will build the frontend and launch Electron")
  (println "")
  (println "🌐 For now, opening in Safari...")

  ;; Fall back to Safari for now when called from bb dev
  (launch-macos-webview! url options))