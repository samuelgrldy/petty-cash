(ns app.astro.pipeline
  (:require [app.astro.api :as api]
            [app.astro.transform :as tx]
            [app.astro.store :as store]
            [app.utils :as u]))

(defn- distinct-by
  "Return a lazy seq with only the first element for every key f."
  [f coll]
  (let [seen (volatile! #{})]
    (filter (fn [x]
              (let [k (f x)]
                (when-not (@seen k)
                  (vswap! seen conj k)
                  true)))
            coll)))

(defn sync!
  "Fetch new orders, merge them into the store, persist to disk."
  [astro-store]                            ; component value from (store)
  (u/info "Astro sync – fetching...")
  (let [existing (set (keys (:orders @(:db astro-store))))
        raw      (api/fetch-all)
        unseen   (remove #(existing (:order_id %)) raw)
        unique   (distinct-by :order_id unseen)   ; <— new
        orders   (map tx/transform-order unique)]
    (swap! (:db astro-store) store/merge-orders orders)
    (store/save! astro-store)
    (u/info "Astro sync done — added" (count orders) "orders")
    {:fetched (count raw) :added (count orders)}))
