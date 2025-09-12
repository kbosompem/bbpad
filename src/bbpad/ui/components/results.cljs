(ns bbpad.ui.components.results
  (:require [helix.core :refer [defnc $]]
            [helix.dom :as d]
            [clojure.string :as str]
            [clojure.edn :as edn]))

(defn apply-basic-syntax-highlighting [code-str]
  (-> code-str
      (str/replace #"\b(defn|def|let|if|when|cond|case|fn|quote|do)\b" 
                   "<span style='color: #569cd6; font-weight: bold;'>$1</span>")
      (str/replace #"\b(true|false|nil)\b" 
                   "<span style='color: #4ec9b0;'>$1</span>")
      (str/replace #"\"[^\"]*\"" 
                   "<span style='color: #ce9178;'>$&</span>")
      (str/replace #"\b\d+\.?\d*\b" 
                   "<span style='color: #b5cea8;'>$&</span>")
      (str/replace #":[a-zA-Z][a-zA-Z0-9-]*" 
                   "<span style='color: #9cdcfe;'>$&</span>")
      (str/replace #";[^\n]*" 
                   "<span style='color: #6a9955; font-style: italic;'>$&</span>")))

(defnc syntax-highlighted-pre [{:keys [content style]}]
  (d/div {:style (merge {:margin 0
                        :padding 0
                        :white-space "pre-wrap"
                        :overflow-x "auto"
                        :font-family "JetBrains Mono, Consolas, Monaco, monospace"}
                       style)
         :dangerouslySetInnerHTML {:__html (apply-basic-syntax-highlighting content)}}))

(defnc table-renderer [{:keys [data]}]
  (when (and (seq data) (every? map? data))
    (let [columns (-> data first keys)
          sorted-data (atom data)]
      (d/div {:style {:overflow "auto"}}
        (d/table {:style {:width "100%"
                         :border-collapse "collapse"
                         :background "#2d2d30"
                         :color "#d4d4d4"
                         :font-family "JetBrains Mono, Consolas, Monaco, monospace"
                         :font-size "13px"}}
          (d/thead
            (d/tr {:style {:background "#3e3e42"}}
              (for [col columns]
                (d/th {:key (str col)
                      :style {:padding "8px 12px"
                             :text-align "left"
                             :border-bottom "1px solid #555"
                             :font-weight "600"
                             :color "#9cdcfe"}}
                      (name col)))))
          (d/tbody
            (for [[idx row] (map-indexed vector data)]
              (d/tr {:key idx
                    :style {:border-bottom "1px solid #333"}}
                (for [col columns]
                  (d/td {:key (str idx "-" col)
                        :style {:padding "6px 12px"
                               :border-right "1px solid #333"
                               :vertical-align "top"}}
                        (str (get row col))))))))))))

