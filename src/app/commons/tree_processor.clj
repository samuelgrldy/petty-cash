(ns app.commons.tree-processor
  (:require [app.utils :as u]))

;; Transforming flat data structure into tree and vice versa
;; Normally used for container-groups, skill-groups, and content-folders
(defn flatten-tree
  "Recursive function to flatten a tree into a flat data structure with children only at one level down,
   and children containing only their IDs. Returns a vector."
  ([data parent-id]
   (let [children (:children data)]
     (if (empty? children)
       [(assoc data :parent parent-id)]
       (let [new-data (assoc data :parent parent-id)]
         (vec (concat [new-data] (mapcat #(flatten-tree % (:_id data)) children)))))))
  ([data]
   (let [children (:children data)]
     (if (empty? children)
       [data]
       (let [new-data (dissoc data :children)]
         (vec (concat [new-data] (mapcat #(flatten-tree % (:_id data)) children))))))))

(defn tree->flat
  "Transforms a tree into a flat data structure with children only at one level down,
   and children containing only their IDs. Returns a vector."
  [data]
  (vec (mapcat #(flatten-tree %) data)))

(defn find-children
  "Find the children of a node in a flat data structure, sort by order"
  [flattened parent]
  (->> (filterv #(= (:parent %) parent) flattened)
       (sort-by :order)))

(defn unflatten
  [flattened item]
  (let [children (find-children flattened (:_id item))]
    (assoc item :children (mapv #(unflatten flattened %) children))))

(defn flat->tree
  "Transforms a flat data structure into a tree. Returns a vector."
  [data]
  (let [roots (find-children data nil)]
    (mapv #(unflatten data %) roots)))

(defn cg-paths
  "Finding paths to the outermost parents from a node, returns array of id"
  [data node]
  (vec
    (loop [cg-id (:_id node) node-data node res []]
      (let [parent-data (first (filter #(= (:_id %) (:parent node-data)) data))]
        (if parent-data
          (recur (:_id parent-data) parent-data (cons cg-id res))
          (cons cg-id res))))))

;;converting tree-to-vector
(defn find-item-by-id [flat-data id]
  (some #(when (= (:_id %) id) %) flat-data))

(defn find-items-by-ids [flat-data ids]
  (let [id-set (set ids)]
    (filter #(contains? id-set (:_id %)) flat-data)))

(defn tree-to-vector [node]
  (let [id (:_id node)
        children (:children node)]
    (if (empty? children)
      [id]
      (vec (cons id (mapv tree-to-vector children))))))

(defn custom-flat->tree
  "Transforms a list of flattened ids into a tree of its item."
  [flat ids]
  (let [filtered-items (find-items-by-ids flat ids)]
    (mapv #(unflatten flat %) filtered-items)))

;;TODO: refactor these function below
(defn tree->vectree
  "Transforms a list of flattened ids into a vector tree of ids.
   Returns a vector."
  [flat ids]
  (let [filtered-items (find-items-by-ids flat ids)
        filtered-tree (mapv #(unflatten flat %) filtered-items)]
    (mapv tree-to-vector filtered-tree)))

(defn collect-all-children
  "Collect all children and their children recursively, flattening the structure."
  [flat-data item]
  (let [children (find-children flat-data (:_id item))]
    (reduce
      (fn [acc child]
        (let [child-result (collect-all-children flat-data child)]
          (concat acc child-result)))
      [item]
      children)))

(defn find-parents-and-children
  "Given a vec of parent ids, returns all parents and their children in a flat structure."
  [flat-data ids]
  (reduce
    (fn [acc id]
      (if-let [parent (get-in flat-data [id])]
        (concat acc (collect-all-children flat-data parent))
        acc))
    []
    ids))

(defn flatten-tree-with-children
  "Flatten a tree into a flat structure where each node contains its children one level down."
  [node]
  (let [children (:children node)]
    (if (empty? children)
      [node]
      (let [current-node (assoc node :children (mapv #(dissoc % :children) children))]
        (vec (concat [current-node]
                     (mapcat flatten-tree-with-children children)))))))

(defn flatten-tree-exhaustive
  "Recursively flattens a tree structure into a vector where each node retains all its attributes and
   descendants are represented at their respective levels."
  [node]
  (let [children (:children node)]
    (if (empty? children)
      [node]
      (let [current-node (assoc node :children children)]
        (vec (concat [current-node]
                     (mapcat flatten-tree-exhaustive children)))))))

(defn tree->pseudo-flat
  "Transforms a tree structure into a pseudo flat structure where all levels are represented."
  [tree]
  (vec (mapcat flatten-tree-exhaustive tree)))


(defn vec-to-map-of-maps
  "Converts a vector of maps into a map of maps keyed by _id as strings."
  [vec-data]
  (into {} (map (juxt :_id identity) vec-data))
  #_(into {} (map (fn [m]
                  [(str (:_id m)) m])
                vec-data)))

;;=============================
;;buat mock
(defn keyword-to-string-keys
  "Converts map keys from keyword to string."
  [m]
  (into {} (map (fn [[k v]] [(name k) v]) m)))

;;gausah pake lagi
(defn find-parents-level
  "Given a map of sg-id-level and a vec of flat data, returns all parents and their children in a flat structure,
  each with the correct :program-level."
  [flat-data sg-id-level]
  (let [sg-id-level (keyword-to-string-keys sg-id-level)]   ;; Konversi dari keyword ke string
    (reduce
      (fn [acc [parent-id program-level]]
        (if-let [parent (find-item-by-id flat-data parent-id)]
          (let [children (collect-all-children flat-data parent)
                children-with-level (map #(assoc % :program-level program-level) children)]
            (concat acc children-with-level))
          acc))
      []
      sg-id-level)))