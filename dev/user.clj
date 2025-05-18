(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh]]
            [app.system :as sys]
            [com.stuartsierra.component :as component]
            [app.astro.pipeline :as pipe]
            [app.astro.analyze :as az]))

(defonce system* (atom nil))

(defn go []
  (reset! system* (component/start (sys/create-system)))
  :started)

(defn reset []
  (when @system* (swap! system* component/stop))
  (refresh :after 'user/go))

;; quick handle to the order store
(defn store [] (:astro @system*))

;; REPL helper: get orders for year-month from the order store
(defn ym [y m]
  (az/orders-by-month (store) y m))

(defonce system* (atom nil))

(defn go []
  (reset! system* (component/start (sys/create-system)))
  :started)

(defn reset []
  (when @system* (swap! system* component/stop))
  (refresh :after 'user/go))

;; quick handle to the order store
(defn store [] (:astro @system*))

(defn dev
  []
  (require '[dev])
  (in-ns 'dev))

(defn gpt-play
  []
  (require '[gpt-play])
  (in-ns 'gpt-play))

(defn fetch! []
  (pipe/sync! (store)))


