(ns app.utils
  (:require [buddy.core.hash :as buddy]
            [buddy.sign.jwt :as jwt]
            [cheshire.core :as json]
            [clj-uuid :as uuid]
            [clojure.edn :as edn]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as cs]
            [environ.core :refer [env]]
            [java-time.api :as t])
  (:import [java.sql Timestamp]
           [java.time Instant]
           java.util.Base64))


(defn now
  []
  (let [[date time] (-> (t/local-date-time)
                        (str)
                        (subs 0 19)
                        (cs/split #"T"))]
    (str date " " time)))

(defn freq-by
  "Like frequencies but with function"
  [f col]
  (->> (group-by f col)
       (map #(let [[k v] %] [k (count v)]))
       (into {})))

(defn num->per
  "Convert decimals into percentage string"
  [num]
  (str (subs (str (* 100 num)) 0 5) "%"))

(defn info [& body]
  (apply println "INFO :" body))

(defn error [& body]
  (apply println "ERROR :" body))

(defn warn [& body]
  (apply println "WARNING :" body))

(def pres clojure.pprint/pprint)

(defn let-pres
  [exprs]
  (pres exprs)
  exprs)

(defmacro pro-catch
  "Macro to report problem error"
  [message coder exprs]
  `(try ~exprs
        (catch Exception ~(gensym)
          (error ~message ~coder)
          (throw (Exception. ~message)))))

(defmacro no-throw
  "Macro to report error without exception"
  [message some-var exprs]
  `(try ~exprs
        (catch Exception ~(gensym)
          (error ~message ~some-var))))

(defmacro silent-try
  "Macro to report error without exception"
  [exprs]
  `(try ~exprs
        (catch Exception ~(gensym))))

(defn pro-rep
  "Reporting error"
  [message coder]
  (error message coder)
  (throw (Exception. message)))

(defn cslurp
  "Helper function to easily slurp and read-string a file"
  [fname]
  ((comp edn/read-string slurp) fname))

(defn cspit
  "Helper function to beautifully print clojure to file"
  [fname data]
  (->> data pprint with-out-str (spit fname)))

(defn cstr
  [data]
  (with-out-str (pprint data)))

(defn uuid
  "When given zero argument, it returns a uuid/v1, given one arguments, it returns
  a list of n uuids."
  ([]
   (cs/replace (str (uuid/v1)) #"-" ""))
  ([n]
   (repeatedly n uuid)))

;; (defn update-config-vals [map vals f]
;;   (reduce #(update-in % [%2] f) map vals))

(defn read-config
  "Reading the config, either intra-project or extra-project"
  []
  (cslurp "resources/config.edn"))

(defn update-config-vals-flat-format [map vals f]
  (reduce #(update-in % [%2] f) map vals))

(defn read-config-true-flat
  "Reading the config from environ, uses flat structure to ease production injection"
  []
  (let [config-keys     [:server-path
                         :server-port
                         :server-host

                         :openai-url
                         :openai-key

                         :db-mongo-uri
                         :db-mongo

                         :db-mongo-port
                         :db-mongo-quiet
                         :db-mongo-debug
                         ]
        to-be-read-keys [:server-port
                         :openai-key
                         :openai-url
                         :db-mongo-quiet
                         :db-mongo-port
                         :db-mongo-debug]]
    (-> env
        (select-keys config-keys)
        (update-config-vals-flat-format to-be-read-keys edn/read-string))))

(defn evaluate-tests
  "Return true if all test passed, else nil. Test must return boolean"
  [tests]
  (empty? (filter false? tests)))

(defn not-empty?
  [obj]
  (cond
    (string? obj) (->> (cs/trim obj) not-empty)
    (number? obj) (identity obj)
    (boolean? obj) (boolean obj)
    :else (not-empty obj)))

(defn empty-field-exist?
  [obj fields]
  (let [result (map #(not-empty? (% obj)) fields)]
    (not (empty? (filter nil? result)))))

(defn empty-fields
  [obj fields]
  (let [result (map #(not-empty? (% obj)) fields)]
    (->> (filter #(nil? (nth result %)) (range (count result)))
         (#(map (fn [x] (nth fields x)) %)))))

(defn indexOf
  "Find the first index of obj in a list"
  [_vec _x]
  (->> (map first
            (filter #(= (second %) _x)
                    (map-indexed vector _vec)))
       first))

(defn find-first
  [f coll]
  (first (filter f coll)))

(defn decode-payload
  "Decode the payload from jwt token"
  [token]
  (let [decoder #(String. (.decode (Base64/getDecoder) %))
        payload (decoder (second (cs/split token #"\.")))]
    (try
      (json/parse-string payload)
      (catch Exception e
        (pro-rep "Error decoding token" e)))))

(defn create-token
  "Encrypt data using jwt, returns encrypted token"
  [data]
  (jwt/encrypt
    data
    (buddy/sha256 (str (:server-secret (read-config-true-flat))))))

(defn read-token
  [token]
  (jwt/decrypt
    token
    (buddy/sha256 (str (:server-secret (read-config-true-flat))))))

(defn epoch-time
  "Return the current epoch time, s-delta is the second difference"
  ([]
   (epoch-time 0))
  ([s-delta]
   (let [time (t/instant)
         time (t/plus time (t/seconds s-delta))]
     (t/to-millis-from-epoch time))))

(defn verify-token
  "Verify the token"
  [token]
  (let [unsigned (try
                   (read-token token)
                   (catch Exception _
                     nil))
        exp (:exp unsigned)]
    (if (and exp (<= (epoch-time) exp))
      unsigned
      nil)))

(defn squuid
  "generate sequential uuid from:
  https://github.com/clojure-cookbook/clojure-cookbook/blob/50a961ece6db1eedcf04cd5284f76d6167755838/01_primitive-data/1-24_uuids.asciidoc"
  []
  (let [uuid (java.util.UUID/randomUUID)
        time (System/currentTimeMillis)
        secs (quot time 1000)
        lsb (.getLeastSignificantBits uuid)
        msb (.getMostSignificantBits uuid)
        timed-msb (bit-or (bit-shift-left secs 32)
                          (bit-and 0x00000000ffffffff msb))]
    (java.util.UUID. timed-msb lsb)))

(defmacro catch-error
  "Catch error and throw it as exception"
  [expr]
  (let [e (gensym)]
    `(try ~expr
          (catch Exception ~e
            (error ~e)
            (throw (Exception. ~e))))))

(defn epoch-to-timestamp
  "Convert epoch time to timestamp"
  [epoch]
  (-> epoch
      java.time.Instant/ofEpochMilli
      Timestamp/from))

(def roles
  {"learner" #{"learner"}
   "manager" #{"learner" "manager"}
   "admin" #{"learner" "manager" "admin"}})

(defn session-type->zenbrain
  [session-type]
  (get {"practice" "practice"
        "diagnostic-test" "diagnostic"
        "assessment" "assessment"}
       session-type))

(defn rename-keys [m key-map]
  (into {}
        (map (fn [[k v]]
               [(get key-map k k) v]) ;; Replace with new key or keep the old one
             m)))