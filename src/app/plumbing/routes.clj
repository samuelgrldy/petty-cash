(ns app.plumbing.routes
  (:require [app.utils :as u]
            [monger.collection :as mc]
            [reitit.ring :as ring]))

(defn api-check
  [db request]
  {:status  200
   :headers {"Content-Type" "application/json"}
   :body    {:status  "ok"
             :message "API is running fine"}})


(defn create-routes
  "Creates the whole routes for the system"
  [db openai zenbrain]
  (ring/router
   [["/" {:get (partial api-check db)}]]))
