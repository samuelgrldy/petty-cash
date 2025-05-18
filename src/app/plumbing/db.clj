(ns app.plumbing.db
  (:require [com.stuartsierra.component :as component]
            [app.utils :as u]))

(declare clear-db)

;{:port        db-mongo-port
;                  :uri-content db-mongo-uri-content
;                  :uri-client  db-mongo-uri-client
;                  :uri-univ    db-mongo-uri-universal
;                  :db-content  db-mongo-content
;                  :db-client   db-mongo-client
;                  :db-univ     db-mongo-universal
;                  :quiet       db-mongo-quiet
;                  :debug       db-mongo-debug}


(defrecord Dbase []
  component/Lifecycle
  (start [this]
    (u/info "Starting stub DB component – no mongo")
    (assoc this :conn nil :db nil))
  (stop [this]
    (u/info "Stub DB stopped")
    this))
                   ;;                          (dbrefs/update-refs-client {:db db-client :refs updated-refs-client})
                   ;;                          future)
                   ;:refs               refs
                   ;; :refs-client        refs-client
                   })))
  (stop [this]
    (when-let [conn (:conn this)]
      (mg/disconnect conn))
    (u/info "Database stopped")
    (dissoc this :conn)
    (reset! (:scheduler-running? this) false)
    (u/info "Scheduler stopped")))

(defn create-database-component []
  (map->Dbase {}))

#_(defn clear-db
  "Dropping the database"
  [db]
  (mc/remove db "creds" {}))

;; read the app.schema so you know the structure







