(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh]]))

(defn dev
  []
  (require '[dev])
  (in-ns 'dev))

(defn gpt-play
  []
  (require '[gpt-play])
  (in-ns 'gpt-play))