(defnc export-buttons [{:keys [data type]}]
  (d/div {:style {:margin-top "8px"
                 :display "flex"
                 :gap "8px"}}
    (when (= type :table)
      (d/button {:style {:padding "4px 8px"
                        :background "#007acc"
                        :color "white"
                        :border "none"
                        :border-radius "4px"
                        :font-size "12px"
                        :cursor "pointer"}
                :on-click (fn []
                           (let [csv-content (str/join "\n" 
                                                      (cons (str/join "," (-> data first keys (map name)))
                                                            (map #(str/join "," (vals %)) data)))
                                 blob (js/Blob. [csv-content] #js {:type "text/csv"})
                                 url (js/URL.createObjectURL blob)
                                 a (.createElement js/document "a")]
                             (set! (.-href a) url)
                             (set! (.-download a) "results.csv")
                             (.click a)
                             (js/URL.revokeObjectURL url)))}
        "Export CSV"))
    (d/button {:style {:padding "4px 8px"
                      :background "#4caf50"
                      :color "white"
                      :border "none"
                      :border-radius "4px"
                      :font-size "12px"
                      :cursor "pointer"}
              :on-click (fn []
                         (let [json-content (js/JSON.stringify (clj->js data) nil 2)
                               blob (js/Blob. [json-content] #js {:type "application/json"})
                               url (js/URL.createObjectURL blob)
                               a (.createElement js/document "a")]
                           (set! (.-href a) url)
                           (set! (.-download a) "results.json")
                           (.click a)
                           (js/URL.revokeObjectURL url)))}
      "Export JSON")))

(defnc result-item [{:keys [type content data data-type]}]
  (let [type-kw (if (keyword? type) type (keyword type))]
    (case type-kw
    :output (d/pre {:class "output"
                    :style {:margin 0
                            :padding "8px"
                            :background "#1e1e1e"
                            :color "#d4d4d4"
                            :font-family "JetBrains Mono, Consolas, Monaco, monospace"
                            :font-size "14px"
                            :white-space "pre-wrap"
                            :overflow-x "auto"}}
                   content)
    
    :error (d/div {:class "error"
                   :style {:padding "12px"
                           :background "#3c1f1f"
                           :border-left "4px solid #f44336"
                           :color "#ff9999"
                           :font-family "JetBrains Mono, Consolas, Monaco, monospace"
                           :font-size "14px"}}
             (d/div {:style {:font-weight "bold"
                            :margin-bottom "8px"}}
                    "Error:")
             (d/pre {:style {:margin 0
                            :white-space "pre-wrap"}}
                    content))
    
    :table (d/div {:class "table-result"
                   :style {:padding "12px"
                           :background "#2d2d30"
                           :border-left "4px solid #ff9800"
                           :color "#d4d4d4"}}
             (d/div {:style {:font-weight "bold"
                            :margin-bottom "12px"
                            :color "#9cdcfe"
                            :font-family "system-ui, -apple-system, sans-serif"}}
                    (str "Table (" (count data) " rows):"))
             ($ table-renderer {:data data})
             ($ export-buttons {:data data :type :table}))
    
    :map (d/div {:class "map-result"
                 :style {:padding "12px"
                         :background "#2d2d30"
                         :border-left "4px solid #9c27b0"
                         :color "#b5cea8"
                         :font-family "JetBrains Mono, Consolas, Monaco, monospace"
                         :font-size "14px"}}
           (d/div {:style {:font-weight "bold"
                          :margin-bottom "8px"
                          :color "#9cdcfe"}}
                  "Map:")
           ($ syntax-highlighted-pre {:content content})
           ($ export-buttons {:data data :type :map}))
    
    :vector (d/div {:class "vector-result"
                    :style {:padding "12px"
                            :background "#2d2d30"
                            :border-left "4px solid #2196f3"
                            :color "#b5cea8"
                            :font-family "JetBrains Mono, Consolas, Monaco, monospace"
                            :font-size "14px"}}
              (d/div {:style {:font-weight "bold"
                             :margin-bottom "8px"
                             :color "#9cdcfe"}}
                     (str "Vector (" (count data) " items):"))
              ($ syntax-highlighted-pre {:content content
                                                 :style {:max-height "300px"
                                                        :overflow-y "auto"}})
              ($ export-buttons {:data data :type :vector}))
    
    :list (d/div {:class "list-result"
                  :style {:padding "12px"
                          :background "#2d2d30"
                          :border-left "4px solid #00bcd4"
                          :color "#b5cea8"
                          :font-family "JetBrains Mono, Consolas, Monaco, monospace"
                          :font-size "14px"}}
            (d/div {:style {:font-weight "bold"
                           :margin-bottom "8px"
                           :color "#9cdcfe"}}
                   "List:")
            ($ syntax-highlighted-pre {:content content
                                       :style {:max-height "300px"
                                              :overflow-y "auto"}})
            ($ export-buttons {:data data :type :list}))
    
    :set (d/div {:class "set-result"
                 :style {:padding "12px"
                         :background "#2d2d30"
                         :border-left "4px solid #795548"
                         :color "#b5cea8"
                         :font-family "JetBrains Mono, Consolas, Monaco, monospace"
                         :font-size "14px"}}
           (d/div {:style {:font-weight "bold"
                          :margin-bottom "8px"
                          :color "#9cdcfe"}}
                  "Set:")
           ($ syntax-highlighted-pre {:content content
                                      :style {:max-height "300px"
                                             :overflow-y "auto"}})
           ($ export-buttons {:data data :type :set}))
    
    :value (d/div {:class "value"
                   :style {:padding "12px"
                           :background "#2d2d30"
                           :border-left "4px solid #4caf50"
                           :color "#b5cea8"
                           :font-family "JetBrains Mono, Consolas, Monaco, monospace"
                           :font-size "14px"}}
             (d/div {:style {:font-weight "bold"
                            :margin-bottom "8px"
                            :color "#9cdcfe"}}
                    (str "Result (" (or data-type "unknown") "):"))
             (d/pre {:style {:margin 0
                            :white-space "pre-wrap"}}
                    content)
             (when data
               ($ export-buttons {:data data :type :value})))
    
    (d/div "Unknown result type"))))

(defnc results-panel [{:keys [results executing?]}]
  (d/div {:class "results-panel"
          :style {:height "100%"
                  :background "#1e1e1e"
                  :color "#d4d4d4"
                  :display "flex"
                  :flex-direction "column"}}
     
     (d/div {:class "results-header"
             :style {:padding "12px"
                     :background "#2d2d30"
                     :border-bottom "1px solid #3e3e42"
                     :font-family "system-ui, -apple-system, sans-serif"
                     :font-size "14px"
                     :font-weight "500"
                     :display "flex"
                     :align-items "center"
                     :justify-content "space-between"}}
        
        (d/span "Results")
        
        (when executing?
          (d/div {:class "spinner"
                  :style {:width "16px"
                          :height "16px"
                          :border "2px solid #007acc"
                          :border-top-color "transparent"
                          :border-radius "50%"
                          :animation "spin 1s linear infinite"}})))
     
     (d/div {:class "results-content"
             :style {:flex 1
                     :overflow-y "auto"
                     :padding "12px"}}
        
        (cond
          executing?
          (d/div {:style {:color "#9cdcfe"
                         :font-family "JetBrains Mono, Consolas, Monaco, monospace"}}
                 "Executing script...")
          
          (nil? results)
          (d/div {:style {:color "#6a6a6a"
                         :font-style "italic"
                         :font-family "system-ui, -apple-system, sans-serif"}}
                 "No results yet. Press F5 or click Execute to run your script.")
          
          (seq results)
          (d/div
           (do
             (js/console.log "Rendering results:" (clj->js results))
             (for [[idx result] (map-indexed vector results)]
               (d/div {:key idx
                      :style {:margin-bottom "12px"}}
                      (do
                        (js/console.log (str "Result " idx ":") (clj->js result))
                        ($ result-item (if (map? result) 
                                         result
                                         {:type "unknown" :content (str result)})))))))
          
          :else
          (d/div {:style {:color "#6a6a6a"
                         :font-style "italic"}}
                 "Script executed successfully with no output.")))))