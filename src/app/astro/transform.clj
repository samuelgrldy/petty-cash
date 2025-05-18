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

(defn parse-order-date
  "Parse Astro order string, injecting year derived from invoice."
  [invoice s]
  (let [[d m _t] (str/split s #"[ ,]+")]
    {:raw   s
     :day   (Integer/parseInt d)
     :month (month->num m)
     :year  (invoice->year invoice)}))

;; item/order transform ---------------------------------------------------
(defn transform-item [m]
  (let [q  (Integer/parseInt (:order_item_product_quantity m))
        bp (Long/parseLong    (:order_item_product_price m))
        dp (Long/parseLong    (:order_item_product_discount_price m))]
    {:name           (:order_item_product_name m)
     :quantity       q
     :base-price     bp
     :discount-price dp
     :total-paid     (* q dp)}))

(defn transform-order [m]
  {:id         (:order_id m)
   :invoice    (:order_invoice m)
   :status     (if (= "Pesanan Dibatalkan" (:order_status m)) :cancelled :delivered)
   :date       (parse-order-date (:order_invoice m) (:order_date m))
   :total-paid (Long/parseLong (:order_total_price m))
   :items      (mapv transform-item (:order_line_items m))})
