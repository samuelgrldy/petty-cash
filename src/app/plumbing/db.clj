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

(defn create-database-component []
  (map->Dbase {}))


;; read the app.schema so you know the structure







