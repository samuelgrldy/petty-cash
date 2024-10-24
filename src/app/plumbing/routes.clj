(ns app.plumbing.routes
  (:require [app.commons.web :as web]
            [app.utils :as u]
            [monger.collection :as mc]
            [reitit.ring :as ring]))

(defn api-check
  [db request]
  {:status  200
   :headers {"Content-Type" "application/json"}
   :body    {:status  "ok"
             :message "API is running fine"}})

#_(defn api-check
  "Helper function for testing api"
  [db request]
  (do
    ;; (throw (IllegalArgumentException. "wow")) ;; debuging
    ;; (println "db : " (keys db))
    ;; (:db-zenleap :db-content :db-client)

    (let [db-zenleap (mc/exists? (:db-zenleap db) "user-problem-answers")
          db-zencore (mc/exists? (:db-content db) "playlists")
          db-client  (mc/exists? (:db-client db) "users")]
      (if (and db-zenleap db-zencore db-client)
        (do
          ;(print "API-CHECK : healthy")
          {:status  200
           :headers {"Content-Type" "application/json"}
           :body    {:status  "ok"
                     :message "API is running fine"}})
        (do
          ;(print "API-CHECK : RUSAK")
          {:status  503
           :headers {"Content-Type" "application/json"}
           :body    {:status  "ok"
                     :message "Something wrong with db"}})))))

#_(defn refresh-dbrefs
  [db request]
  (u/info "Refreshing dbrefs")
  (try
    (let [result (dbrefs/refresh-dbrefs db (:refs db))]
      {:status 200
       :body result})
    (catch Exception e
      (u/error "Error refreshing dbrefs:" (.getMessage e))
      {:status 500
       :body {:error "Internal server error"}})))

#_(defn api
  "APIs specifically for backsite needs"
  [db openai zenbrain midware]
  (u/info "Getting into backsite-api")
  ["/api"
   ["/v1"
    ["" {:get api-check}]
    ]])

#_(defn frontend-routes
  "Routes for frontend, for testing some stuffs only"
  [db openai midware]
  (fe/frontend-routes db openai midware))

;; (defn health-check []
;;   ["/health" {:get api-check}])

(defn create-routes
  "Creates the whole routes for the system"
  [db openai zenbrain]
  (ring/router
   [["/" {:get (partial api-check db)}]
    ;; (health-check)
    #_(api db openai zenbrain web/backware)  ; Changed from web/backware-pass to web/backware
    ;; ini frontend-routes buat testing ajah
    #_(frontend-routes db openai web/frontware-pass)]))
