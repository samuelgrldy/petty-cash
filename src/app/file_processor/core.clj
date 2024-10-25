(ns app.file-processor.core
  (:require
    [app.utils :as u]
    [clj-http.client :as http]
    [cheshire.core :as json]
    [clojure.string :as cs]
    [clojure.java.io :as io]
    [clojure.data.codec.base64 :as codec]))


(defn encode-image [file-path]
  (let [file (io/file file-path)
        bytes (byte-array (.length file))]
    (with-open [in (io/input-stream file)]
      (.read in bytes))
    (String. (codec/encode bytes))))

(defn image->codec
  [file-path]
  (str "data:image/jpeg;base64," (encode-image file-path)))

(comment

  (def test-encode
    (image->codec "resources/raw/WhatsApp Image 2024-10-24 at 12.02.27.jpeg"))

  (req-to-openai (-> @dev/dev-system :openai)
                 "gpt-4o-mini"
                 test-encode)




  )

(defn vision-message
  [url]
  [{:role    "system"
    :content "I am a AI agent specialized in reading bills and provide information based on the user request.
    By default, I will return a json object with keys item that contains array of object for each item names and its total price,
    and keys date that contains the date of that bills with following format \"MM_DD_YYYY\". If the bills is unreadable on those intended keys, i will return null instead. Unless stated otherwise, I will return those two keys."}
   {:role    "user"
    :content [{:type "text"
               :text "Just give me the date of the bill"}
              {:type      "image_url"
               :image_url {:url url
                           }}]}])

(defn req-to-openai
  "Send the request to OpenAI"
  [openai model url]
  (let [gen-fn (:openai openai)
        result (-> (gen-fn {:model    model
                            :messages (vision-message url)})
                   (try (catch Exception e
                          (u/pres e)))
                   u/let-pres)]))


