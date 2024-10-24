(ns app.plumbing.db
  (:require
    [com.stuartsierra.component :as component]
    [monger.collection :as mc]
    [app.utils :as u]
    [monger.core :as mg]))

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


(defrecord Dbase [db-mongo-config]
  component/Lifecycle
  (start [this]
    (u/info "Starting the database component")
    #_(u/pres db-mongo-config)
    (let [
          ;conn (mg/connect db-mongo-config)
          ;db (mg/get-db conn (:db db-mongo-config))
          ;refs (dbrefs/create-refs)
          scheduler-running? (atom true)
          ;updated-refs (dbrefs/prep-refs {:db-content db-content :db-client db-client :refs refs})
          ;; updated-refs-client       (dbrefs/prep-refs {:db db-client :refs refs-client})
          ]
      (u/info "Starting the database and the dbref")
      (merge this {:conn nil
                   :db nil
                   :scheduler-running? scheduler-running?
                   ;:scheduler          (->> scheduler-running?
                   ;                         (dbrefs/update-refs {:db-content db-content :db-client db-client :refs updated-refs})
                   ;                         future)
                   ;;; :scheduler-client   (->> scheduler-running?
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

(defn create-database-component [db-mongo-config]
  (map->Dbase {:db-mongo-config db-mongo-config}))

(defn clear-db
  "Dropping the database"
  [db]
  (mc/remove db "creds" {}))

;; read the app.schema so you know the structure







