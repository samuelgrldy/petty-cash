(ns app.commons.web
  (:require
   [app.utils :as u]
   [monger.collection :as mc]
   [java-time :as t]))

(defn auth
  ;; jangan pake ini, bikin function baru yg nge check header, authorization
  ;; u/read-token, samain dengan token yg ada di server
  "Authenticating user request"
  [db request]
  ;; I want to check the method, true if get and if post do the other things
  (if (= :get (:request-method request))
    true
    (let [user (get-in request [:body :cred])]
      (u/info "Authenticating user " (:email user))
      (when-let [db-user (mc/find-one-as-map (:db db) "creds" {:email (:email user)})]
        (mc/update-by-id (:db db)
                         "creds"
                         (:email db-user)
                         (assoc db-user :last-active (u/now)))
        (and (:approved db-user)
             (= (select-keys user [:email :token])
                (select-keys db-user [:email :token])))))))

(defn wrap-jwt-auth [handler]
  (fn [request]
    (if-let [token (get-in request [:headers "authorization"])]
      (if-let [claims (u/verify-token (second (re-find #"^Bearer (.+)$" token)))]
        (handler (assoc request :identity claims))
        {:status 401
         :body {:error "Invalid or expired token"}})
      {:status 401
       :body {:error "No authorization token provided"}})))

(defn backware
  "Create a json response out of a function, including JWT verification"
  ([fun db request]
   (u/info "=======================================================================")
   (u/info "URI : " (:uri request))
   ((wrap-jwt-auth
      (fn [req]
        (merge {:status 200
                :headers {"Content-Type" "application/json"}}
               (fun db req))))
    request))
  ([fun db openai request]
   (u/info "=======================================================================")
   (u/info "URI : " (:uri request))
   ((wrap-jwt-auth
      (fn [req]
        (merge {:status 200
                :headers {"Content-Type" "application/json"}}
               (fun db openai req))))
    request)))

;(defn backware
;  ;; harus nya ga pake auth, bikin function baru yg nge check header, authorization
;  ;; berarti di kirim dari frontend itu encrypted, di decrypt di backend
;  ;; check sama apa ngga
;  ;; passing kalo sama, return 401 kalo beda
;  "Create a json response out of a function"
;  ([fun db request]
;   (u/info "=======================================================================")
;   (u/info "URI : " (:uri request))
;   (if (auth db request)
;     (merge {:status  200
;             :headers {"Content-Type" "application/json"}}
;            (fun db request))
;     {:status  401
;      :headers {"Content-Type" "application/json"}
;      :body    {:status  "error"
;                :message "Failed to login, please relogin and refresh the app"}}))
;  ([fun db openai request]
;   (u/info "=======================================================================")
;   (u/info "URI : " (:uri request))
;   (if (auth db request)
;     (merge {:status  200
;             :headers {"Content-Type" "application/json"}}
;            (fun db openai request))
;     {:status  401
;      :headers {"Content-Type" "application/json"}
;      :body    {:status  "error"
;                :message "Failed to login, please relogin and refresh the app"}})))


(defn backware-pass ;; ini nge bypass, ini ngebuat jadi return nya json doang
  "Create a json response out of a function"
  ([fun db request]
   (u/info "=======================================================================")
   (u/info "URI : " (:uri request))
   (let [data (fun db request)]
     (merge {:status  200
             :headers {"Content-Type" "application/json"}}
            data)))
  ([fun db openai request]
   (u/info "=======================================================================")
   (u/info "URI : " (:uri request))
   (merge {:status  200
           :headers {"Content-Type" "application/json"}}
          (fun db openai request))))

(defn frontware-pass
  "Middleware for frontend routes"
  [fun db openai request]
  (u/info "=======================================================================")
  (u/info "URI : " (:uri request))
  (merge {:status  200
          :headers {"Content-Type" "text/html"}}
         (fun db openai request)))

;; Temporary using expiration date for user authorization
(defn frontware
  "Authenticating user bearer token"
  [fun db request]
  (u/info "=======================================================================")
  (u/info "URI : " (:uri request))
  (let [access-token (-> (get-in request [:headers "authorization"])
                         (subs 7))
        token-expired? (-> (u/read-token access-token)
                           (:expired)
                           (t/local-date-time)
                           (t/before? (t/local-date-time)))]
    (if token-expired?
      {:status  401
       :headers {"Content-Type" "application/json"}
       :body    {:status  "error"
                 :message "Access token expired"}}
      (merge {:status  200
              :headers {"Content-Type" "application/json"}}
             (fun db request)))))
