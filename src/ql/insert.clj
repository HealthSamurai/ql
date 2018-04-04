(ns ql.insert
  (:require [clojure.string :as str]
            [ql.method :refer [to-sql conj-sql conj-param reduce-separated]]))


(defmethod to-sql :ql/value
  [acc expr]
  (let [ks (ql.method/)]
    (reduce (fn [acc k]
              (if-let [v (get expr k)]
                (to-sql acc (if (and (map? v) (not (:ql/type v)))
                              (assoc v :ql/type k)
                              v))
                acc))
            acc [:ql/table_name :ql/values])))

(defmethod to-sql :ql/insert
  [acc expr]
  (let [acc (conj-sql acc "INSERT INTO")]
    (reduce (fn [acc k]
              (if-let [v (get expr k)]
                (to-sql acc (if (and (map? v) (not (:ql/type v)))
                              (assoc v :ql/type k)
                              v))
                acc))
            acc [:ql/table_name :ql/values])))
