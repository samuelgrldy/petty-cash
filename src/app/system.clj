(ns app.system
  (:require [app.plumbing.db :as db]
            [app.plumbing.handler :as http]
            [app.plumbing.openai :as openai]
            [app.astro.store :as astro]
            [app.plumbing.server :as immut]
            [app.utils :as u]
            [com.stuartsierra.component :as component]))

(defn create-system
  "It creates a system, and return the system, but not started yet"
  []
  (let [{:keys [server-path
                server-port
                server-host

                openai-url
                openai-key

                db-mongo-uri
                db-mongo

                db-mongo-port
                db-mongo-quiet
                db-mongo-debug
                ]} (u/read-config-true-flat)
        server {:port server-port :path server-path :host server-host}
        db-mongo {:port  db-mongo-port
                  :uri   db-mongo-uri
                  :db    db-mongo
                  :quiet db-mongo-quiet
                  :debug db-mongo-debug}
        other-config {:openai-url openai-url
                      :openai-key openai-key}]
    (u/info "Preparing the system")
    (component/system-map
      :openai (openai/create-openai-component other-config)
      :dbase (db/create-database-component)
      :server (component/using (immut/create-server-component server) [:handler])
      :handler (component/using (http/create-handler-component) [:dbase :openai])
      :order-store (astro/create-store-component)))
