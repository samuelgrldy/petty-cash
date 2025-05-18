(ns app.astro.store
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [app.utils :as u]
            [com.stuartsierra.component :as component]))

(defn- load-db [f]
  (if (.exists f) (edn/read-string (slurp f))
      {:meta {} :orders {} :indexes {}}))

(defn- save-db! [f db]
  ;; ensure parent directory exists before we spit
  (doto (.getParentFile f) (.mkdirs))
  (spit f (with-out-str (pr db))))

(defn save!
  "Persist current state of store to its EDN file."
  [{:keys [file db] :as store}]
  (save-db! file @db)
  store)

;; ---- index helpers -----------------------------------------------------
(defn- add-index [idx k id]
  (update idx k (fnil conj #{}) id))

(defn- update-indexes [indexes {:keys [id date status]}]
  (-> indexes
      ;; month only (kept for backward-compat)
      (add-index [:by-month (:month date)] id)
      ;; NEW: year-month index → [yyyy mm]
      (add-index [:by-year-month (:year date) (:month date)] id)
      (add-index [:by-status status] id)))

;; ---- merge -------------------------------------------------------------
(defn merge-orders [db orders]
  (reduce
    (fn [{:keys [orders indexes] :as acc} o]
      (if (contains? orders (:id o))
        acc
        (let [orders'  (assoc orders (:id o) o)
              indexes' (update-indexes indexes o)]
          (assoc acc :orders orders' :indexes indexes'))))
    db orders))

;; ---- component ---------------------------------------------------------
(defrecord OrderStore [path file db]         ;; db is now stored in the map
  component/Lifecycle
  (start [this]
    (u/info "Starting OrderStore" path)
    (let [file  (io/file path)
          state (atom (load-db file))]
      (assoc this :file file :db state)))
  (stop  [this] this))

(defn create-store-component
  ([]
   (create-store-component "data/orders.edn"))
  ([path]
   (map->OrderStore {:path path})))
