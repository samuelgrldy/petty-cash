(ns app.astro.analyze
  (:require [clojure.set :as set]))

;; ----- helpers ----------------------------------------------------------
(defn- all-orders
  "Return the store’s orders as a seq of order maps."
  [astro-store]
  (-> astro-store :db deref :orders vals))

(defn- date->vec [{:keys [date]}]
  (let [{:keys [year month day hour minute]} date]
    [year month day (or hour 0) (or minute 0)]))

(defn- valid-order?
  [{:keys [date]}]
  (every? some? [(:year date) (:month date)]))

(defn- ym-key
  [{:keys [date]}]
  [(:year date) (:month date)])

(defn orders-by-month
  "Return the vector of orders for the given year & month."
  [astro-store year month]
  (->> (all-orders astro-store)
       (filter valid-order?)
       (filter #(= [year month] (ym-key %)))
       (sort-by date->vec)
       vec))

(defn spend-by-month
  "Sum of :total-paid for the given month."
  [astro-store year month]
  (->> (orders-by-month astro-store year month)
       (map :total-paid)
       (reduce + 0)))

(defn items-by-month
  "Flattened seq of all line-items bought in the month."
  [astro-store year month]
  (mapcat :items (orders-by-month astro-store year month)))

(defn monthly-summary
  "Returns a map { [yyyy mm] {:orders n, :total-spent x} … } for every month
   present in the store."
  [astro-store]
  (->> (all-orders astro-store)
       (filter valid-order?)
       (group-by ym-key)
       (reduce-kv (fn [acc ym os]
                    (assoc acc ym {:orders-count (count os)
                                   :total-spent  (reduce + (map :total-paid os))}))
                  {})))

(comment

  ;testing and running here. lfgo

  (orders-by-month (:astro @user/system*) 2024 11)

  (spend-by-month (:astro @user/system*) 2025 5)

  )
