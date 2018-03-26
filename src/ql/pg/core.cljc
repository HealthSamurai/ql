(ns ql.pg.core
  (:require [ql.method :refer [to-sql conj-sql conj-param reduce-separated]]))

(defmethod to-sql :jsonb/build-object
  [acc obj]
  (let [acc (-> acc
                (conj-sql "jsonb_build_object("))]
    (->
     (reduce-separated
      ","
      (fn [acc [k v]]
        (-> acc
            (to-sql (if (keyword? k) (name k) k))
            (conj-sql ",")
            (to-sql v)))
      acc (dissoc obj :ql/type))
     (conj-sql ")"))))

(defmethod to-sql :jsonb/merge
  [acc [_ a b]]
  (-> acc
      (to-sql a)
      (conj-sql "||")
      (to-sql b)))

(defmethod to-sql :jsonb/->
  [acc [_ col k]]
  (-> acc
      (to-sql col)
      (conj-sql (str "->'" (name k) "'"))))



