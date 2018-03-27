(ns ql.core
  (:require [clojure.string :as str]
            [ql.method :refer [to-sql conj-sql conj-param reduce-separated operator-args]]
            [ql.select]
            [ql.pg.core]))



(defmethod to-sql :ql/param
  [acc expr]
  (let [v (if (vector? expr) (second expr) (:ql/value expr))]
    (-> acc
        (conj-sql "?")
        (conj-param v))))

(defmethod to-sql :ql/string
  [acc expr]
  (conj-sql acc (str "$str$" (:ql/value expr) "$str$")))

(defmethod to-sql :ql/=
  [acc expr]
  (let [[_ a b] (operator-args expr)]
    (-> acc
        (to-sql a)
        (conj-sql "=")
        (to-sql b))))

(defmethod to-sql :ql/<>
  [acc [_ a b]]
  (-> acc
      (to-sql a)
      (conj-sql "<>")
      (to-sql b)))

(defmethod to-sql :ql/*
  [acc _]
  (conj-sql acc "SELECT" "*"))

(defmethod to-sql java.lang.Long
  [acc expr]
  (conj-sql acc (str expr)))


(defmethod to-sql java.lang.String
  [acc expr]
  (if (= :honeysql (get-in acc [:opts :style]))
    (-> acc
       (conj-sql "?")
       (conj-param expr))
    (conj-sql acc (str "'" expr "'"))))

(defmethod to-sql nil
  [acc expr]
  (conj-sql acc "NULL"))

(defmethod to-sql clojure.lang.Keyword
  [acc expr]
  (conj-sql acc (name expr)))

(defn sql [expr & [opts]]
  (->
   {:sql [] :params [] :opts opts}
   (to-sql  (if (map? expr)
              (update expr :ql/type (fn [x] (if x x :ql/query)))
              expr))
   (update :sql (fn [x] (str/join " " x)))))
