(ns bbpad.ui.components.toolbar
  (:require [helix.core :refer [defnc $]]
            [helix.dom :as d]))

(defnc toolbar [{:keys [on-execute executing? on-save on-load]}]
  (d/div {:class "toolbar"
          :style {:height "48px"
                  :background "#2d2d30"
                  :border-bottom "1px solid #3e3e42"
                  :display "flex"
                  :align-items "center"
                  :padding "0 16px"
                  :gap "12px"}}
     
     (d/button {:class "execute-btn"
                :style {:padding "6px 16px"
                        :background (if executing? "#555" "#007acc")
                        :color "white"
                        :border "none"
                        :border-radius "4px"
                        :font-family "system-ui, -apple-system, sans-serif"
                        :font-size "14px"
                        :font-weight "500"
                        :cursor (if executing? "not-allowed" "pointer")
                        :display "flex"
                        :align-items "center"
                        :gap "6px"}
                :disabled executing?
                :on-click (when (and on-execute (not executing?))
                           on-execute)}
        
        (if executing?
          (d/span "Executing...")
          (d/span "▶ Execute (F5)")))
     
     (d/div {:class "separator"
             :style {:width "1px"
                     :height "24px"
                     :background "#3e3e42"}})
     
     (d/button {:class "save-btn"
                :style {:padding "6px 12px"
                        :background "transparent"
                        :color "#cccccc"
                        :border "1px solid #3e3e42"
                        :border-radius "4px"
                        :font-family "system-ui, -apple-system, sans-serif"
                        :font-size "14px"
                        :cursor "pointer"}
                :on-click on-save
                :disabled executing?}
               "Save")
     
     (d/button {:class "load-btn"
                :style {:padding "6px 12px"
                        :background "transparent"
                        :color "#cccccc"
                        :border "1px solid #3e3e42"
                        :border-radius "4px"
                        :font-family "system-ui, -apple-system, sans-serif"
                        :font-size "14px"
                        :cursor "pointer"}
                :on-click on-load
                :disabled executing?}
               "Load")
     
     (d/div {:class "spacer" :style {:flex 1}})
     
     (d/div {:class "title"
             :style {:color "#9cdcfe"
                     :font-family "system-ui, -apple-system, sans-serif"
                     :font-size "14px"
                     :font-weight "500"}}
            "BBPad - Babashka Script Runner")))