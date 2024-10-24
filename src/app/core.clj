(ns app.core
  (:require
    [app.system :as system]
    [com.stuartsierra.component :as component]
    [app.utils]
    [immutant.util :as log])
  (:gen-class))

(defonce system (atom nil))

(defn start
  "Starting the webapp"
  []
  (->> (system/create-system)
       (component/start-system)
       (reset! system)))

(defn -main
  "starts service"
  [& args]
  (app.utils/read-config-true-flat)
  (log/set-log-level! :ERROR)
  (start))
