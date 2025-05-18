(ns app.plumbing.handler
  (:require
   [com.stuartsierra.component :as component]
   [app.plumbing.routes :as routes]
   [reitit.ring :as ring]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
   [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.cookies :refer [wrap-cookies]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.middleware.stacktrace :refer [wrap-stacktrace]]
   [jumblerg.middleware.cors :as jcors]))

(defn create-handler [astro openai]
  (-> (routes/create-routes @(:db astro) openai)
      (ring/ring-handler)
      (jcors/wrap-cors #".*")
      wrap-params
      (wrap-json-body {:keywords? true :bigdecimals? true})
      wrap-cookies
      wrap-session
      wrap-json-response
      wrap-stacktrace
      ;; https://github.com/steffan-westcott/clj-otel/blob/master/doc/guides.adoc#work-with-http-client-and-server-spans
      ;; looks like wrap-route is used when having more spesific use case
      ;; trace-http/wrap-route
      ;; (trace-http/wrap-server-span {:create-span? false})
      ;; false, we use agent for creating span
      ;; trace-http/wrap-exception-event
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))))

(defrecord Handler [astro openai]
  component/Lifecycle
  (start [this]
    (assoc this :handler (create-handler astro openai)))
  (stop [this]
    this))

(defn create-handler-component []
  (map->Handler {}))
