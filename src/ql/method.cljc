(ns ql.method)

(defn dispatch-sql [x]
  (cond (map? x) (get x :ql/type)
        (vector? x) (first x)
        :else (type x)))

(defmulti to-sql (fn [{sql :sql params :params} x] (dispatch-sql x)))

(defn conj-sql [acc & sql]
  (update acc :sql (fn [x] (apply conj x sql))))

(defn conj-param [acc v]
  (update acc :params conj v))

(defn reduce-separated [sep f acc coll]
  (loop [acc acc
         [x & xs] coll]
    (if (nil? xs)
      (f acc x)
      (recur (update (f acc x) :sql conj sep) xs))))

