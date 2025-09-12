(ns bbpad.ui.components.layout
  (:require [helix.core :refer [defnc $]]
            [helix.hooks :refer [use-state use-effect]]
            [helix.dom :as d]
            [bbpad.ui.components.editor :as editor]
            [bbpad.ui.components.results :as results]
            [bbpad.ui.components.toolbar :as toolbar]
            [bbpad.ui.state.app :as app-state]))

(defnc split-pane [{:keys [left right split-position on-split-change]}]
  (let [position (or split-position 50)]
    
    (d/div {:class "split-pane"
            :style {:display "flex"
                    :height "100%"
                    :width "100%"
                    :position "relative"}}
       
       (d/div {:class "split-pane-left"
               :style {:width (str position "%")
                       :height "100%"
                       :overflow "auto"}}
              left)
       
       (d/div {:class "split-pane-divider"
               :style {:width "4px"
                       :background "#e0e0e0"
                       :cursor "col-resize"
                       :position "relative"}}
          
          (d/div {:style {:position "absolute"
                          :top "50%"
                          :left "50%"
                          :transform "translate(-50%, -50%)"
                          :width "2px"
                          :height "30px"
                          :background "#999"}}))
       
       (d/div {:class "split-pane-right"
               :style {:flex 1
                       :height "100%"
                       :overflow "auto"}}
              right))))

(defnc main-layout []
  (let [[code set-code!] (use-state "(println \"Hello BBPad!\")")
        [results set-results!] (use-state nil)
        [executing? set-executing!] (use-state false)]
    
    (d/div {:class "main-layout"
            :style {:display "flex"
                    :flex-direction "column"
                    :height "100vh"
                    :width "100vw"
                    :background "#f5f5f5"}}
       
       ($ toolbar/toolbar
          {:on-execute (fn []
                        (set-executing! true)
                        (app-state/execute-script 
                         code
                         (fn [result]
                           (set-results! result)
                           (set-executing! false))))
           :executing? executing?})
       
       (d/div {:class "content"
               :style {:flex 1
                       :display "flex"
                       :overflow "hidden"}}
          
          ($ split-pane
             {:split-position 50
              :left ($ editor/code-editor
                      {:value code
                       :on-change set-code!
                       :executing? executing?})
              :right ($ results/results-panel
                       {:results results
                        :executing? executing?})})))))