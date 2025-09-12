(ns bbpad.ui.state.app
  (:require [bbpad.ui.api.client :as api]))

(defonce app-state (atom {:scripts []
                          :current-script nil
                          :execution-results nil
                          :executing? false}))

(defn init! []
  (println "BBPad UI initialized"))

(defn execute-script [code callback]
  (swap! app-state assoc :executing? true)
  (api/execute-script
   code
   (fn [result]
     (swap! app-state assoc 
            :executing? false
            :execution-results result)
     (when callback
       (callback result)))
   (fn [error]
     (swap! app-state assoc 
            :executing? false
            :execution-results [{:type :error 
                               :content (str error)}])
     (when callback
       (callback [{:type :error :content (str error)}])))))

(defn save-script [name code]
  (api/save-script
   {:name name :code code}
   (fn [result]
     (swap! app-state update :scripts conj result))
   (fn [error]
     (js/console.error "Failed to save script:" error))))

(defn load-scripts []
  (api/get-scripts
   (fn [scripts]
     (swap! app-state assoc :scripts scripts))
   (fn [error]
     (js/console.error "Failed to load scripts:" error))))