(ns voipbridge.yaml
  (:require [clj-yaml.core :as yaml]))

(def ^:dynamic *yml-cfg* (atom {}))

(defn override-yml
  "Convert the list of keywords to a dotted string then attempt to find that property in the Java system properties"
  [ path ]
  (let [ key (apply str (interpose "." (map #(name %) path))) ]
    (System/getProperty key)))

(defn init-yml
  "Load the yaml file and produce a map of configuration values"
  [ file-name ]
  (swap! *yml-cfg* conj (yaml/parse-string (slurp file-name))))


(defn get-cfg-value
  "Lookup the configuration value by key(s), first checking to see if there were any command line overrides"
  [ path & def-val]
  (let [ override (override-yml path)]
    (if (nil? override)
      (get-in (deref *yml-cfg* ) path (first def-val))
      override)))
