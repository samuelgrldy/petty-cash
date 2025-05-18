(ns app.astro.pipeline
  (:require [app.astro.api :as api]
            [app.astro.transform :as tx]
            [app.astro.store :as store]
            [app.utils :as u]))

(defn sync!
  "Fetch new orders, merge them into the store, persist to disk."
  [astro-store]                            ; component value from (store)
  (u/info "Astro sync – fetching...")
  (let [existing (set (keys (:orders @(:db astro-store))))
        raw      (api/fetch-all)
        unseen   (remove #(existing (:order_id %)) raw)
        orders   (map tx/transform-order unseen)]
    (swap! (:db astro-store) store/merge-orders orders)
    (store/save! astro-store)
    (u/info "Astro sync done — added" (count orders) "orders")
    {:fetched (count raw) :added (count orders)}))
