(ns app.commons.search
  (:require
    [app.utils :refer :all]
    [monger.collection :as mc]
    [clojure.string :as cs]
    [clojure.set :as cset]))

(defn process-string
  "Process a string to get a vector of words"
  [st]
  (->> (-> (cs/lower-case st)
           (cs/trim)
           (cs/split #"\s+"))
       (filterv #(> (count %) 2))
       set))

(defn count-matches
  "Count the number of matches in a string"
  [st1 st2]
  (let [st1-words (process-string st1)
        st2-words (process-string st2)]
    (count (cset/intersection st1-words st2-words))))






