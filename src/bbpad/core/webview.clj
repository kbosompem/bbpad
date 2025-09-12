(ns bbpad.core.webview
  "WebView management for BBPad - handles platform-specific WebView launching"
  (:require [bbpad.core.config :as config]
            [clojure.java.io :as io]
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

(defn find-webview-executable
  "Find the appropriate WebView executable for the current platform"
  []
  (let [platform (detect-platform)
        webview-dir "webview/bin"
        executable-name (case platform
                          :windows "webview.exe"
                          :macos "webview"
                          :linux "webview"
                          nil)]
    (when executable-name
      (let [local-path (str webview-dir "/" executable-name)
            bundled-resource (io/resource local-path)]
        (cond
          ;; Check if bundled executable exists
          bundled-resource
          (let [temp-file (java.io.File/createTempFile "bbpad-webview" 
                                                       (if (= platform :windows) ".exe" ""))]
            (.deleteOnExit temp-file)
            (io/copy (io/input-stream bundled-resource) temp-file)
            (.setExecutable temp-file true)
            (.getAbsolutePath temp-file))
          
          ;; Check if local file exists (development)
          (.exists (io/file local-path))
          (.getAbsolutePath (io/file local-path))
          
          :else nil)))))

(defn launch-browser-fallback
  "Fallback to launching browser in app mode"
  [url {:keys [dev-mode]}]
  (let [platform (detect-platform)
        browser-cmd (case platform
                      :windows ["cmd" "/c" "start" "chrome" "--app=%s" "--user-data-dir=%TEMP%\\bbpad"]
                      :macos ["open" "-a" "Google Chrome" "--args" "--app=%s" "--user-data-dir=/tmp/bbpad"]
                      :linux ["google-chrome" "--app=%s" "--user-data-dir=/tmp/bbpad"]
                      nil)]
    
    (if browser-cmd
      (try
        (let [formatted-cmd (map #(str/replace % "%s" url) browser-cmd)
              result (apply sh formatted-cmd)]
          (if (zero? (:exit result))
            (println "🌐 Launched browser in app mode")
            (println "⚠️  Browser launch failed, please open manually:" url)))
        (catch Exception e
          (println "⚠️  Could not launch browser automatically")
          (println "Please open this URL manually:" url)
          (when dev-mode
            (.printStackTrace e))))
      (println "Please open this URL in your browser:" url))))

(defn launch-webview-native
  "Launch native WebView if available"
  [url {:keys [dev-mode] :as options}]
  (if-let [webview-exe (find-webview-executable)]
    (try
      (let [webview-config (config/get-webview-config)
            args [webview-exe
                  "--url" url
                  "--title" (:title webview-config "BBPad")
                  "--width" (str (:width webview-config 1200))
                  "--height" (str (:height webview-config 800))]
            args (if (:dev-tools webview-config false)
                   (conj args "--dev-tools")
                   args)
            result (apply sh args)]
        
        (if (zero? (:exit result))
          (println "🖥️  Native WebView launched successfully")
          (do
            (println "⚠️  Native WebView failed, falling back to browser")
            (when dev-mode
              (println "WebView error:" (:err result)))
            (launch-browser-fallback url options))))
      
      (catch Exception e
        (println "⚠️  WebView launch exception, falling back to browser")
        (when dev-mode
          (.printStackTrace e))
        (launch-browser-fallback url options)))
    
    (do
      (println "ℹ️  No WebView executable found, using browser fallback")
      (launch-browser-fallback url options))))

(defn launch-webview!
  "Launch WebView with the given URL"
  [url options]
  (println (str "🚀 Opening BBPad at: " url))
  
  ;; Try native WebView first, fall back to browser
  (launch-webview-native url options)
  
  ;; Give user instructions
  (when (config/dev-mode?)
    (println "💡 Tips:")
    (println "  - If WebView doesn't open, try opening the URL manually")
    (println "  - Press Ctrl+C to stop BBPad")
    (println "  - Check the console for any error messages")))