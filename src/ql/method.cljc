(ns ql.method
  (:require [clojure.string :as str]
            [ql.pretty-sql :as pretty-sql]))

(defn dispatch-sql [x]
  (cond (map? x) (get x :ql/type)
        (vector? x) (first x)
        :else (type x)))

(defn simple-type? [x]
  (not (or (vector? x) (map? x))))

(defmulti to-sql (fn [{sql :sql params :params} x] (dispatch-sql x)))

(defn cast-to-sql-string [x]
  (if (simple-type? x)
    (-> (to-sql {} x)
        :sql
        first)
    (str x)))

(defn conj-sql [acc & sql]
  (let [sql        (flatten sql)
        plain-sql  (filter (complement pretty-sql/pretty-operations) sql)
        pretty-sql sql]
    (-> acc
        (update :sql (fn [x] (apply conj x plain-sql)))
        (update :pretty-sql (fn [x] (apply conj x pretty-sql))))))

(defn conj-param [acc v]
  (if (= :inline (get-in acc [:opts :format]))
    (conj-sql acc (cast-to-sql-string v))
    (-> acc
        (conj-sql "?")
        (update :params conj v))))

(defn reduce-separated [sep f acc coll]
  (loop [acc acc
         [x & xs] coll]
    (if (nil? xs)
      (f acc x)
      (recur (conj-sql (f acc x) sep) xs))))

(defn comma-separated [ks]
  (->> ks
       (mapv (fn [x] (if (keyword? x) (name x) (str x))))
       (str/join ",")))

;; (comma-separated [:a :b 0])

(defn operator-args [opts]
  (if (map? opts)
    [(:ql/type opts)
     (or (:left opts) (get opts 0))
     (or (:right opts) (get opts 1))]
    opts))

(namespace :ql/ups)

(defn clear-ql-keys [m]
  (reduce (fn [m [k v]]
            (if (= "ql" (namespace k))
              m (assoc m k v))) {} m))


(defn only-ql-keys [m]
  (reduce (fn [m [k v]]
            (if (= "ql" (namespace k))
              (assoc m k v) m)) {} m))

(defn add-clause
  "Add a new clause to reduce order for specific ql-type,
  can be used for extending existing to-sql implementation"
  [opts ql-type action clause-key new-clause]
  (update-in opts [:reduce-order ql-type]
             (fn [a] (reduce #(into %1 (if (= clause-key (:key %2))
                                         (case action
                                           :before [new-clause %2]
                                           :after  [%2 new-clause])
                                         [%2]))
                             []
                             a))))

(only-ql-keys {:ql/a 1 :k 2})
(clear-ql-keys {:ql/a 1 :k 2})
