(ns bbpad.ui.core
  (:require [helix.core :refer [defnc $]]
            [helix.dom :as d]
            ["react-dom" :as rdom]
            [bbpad.ui.components.layout :as layout]
            [bbpad.ui.state.app :as app-state]))

(defnc app []
  (d/div {:style {:padding "20px"
                  :background "white"
                  :color "black"
                  :font-family "Arial, sans-serif"}}
         (d/h1 "BBPad - Loading...")
         (d/p "If you see this, React is working!")
         ($ layout/main-layout)))

(defn init! []
  (js/console.log "BBPad initializing...")
  (let [root (js/document.getElementById "app")]
    (js/console.log "Root element:" root)
    (if root
      (do
        (js/console.log "Starting app...")
        (app-state/init!)
        (rdom/render ($ app) root)
        (js/console.log "App rendered!"))
      (js/console.error "Could not find #app element!"))))

(defn ^:dev/after-load reload! []
  (init!))