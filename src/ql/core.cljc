(ns ql.core
  (:require [clojure.string :as str]
            [ql.method :refer [to-sql conj-sql conj-param reduce-separated operator-args
                               clear-ql-keys only-ql-keys]]
            [ql.select]
            [ql.insert]
            [ql.pg.core]))

(defmethod to-sql :ql/param
  [acc expr]
  (let [v (if (vector? expr) (second expr) (:ql/value expr))]
    (conj-param acc v)))

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


(defmethod to-sql :ql/ident
  [acc args]
  ;; TODO: do escaping
  (conj-sql acc
            (str "\""
                 (name (if (vector? args)
                         (second args )
                         (:ql/value args)))
                 "\"")))

(defmethod to-sql :ql/with
  [acc expr]
  (let [acc (conj-sql acc "WITH")
        acc (reduce-separated
             ","
             (fn [acc [k v]]
               (-> acc
                   (conj-sql (name k) "AS" "(")
                   (to-sql (-> v
                               (update :ql/type (fn [x] (if x x :ql/select)))
                               (dissoc :ql/weight)))
                   (conj-sql ")\n")))
             acc
             (->>
              (clear-ql-keys expr)
              (sort-by :ql/weight)))]

    (to-sql acc (assoc (only-ql-keys expr) :ql/type :ql/select))))

(defn sql [expr & [opts]]
  (let [res (->
             {:sql [] :params [] :opts opts}
             (to-sql  (if (map? expr)
                        (update expr :ql/type (fn [x] (if x x :ql/select)))
                        expr))
             (update :sql (fn [x] (str/join " " x))))]
    (if (= :jdbc (:format opts))
      (into [(:sql res)] (:params res))
      res)))
