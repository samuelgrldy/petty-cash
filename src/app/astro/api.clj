(ns app.astro.api
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [environ.core :refer [env]]
            [app.utils :as u]))

;; --- helpers ------------------------------------------------------------
(defn ^:private auth-headers []
  (let [token  (env :astro-token)
        dev-id (env :astro-device-id)]
    (when-not (and token dev-id)
      (u/pro-rep "Missing ASTRO env" {:token token :dev-id dev-id}))
    {"Authorization"    (str "Bearer " token)
     "X-Api-Version"    (or (env :astro-api-version)   "1.9.11")
     "X-App-Version"    (or (env :astro-app-version)   "android-2.17.1")
     "X-Device"         "web-customer"
     "X-Device-Id"      dev-id
     "X-Device-Version" (or (env :astro-device-version) "2.17.2")
     "User-Agent"       "astro-clj/0.1"
     "Accept"           "application/json"}))

;; --- public -------------------------------------------------------------
(defn fetch-page
  [{:keys [page-size page-index] :or {page-size 25 page-index 0}}]
  (let [req {:url          "https://api.astronauts.id/api/order"
             :query-params {:pageIndex page-index
                            :pageSize  page-size
                            :orderType "instant"}
             :headers      (auth-headers)
             :as           :json}]
    (:body (http/get (:url req) (dissoc req :url)))))

(defn fetch-all
  "Return lazy seq of **raw** order maps."
  ([]
   (letfn [(step [idx]
             (lazy-seq
               (let [{:keys [content last]} (fetch-page {:page-index idx})]
                 (concat content (when-not last (step (inc idx)))))))]
     (step 0))))
