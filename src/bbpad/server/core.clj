(ns bbpad.server.core
  "Babashka HTTP server for BBPad web interface - using org.babashka/http-server"
  (:require [bbpad.server.babashka-http :as http]
            [bbpad.core.config :as config]))

;; Delegate all server operations to babashka-http

(defn start-server! 
  "Start the Babashka HTTP server"
  [options]
  (http/start-server! options))

(defn stop-server! []
  "Stop the Babashka HTTP server"
  (http/stop-server!))

(defn restart-server! [options]
  "Restart the server with new options"
  (http/restart-server! options))