(ns ql.insert
  (:require [clojure.string :as str]
            [ql.method :refer [to-sql conj-sql conj-param reduce-separated clear-ql-keys]]))


(defmethod to-sql :ql/value
  [acc expr]
  (let [ks (keys (clear-ql-keys expr))
        acc (conj-sql acc "(")
        acc (reduce-separated "," (fn [acc k] (conj-sql acc (name k)))  acc ks)
        acc (conj-sql acc ")" "VALUES" "(")
        acc (reduce-separated
             ","
             (fn [acc k] (to-sql acc (get expr k))) acc ks)

        acc (conj-sql acc ")")]
    acc))

(defmethod to-sql :ql/insert
  [acc expr]
  (reduce (fn [acc {prep :token k :key}]
            (if-let [v (get expr k)]
              (cond->
                  acc
                prep (conj-sql prep)
                true (to-sql  (if (and (map? v) (not (:ql/type v))) (assoc v :ql/type k) v)))
              acc))
          acc [{:key :ql/table_name
                :token "INSERT INTO"}
               {:key :ql/values}
               {:key :ql/value}
               {:key :ql/returning
                :token "RETURNING"}]))

(defmethod to-sql :ql/truncate
  [acc expr]
  (reduce (fn [acc [prep k]]
            (if-let [v (get expr k)]
              (cond->
                  acc
                prep (conj-sql prep)
                true (to-sql  (if (and (map? v) (not (:ql/type v))) (assoc v :ql/type k) v)))
              acc))
          acc [["TRUNCATE" :ql/table_name]]))

(defmethod to-sql :ql/drop-table
  [acc expr]
  (reduce (fn [acc [prep k]]
            (if-let [v (get expr k)]
              (cond->
                  acc
                prep (conj-sql prep)
                true (to-sql  (if (and (map? v) (not (:ql/type v))) (assoc v :ql/type k) v)))
              acc))
          acc [[(str "DROP TABLE" (when (:ql/if-exists expr) " IF EXISTS")) :ql/table_name]]))

(defmethod to-sql :ql/column
  [acc expr]
  (apply conj-sql acc 
         (->> [(:ql/column-type expr) (when (:ql/primary-key expr) "PRIMARY KEY")]
              (filterv identity)
              (mapv name))))

(defmethod to-sql :ql/columns
  [acc expr]
  (let [cols (sort-by #(second (:ql/weight %)) (clear-ql-keys expr))]
    (-> 
     (reduce-separated
      ","
      (fn [acc [k v]]
        (-> acc
            (conj-sql (name k))
            (to-sql (update v :ql/type (fn [x] (if x x :ql/column))))))
      (conj-sql acc "(")
      cols)
     (conj-sql ")"))))

(defmethod to-sql :ql/create-table
  [acc expr]
  (reduce (fn [acc [prep k]]
            (if-let [v (get expr k)]
              (cond->
                  acc
                prep (conj-sql prep)
                true (to-sql  (if (and (map? v) (not (:ql/type v))) (assoc v :ql/type k) v)))
              acc))
          acc [[(str "CREATE TABLE" (when (:ql/if-not-exists expr) " IF NOT EXISTS")) :ql/table_name]
               [nil :ql/columns]]))
