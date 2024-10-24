(ns dev
  (:require
    [com.stuartsierra.component :as component]
    [app.system :as system]
    [clojure.tools.namespace.repl :refer [refresh]]
    [app.utils :refer :all]
    [cheshire.core :as json]
    [clj-http.client :as http]))

(defonce dev-system (atom nil))

;; create a function that reads config.edn and then get the port
;; looks like only for logging
(defn url
  "Get the port from the config.edn file"
  [path]
  (let [port (-> (read-config)
                 (get-in [:server :port]))]
    (str "http://localhost:" port "/api" path)))

;;===== SYSTEM RELATED FUNCTIONS ======

;; Create a function to restart the system

(defn start
  "Starting the webapp"
  []
  (->> (system/create-system)
       (component/start-system)
       (reset! dev-system))
  :system-started)

(defn stop []
  (swap! dev-system component/stop-system))

(defn restart
  []
  (do (stop)
      (print "Restarting the system in 2 seconds... ")
      (Thread/sleep 100)
      (println "plus/minus 5 minutes.")
      (refresh)
      (info "Abis refreshing nih hehe")
      (Thread/sleep 100)
      (start)))

(defn reset [] ;; better restart, start executed exactly after refresh, wipe ./target directory if you're  getting issues
  (stop)
  (refresh :after 'dev/start))

(comment
  ;; if db is not seeded with user, it needs to do this
  (app.plumbing.db/init-admin (:dbase @dev-system) "[insert password]")
  (app.plumbing.db/init-trash (:dbase @dev-system))

  )
