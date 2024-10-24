(ns app.plumbing.openai
  (:require
   [com.stuartsierra.component :as component]
   [app.utils :as u]
   [clj-http.client :as http]
   [cheshire.core :as json]))

(declare base-request generate models)

;; this is the component openai with basic setting of openai
;; returns the generator function

(defrecord Openai [openai-url openai-key]
  component/Lifecycle
  (start [this]
    (u/info "Setting up the openai component")
    (u/info "Openai URL: " openai-url)
    (assoc this
           :openai (fn [{:keys [model messages json-schema]}]
                     (u/info "Generating from openai")
                     (let [send-to-openai {:model      model
                                           :openai-url (str openai-url)
                                           :messages   messages
                                           :openai-key (str openai-key)}]
                       (if (nil? json-schema)
                         (generate send-to-openai)
                         (generate send-to-openai json-schema))))))
  (stop [this]
    (u/info "Openai stopped")
    this))

(defn create-openai-component
  "Openai component constructor"
  [{:keys [openai-url openai-key]}]
  (map->Openai {:openai-url openai-url
                :openai-key openai-key}))

(defn base-request
  [api-token]
  {:accept       :json
   :content-type :json
   :headers      {"Authorization" (str "Bearer " api-token)}})

(def models
  {:gpt-3 "gpt-3.5-turbo-0125"
   "gpt-3" "gpt-3.5-turbo-0125"
   :gpt-4 "gpt-4o"
   "gpt-4" "gpt-4o"
   :gpt-4o "gpt-4o-mini"
   "gpt-4o" "gpt-4o-mini"})

(defn generate
  "Just call this one to generate the response from openAI"
  ([{:keys [model openai-url messages openai-key] :as send-to-openai}]
   (u/info "Getting into generate function inside openai component")
   (let [data {:model           (models model)
               :messages        messages
               :response_format {:type "json_object"}
               :max_tokens      16000
               :temperature     0.21
               :n               1}]
     (u/pres data)
     (let [resp (try (->> data
                          (json/generate-string)
                          (assoc (base-request openai-key) :body)
                          (http/post openai-url))
                     (catch Exception e (u/error e)))]
       (u/pres resp)
       (let [resp1 (-> (:body resp)
                       (json/parse-string true))]
         (u/pres resp1)
         (-> (select-keys resp1 [:usage])
             (assoc :result (-> (get-in resp1 [:choices 0 :message :content])
                                (json/parse-string true))))))))

  ([{:keys [model openai-url messages openai-key] :as send-to-openai} json-schema]
   (u/info "Getting into generate function inside openai component")
   (let [data {:model           (models model)
               :messages        messages
               :response_format json-schema
               :max_tokens      16000
               :temperature     0.21
               :n               1}]
     (u/pres data)
     (let [resp (try (->> data
                          (json/generate-string)
                          (assoc (base-request openai-key) :body)
                          (http/post openai-url))
                     (catch Exception e (u/error e)))]
       (u/pres resp)
       (let [resp1  (-> (:body resp)
                        (json/parse-string true))]
         (u/pres resp1)
         (-> (select-keys resp1 [:usage])
             (assoc :result (-> (get-in resp1 [:choices 0 :message :content])
                                (json/parse-string true))))))))
  )