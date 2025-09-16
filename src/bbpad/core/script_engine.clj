(ns bbpad.core.script-engine
  "Script execution engine for BBPad"
  (:require [clojure.string :as str]
            [clojure.pprint :as pprint]
            [clojure.data.json :as json]
            [bbpad.db.app-storage :as storage]
            [bbpad.connections :as connections]))

(defn normalize-result
  "Normalize result data, converting special types to regular Clojure data"
  [result]
  (try
    (cond
      ;; Known primitive types - pass through
      (nil? result) nil
      (string? result) result
      (number? result) result
      (boolean? result) result
      (keyword? result) result
      (symbol? result) result

      ;; Collections - recursively normalize
      (map? result)
      (into {} (map (fn [[k v]] [(normalize-result k) (normalize-result v)]) result))

      (vector? result)
      (mapv normalize-result result)

      (set? result)
      (into #{} (map normalize-result result))

      (seq? result)
      (map normalize-result result)

      ;; Unknown type - convert to string (handles Transit, etc.)
      :else
      (str result))
    (catch Exception _
      ;; If anything fails, just convert to string
      (str result))))

(defn format-result
  "Format a result based on its type for display"
  [result]
  (let [normalized (normalize-result result)]
    (cond
      (nil? normalized)
      {:type :value :content "nil" :data-type "nil"}

      (string? normalized)
      {:type :value :content normalized :data-type "string"}

      (number? normalized)
      {:type :value :content (str normalized) :data-type "number"}

      (boolean? normalized)
      {:type :value :content (str normalized) :data-type "boolean"}

      (keyword? normalized)
      {:type :value :content (str normalized) :data-type "keyword"}

      (symbol? normalized)
      {:type :value :content (str normalized) :data-type "symbol"}

      (and (coll? normalized) (every? map? normalized) (seq normalized))
      {:type :table
       :content (with-out-str (pprint/print-table normalized))
       :data normalized
       :data-type "table"}

      (map? normalized)
      {:type :map
       :content (with-out-str (pprint/pprint normalized))
       :data normalized
       :data-type "map"}

      (vector? normalized)
      {:type :vector
       :content (with-out-str (pprint/pprint normalized))
       :data normalized
       :data-type "vector"}

      (list? normalized)
      {:type :list
       :content (with-out-str (pprint/pprint normalized))
       :data normalized
       :data-type "list"}

      (set? normalized)
      {:type :set
       :content (with-out-str (pprint/pprint normalized))
       :data normalized
       :data-type "set"}

      :else
      {:type :value
       :content (with-out-str (pprint/pprint normalized))
       :data normalized
       :data-type "unknown"})))

(defn execute-script
  "Execute a Babashka script with given parameters"
  [code {:keys [parameters context with-connections?]}]
  (let [start-time (System/currentTimeMillis)]
    (try
      (let [output (java.io.StringWriter.)
            ;; Load saved connections if requested
            saved-connections (when with-connections?
                               (try
                                 (into {}
                                       (map (fn [conn]
                                              [(:id conn) conn])
                                            (storage/list-connections)))
                                 (catch Exception _ {})))]
        (binding [*out* output
                  ;; Make connections available to scripts
                  connections/*connections* (or saved-connections {})]
          ;; Prepend connection helpers to make them available
          (let [enhanced-code (if with-connections?
                               (str "(require '[bbpad.connections :as conn])\n" code)
                               code)
                result (eval (read-string (str "(do " enhanced-code ")")))
                execution-time (- (System/currentTimeMillis) start-time)
                output-str (str output)
                ;; Try to format result, but if it fails (e.g., Transit values),
                ;; just convert to string
                formatted-result (try
                                   (format-result result)
                                   (catch Exception _
                                     {:type :value
                                      :content (str result)
                                      :data-type "datalevin-result"}))]
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