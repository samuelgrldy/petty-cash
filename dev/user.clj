(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh]]
            [app.system :as sys]
            [com.stuartsierra.component :as component]))

(defn dev
  []
  (require '[dev])
  (in-ns 'dev))

(defn gpt-play
  []
  (require '[gpt-play])
  (in-ns 'gpt-play))


