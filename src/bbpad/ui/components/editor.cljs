(ns bbpad.ui.components.editor
  (:require [helix.core :refer [defnc $]]
            [helix.dom :as d]))

(defnc code-editor [{:keys [value on-change executing?]}]
  (d/div {:class "editor-container"
          :style {:width "100%"
                  :height "100%"
                  :position "relative"
                  :background "#1e1e1e"
                  :color "#d4d4d4"}}
    (d/textarea {:class "code-textarea"
                 :value value
                 :on-change (fn [e] (when on-change (on-change (.. e -target -value))))
                 :disabled executing?
                 :style {:width "100%"
                         :height "100%"
                         :border "none"
                         :outline "none"
                         :resize "none"
                         :padding "16px"
                         :font-family "JetBrains Mono, Consolas, Monaco, monospace"
                         :font-size "14px"
                         :line-height "1.5"
                         :background "#1e1e1e"
                         :color "#d4d4d4"
                         :tab-size "2"}})))