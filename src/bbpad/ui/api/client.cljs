(ns bbpad.ui.api.client
  (:require [ajax.core :as ajax]))

(def api-base-url "http://localhost:8080/api")

(defn execute-script [code on-success on-error]
  (ajax/POST (str api-base-url "/execute")
            {:params {:code code}
             :format :json
             :response-format :json
             :keywords? true
             :handler (fn [response]
                       (js/console.log "API Response:" (clj->js response))
                       (let [results (if (:success response)
                                       (let [result-items []
                                             result (:result response)]
                                         (-> result-items
                                             (cond-> (:output response)
                                               (conj {:type "output" :content (:output response)}))
                                             (cond-> result
                                               (conj (assoc result 
                                                           :type (or (:type result) "value")
                                                           :content (or (:content result) (str result))
                                                           :data (or (:data result) result)
                                                           :data-type (or (:data-type result) "unknown"))))))
                                       [{:type "error" :content (or (:error response) "Unknown error")}])]
                         (js/console.log "Processed results:" (clj->js results))
                         (on-success results)))
             :error-handler (fn [error]
                             (on-error (str "Execution failed: " 
                                          (or (get-in error [:response :message])
                                              (:status-text error)
                                              "Unknown error"))))}))

(defn save-script [script on-success on-error]
  (ajax/POST (str api-base-url "/scripts")
            {:params script
             :format :json
             :response-format :json
             :keywords? true
             :handler on-success
             :error-handler on-error}))

(defn get-scripts [on-success on-error]
  (ajax/GET (str api-base-url "/scripts")
           {:response-format :json
            :keywords? true
            :handler on-success
            :error-handler on-error}))

(defn get-script [id on-success on-error]
  (ajax/GET (str api-base-url "/scripts/" id)
           {:response-format :json
            :keywords? true
            :handler on-success
            :error-handler on-error}))

(defn delete-script [id on-success on-error]
  (ajax/DELETE (str api-base-url "/scripts/" id)
              {:handler on-success
               :error-handler on-error}))