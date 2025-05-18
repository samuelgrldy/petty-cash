(ns test-nembak
  (:require [clj-http.client :as client]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [java-time :as t]))

(def auth-token "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3NTUyNjYyMzEsImlzcyI6Imh0dHBzOi8vd3d3LmFzdHJvbmF1dHMuaWQiLCJyZWZyZXNoX2V4cGlyeSI6MTc1NTI2NjIzMSwic3ViIjoiMTIzMjIwNyIsInRlbXBvcmFyeV90b2tlbiI6ZmFsc2UsInVzZXJfcGVybWlzc2lvbnMiOiJBVVRIX0NVU1RPTUVSIiwidXNlcl90eXBlIjoiY3VzdG9tZXIiLCJ2ZW5kb3JfaWQiOjB9.kTLl7oHxoo0STEGOBjUwIHtnW4x2Ed3vAb5XpUUqSbKZG2l5vMc2LBDqrAANqIQueQgYZ6OE8YRb8L82uVnQgg") ;; Isi dengan token aslimu

(defn get-astro-orders
  "Mengambil riwayat order dari API Astro Groceries"
  [& {:keys [page-index page-size order-type]
      :or {page-index 0
           page-size 10
           order-type "instant"}}]
  (let [url "https://api.astronauts.id/api/order"
        headers {"Authorization" (str "Bearer " auth-token)
                 "X-Api-Version" "1.9.11"
                 "X-App-Version" "android-2.17.1"
                 "X-Device" "web-customer"
                 "X-Device-Id" "0291180c0cf595111a51a4561c02b72f301c1bf8288f341109e42e10ec81062c" ;; ID perangkatmu
                 "X-Device-Version" "2.17.2"
                 "User-Agent" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36"
                 "Content-Type" "application/json"
                 "Accept" "application/json, text/plain, */*"
                 "Origin" "https://www.astronauts.id"
                 "Referer" "https://www.astronauts.id/"}
        query-params {:pageIndex page-index
                      :pageSize page-size
                      :orderType order-type}]
    (try
      (let [response (client/get url {:headers headers
                                      :query-params query-params
                                      :as :json})]
        (:body response))
      (catch Exception e
        (println "Error:" (.getMessage e))
        nil))))

(comment

  (get-astro-orders :page-index 0 :page-size 20)

  )

(defn transform-order [raw-order]
  {:id (:order_id raw-order)
   :total-paid (Long/parseLong (:order_total_price raw-order))
   :order-status (:order_status raw-order)
   :order-date (:order_date raw-order)
   :items (:order_line_items raw-order)
   :metadata (dissoc raw-order
                     :order_id
                     :order_total_price
                     :order_status
                     :order_date
                     :order_line_items)})

(defn transform-item [raw-item]
  (let [base-price (Long/parseLong (:order_item_product_price raw-item))
        discount-price (Long/parseLong (:order_item_product_discount_price raw-item))
        quantity (Integer/parseInt (:order_item_product_quantity raw-item))
        discount-amount (- base-price discount-price)
        total-paid (* discount-price quantity)]
    {:id (:order_item_product_id raw-item)
     :name (:order_item_product_name raw-item)
     :quantity quantity
     :base-price base-price           ;; Harga asli per item
     :discount-price discount-price   ;; Harga setelah diskon per item
     :discount-amount discount-amount ;; Jumlah diskon per item
     :total-paid total-paid        ;; Total yang dibayar untuk item ini (quantity * discount-price)
     :metadata (dissoc raw-item
                       :order_item_product_name
                       :order_item_product_quantity
                       :order_item_product_price
                       :order_item_product_discount_price
                       :order_item_product_discount_quantity
                       :order_item_product_image
                       :modifiers
                       :ticket_detail
                       :order_item_product_inventory_discount_id
                       :product_modifier
                       :order_line_item_id)}))

(defn process-orders
  [raw-orders]
  (mapv (fn [order]
          (let [transformed-order (transform-order order)
                items (:items transformed-order)]
            (assoc transformed-order
                   :items (mapv transform-item items))))
        (:content raw-orders)))

(comment

  (def test-whole-data
    (get-astro-orders :page-index 0 :page-size 20))

  (def test-processed-data
    (process-orders test-whole-data))

  )

(defn fetch-all-orders
  "Mengambil semua order dari semua halaman yang tersedia"
  []
  (loop [all-orders []
         page 0]
    (println "Fetching page" page "...")
    (let [orders (get-astro-orders :page-index page)]
      (if (or (nil? orders)
              (empty? (get orders "content"))
              (get orders "last"))
        ;; Jika halaman terakhir atau error, kembalikan semua order yang telah dikumpulkan
        (if (nil? orders)
          all-orders
          (concat all-orders (get orders "content")))
        ;; Jika belum halaman terakhir, lanjutkan ke halaman berikutnya
        (recur (concat all-orders (get orders "content"))
               (inc page))))))

(defn save-orders-to-json
  "Menyimpan orders ke file JSON"
  [orders]
  (let [now (t/format "yyyyMMdd_HHmmss" (t/local-date-time))
        filename (str "astro_orders_" now ".json")]
    (with-open [writer (io/writer filename)]
      (json/encode orders writer {:pretty true}))
    (println "Berhasil menyimpan" (count orders) "order ke" filename)
    filename))

;(defn -main
;  "Fungsi utama untuk menjalankan aplikasi"
;  [& args]
;  (println "Mengambil data riwayat pembelian dari Astro Groceries...")
;  (let [orders (fetch-all-orders)]
;    (if (empty? orders)
;      (println "Tidak ada data yang berhasil diambil.")
;      (save-orders-to-json orders))))

;; Contoh penggunaan:
;; (-main)