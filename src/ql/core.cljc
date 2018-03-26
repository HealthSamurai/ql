(ns ql.core
  (:require [clojure.string :as str]
            [ql.method :refer [to-sql conj-sql conj-param reduce-separated]]
            [ql.pg.core]))


(defmethod to-sql :ql/projection
  [acc expr]
  (reduce-separated
   ","
   (fn [acc [k v]]
     (let [complex? (or (vector? v) (map? v))]
       (cond-> acc
         complex? (conj-sql "(")
         true (to-sql v)
         complex? (conj-sql ")")
         true (conj-sql "AS" (name k)))))
   (conj-sql acc "SELECT") (dissoc expr :ql/type)))

(defmethod to-sql :ql/from
  [acc expr]
  (reduce-separated
   ","
   (fn [acc [k v]]
     (conj-sql acc (name k) (name v)))
   (conj-sql acc "FROM") (dissoc expr :ql/type)))

(defmethod to-sql :ql/predicate
  [acc expr]
  (reduce-separated
   (or (:ql/comp expr) "AND")
   (fn [acc [k v]]
     (-> acc
         (conj-sql "/**" (name k) "**/" "(")
         (to-sql v)
         (conj-sql ")")))
   acc
   (dissoc expr :ql/type :ql/comp)))

(defmethod to-sql :ql/where
  [acc expr]
  (->
   (conj-sql acc "WHERE")
   (to-sql (assoc expr :ql/type :ql/predicate))))

(defmethod to-sql :ql/join
  [acc {tp :ql/join-type rel :ql/rel on :ql/on a :ql/alias :as expr}]
  (cond-> acc
      tp (conj-sql tp)
      true (conj-sql "JOIN")
      true (to-sql rel)
      true (conj-sql (name a) "ON")
      true (to-sql (update on :ql/type (fn [x] (if x x :ql/predicate))))))

(defmethod to-sql :ql/joins
  [acc expr]
  (reduce
   (fn [acc [k v]]
     (to-sql acc (-> v
                      (assoc :ql/alias k)
                      (update :ql/type (fn [x] (if x x :ql/join))))))
   acc (dissoc expr :ql/type)))


(defmethod to-sql :ql/select
  [acc expr]
  (reduce (fn [acc [k def-type]]
            (if-let [v (get expr k)]
              (to-sql acc (if (and (map? v) (not (:ql/type v)))
                             (assoc v :ql/type def-type)
                             v))
              acc))
          acc [[:select :ql/projection]
               [:from :ql/from]
               [:where :ql/where]
               [:joins :ql/joins]
               [:limit :ql/limit]
               [:offset :ql/offset]]))

(defmethod to-sql :ql/limit
  [acc expr]
  (if-let [v (:ql/value expr)]
    (-> (conj-sql acc "LIMIT")
        (to-sql v))
    acc))

(defmethod to-sql :ql/param
  [acc expr]
  (-> acc
      (conj-sql "?")
      (conj-param (:ql/value expr))))

(defmethod to-sql :ql/string
  [acc expr]
  (conj-sql acc (str "$str$" (:ql/value expr) "$str$")))

(defmethod to-sql :ql/=
  [acc [_ a b]]
  (-> acc
      (to-sql a)
      (conj-sql "=")
      (to-sql b)))

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
  (conj-sql acc (str "'" expr "'")))

(defmethod to-sql nil
  [acc expr]
  (conj-sql acc "NULL"))

(defmethod to-sql clojure.lang.Keyword
  [acc expr]
  (conj-sql acc (name expr)))

(defn sql [expr & [opts]]
  (->
   {:sql [] :params []}
   (to-sql  (update expr :ql/type (fn [x] (if x x :ql/select))))
   (update :sql (fn [x] (str/join " " x)))))
