(ns app.astro.transform
  (:require [clojure.string :as str]))

;; month map + parser -----------------------------------------------------
(def ^:private month->num
  {"Jan" 1 "Feb" 2 "Mar" 3 "Apr" 4 "May" 5 "Jun" 6
   "Jul" 7 "Aug" 8 "Sep" 9 "Oct" 10 "Nov" 11 "Dec" 12})

(defn- invoice->year [invoice]
  ;; "INV/AS/20240517/…" -> 2024
  (try
    (-> invoice
        (clojure.string/split #"/")
        (nth 2)
        (subs 0 4)
        Integer/parseInt)
    (catch Exception _ nil)))
  
(defn- parse-money
  "Turn a price string such as \"17.39\", \"17.390\", \"17,390\" or \"17390\"
   into a long (integer Rupiah).  Removes every non-digit char before parsing."
  [s]
  (-> (or s "0")
      (str/replace #"[^0-9]" "")
      (Long/parseLong)))

(def ^:private delivered-str "Pesanan Sudah Diterima")

(defn- normalize-status
  "Turn Astro status string into our keyword.
   Guarantees :delivered whenever the constant `delivered-str`
   appears, explicit :cancelled for the known cancel string,
   otherwise slug-ify whatever we get."
  [s]
  (cond
    (= s delivered-str)         :delivered
    (= s "Pesanan Dibatalkan")  :cancelled
    :else (-> s
              str/lower-case
              (str/replace #"[^a-z0-9]+" "-")
              keyword)))

(defn parse-order-date
  "Parse Astro order string, injecting year derived from invoice and extracting hour/minute if present."
  [invoice s]
  (let [[d m t] (str/split s #"[ ,]+")
        [hh mm] (when t
                  (map #(Integer/parseInt %)
                       (str/split t #":")))]
    {:raw    s
     :day    (Integer/parseInt d)
     :month  (month->num m)
     :year   (invoice->year invoice)
     :hour   (or hh 0)
     :minute (or mm 0)}))

;; item/order transform ---------------------------------------------------
(defn transform-item [m]
  (let [q  (Integer/parseInt (:order_item_product_quantity m))
        bp (parse-money      (:order_item_product_price m))
        dp (parse-money      (:order_item_product_discount_price m))]
    {:name           (:order_item_product_name m)
     :quantity       q
     :base-price     bp
     :discount-price dp
     :total-paid     (* q dp)}))

(defn transform-order [m]
  {:id         (:order_id m)
   :invoice    (:order_invoice m)
   :status     (normalize-status (:order_status m))
   :date       (parse-order-date (:order_invoice m) (:order_date m))
   :total-paid (parse-money (:order_total_price m))
   :items      (mapv transform-item (:order_line_items m))})
