#!/usr/bin/env bb

(ns custom-babashka
  "Build script for creating custom Babashka with BBPad-specific features"
  (:require [babashka.fs :as fs]
            [babashka.process :refer [shell]]
            [clojure.string :as str]))

(def feature-flags
  "Feature flags to enable in custom Babashka build"
  {:jdbc true
   :postgresql true
   :mysql true
   :hsqldb true
   :httpkit-server true
   :httpkit-client true
   :data-readers true})

(defn set-env-vars!
  "Set environment variables for custom build"
  []
  (doseq [[feature enabled?] feature-flags]
    (when enabled?
      (let [env-var (str "BABASHKA_FEATURE_" (str/upper-case (name feature)))]
        (println (str "Setting " env-var "=true"))
        (System/setProperty env-var "true"))))
  
  ;; Additional build settings
  (System/setProperty "NATIVE_IMAGE_DEPRECATED_BUILDER_SANITATION" "true")
  (System/setProperty "GRAALVM_HOME" (or (System/getenv "GRAALVM_HOME") 
                                         "/usr/lib/graalvm")))

(defn check-prerequisites
  "Check if build prerequisites are available"
  []
  (let [graalvm-home (System/getProperty "GRAALVM_HOME")]
    (println "🔍 Checking build prerequisites...")
    
    ;; Check GraalVM
    (if (and graalvm-home (fs/exists? graalvm-home))
      (println (str "✅ GraalVM found at: " graalvm-home))
      (do
        (println "❌ GraalVM not found. Please set GRAALVM_HOME")
        (System/exit 1)))
    
    ;; Check available memory
    (let [max-memory (/ (.maxMemory (Runtime/getRuntime)) 1024 1024)]
      (if (> max-memory 4000)
        (println (str "✅ Available memory: " (int max-memory) "MB"))
        (println (str "⚠️  Low memory: " (int max-memory) "MB. Build may fail."))))
    
    ;; Check if babashka source is available
    (if (fs/exists? "babashka")
      (println "✅ Babashka source directory found")
      (do
        (println "❌ Babashka source not found. Cloning...")
        (shell "git clone https://github.com/babashka/babashka.git")))))

(defn build-custom-babashka
  "Build custom Babashka with BBPad features"
  []
  (println "🔨 Building custom Babashka...")
  
  (let [build-dir "babashka"
        start-time (System/currentTimeMillis)]
    
    ;; Change to babashka directory
    (fs/set-current-directory build-dir)
    
    (try
      ;; Set environment variables
      (set-env-vars!)
      
      ;; Build uberjar
      (println "📦 Building uberjar...")
      (shell "script/uberjar")
      
      ;; Compile native image
      (println "🏗️  Compiling native image (this may take 10-15 minutes)...")
      (shell "script/compile")
      
      (let [elapsed (/ (- (System/currentTimeMillis) start-time) 1000.0)]
        (println (str "✅ Custom Babashka built successfully in " elapsed "s")))
      
      ;; Copy to BBPad directory
      (let [bb-binary (if (str/includes? (System/getProperty "os.name") "Windows")
                        "bb.exe" 
                        "bb")]
        (when (fs/exists? bb-binary)
          (fs/copy bb-binary (str "../bbpad-bb-" bb-binary))
          (println (str "✅ Custom binary copied to bbpad-bb-" bb-binary))))
      
      (catch Exception e
        (println (str "❌ Build failed: " (.getMessage e)))
        (.printStackTrace e)
        (System/exit 1))
      
      (finally
        ;; Return to original directory
        (fs/set-current-directory "..")))))

(defn create-fallback-script
  "Create fallback script using standard bb + pods"
  []
  (println "📝 Creating fallback script for standard Babashka...")
  
  (let [script-content (str "#!/usr/bin/env bb\n\n"
                           ";; BBPad fallback script - uses standard bb + pods\n\n"
                           "(require '[babashka.pods :as pods])\n\n"
                           ";; Load required pods\n"
                           "(pods/load-pod 'org.babashka/postgresql \"0.1.0\")\n"
                           "(pods/load-pod 'org.babashka/hsqldb \"0.1.0\")\n\n"
                           ";; Load main BBPad\n"
                           "(load-file \"src/bbpad/main.clj\")\n\n"
                           ";; Start BBPad\n"
                           "(apply bbpad.main/-main *command-line-args*)\n")]
    
    (spit "bbpad-fallback" script-content)
    (shell "chmod +x bbpad-fallback")
    (println "✅ Fallback script created: bbpad-fallback")))

(defn -main [& args]
  (println "🚀 BBPad Custom Babashka Builder")
  (println "===============================")
  
  (let [build-type (first args)]
    (case build-type
      "custom" (do
                 (check-prerequisites)
                 (build-custom-babashka))
      
      "fallback" (create-fallback-script)
      
      "both" (do
               (check-prerequisites)
               (build-custom-babashka)
               (create-fallback-script))
      
      ;; Default: try custom, fallback on failure
      (do
        (println "Attempting custom build first...")
        (try
          (check-prerequisites)
          (build-custom-babashka)
          (catch Exception e
            (println "Custom build failed, creating fallback...")
            (create-fallback-script))))))
  
  (println "\n🎉 Build process complete!"))

;; Allow direct execution
(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))