(ns ql.core
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [clojure.string :as str]
            [ql.select]
            [ql.pretty-sql :as pretty-sql]
            [ql.insert]
            [ql.pg.core]
            [ql.method :refer [conj-param conj-sql operator-args to-sql]]))

(def default-opts (merge ql.select/default-opts))

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
    (conj-param acc expr)
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


(defn sql [expr & [opts]]
  (let [res (->
             {:sql [] :pretty-sql [] :params [] :opts (merge default-opts opts)}
             (to-sql  (if (map? expr)
                        (update expr :ql/type (fn [x] (if x x :ql/select)))
                        expr))
             (update :sql (fn [x] (str/join " " x))))]
     (case (:format opts)
       :jdbc  (into [(:sql res)] (:params res))
       :pretty (assoc res :sql (pretty-sql/make-pretty-sql (:pretty-sql res)))
       res)))

