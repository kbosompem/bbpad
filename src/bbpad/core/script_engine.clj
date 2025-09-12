(ns bbpad.core.script-engine
  "Script execution engine for BBPad"
  (:require [clojure.string :as str]
            [clojure.pprint :as pprint]
            [clojure.data.json :as json]))

(defn format-result
  "Format a result based on its type for display"
  [result]
  (cond
    (nil? result)
    {:type :value :content "nil" :data-type "nil"}
    
    (string? result)
    {:type :value :content result :data-type "string"}
    
    (number? result)
    {:type :value :content (str result) :data-type "number"}
    
    (boolean? result)
    {:type :value :content (str result) :data-type "boolean"}
    
    (keyword? result)
    {:type :value :content (str result) :data-type "keyword"}
    
    (symbol? result)
    {:type :value :content (str result) :data-type "symbol"}
    
    (and (coll? result) (every? map? result) (seq result))
    {:type :table 
     :content (with-out-str (pprint/print-table result))
     :data result
     :data-type "table"}
    
    (map? result)
    {:type :map 
     :content (with-out-str (pprint/pprint result))
     :data result
     :data-type "map"}
    
    (vector? result)
    {:type :vector 
     :content (with-out-str (pprint/pprint result))
     :data result
     :data-type "vector"}
    
    (list? result)
    {:type :list 
     :content (with-out-str (pprint/pprint result))
     :data result
     :data-type "list"}
    
    (set? result)
    {:type :set 
     :content (with-out-str (pprint/pprint result))
     :data result
     :data-type "set"}
    
    :else
    {:type :value 
     :content (with-out-str (pprint/pprint result))
     :data result
     :data-type "unknown"}))

(defn execute-script 
  "Execute a Babashka script with given parameters"
  [code {:keys [parameters context]}]
  (let [start-time (System/currentTimeMillis)]
    (try
      (let [output (java.io.StringWriter.)]
        (binding [*out* output]
          (let [result (eval (read-string (str "(do " code ")")))
                execution-time (- (System/currentTimeMillis) start-time)
                output-str (str output)
                formatted-result (format-result result)]
            {:result formatted-result
             :output (when (not-empty output-str) output-str)
             :execution-time execution-time
             :success true})))
      (catch Exception e
        (let [execution-time (- (System/currentTimeMillis) start-time)]
          {:error (.getMessage e)
           :execution-time execution-time
           :success false})))))

(defn load-script-from-url
  "Load a script from a URL"
  [url]
  {:code "(println \"Hello from URL\")"
   :title "Example Script"
   :description "A sample script loaded from URL"})