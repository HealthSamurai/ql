(ns ql.select
  (:require [clojure.string :as str]
            [ql.method :refer [to-sql conj-sql conj-param reduce-separated]]))



;; [ WITH [ RECURSIVE ] with_query [, ...] ]
;; SELECT [ ALL | DISTINCT [ ON ( expression [, ...] ) ] ]
;;     [ * | expression [ [ AS ] output_name ] [, ...] ]
;;     [ FROM from_item [, ...] ]
;;     [ WHERE condition ]
;;     [ GROUP BY grouping_element [, ...] ]
;;     [ HAVING condition [, ...] ]
;;     [ WINDOW window_name AS ( window_definition ) [, ...] ]
;;     [ { UNION | INTERSECT | EXCEPT } [ ALL | DISTINCT ] select ]
;;     [ ORDER BY expression [ ASC | DESC | USING operator ] [ NULLS { FIRST | LAST } ] [, ...] ]
;;     [ LIMIT { count | ALL } ]
;;     [ OFFSET start [ ROW | ROWS ] ]
;;     [ FETCH { FIRST | NEXT } [ count ] { ROW | ROWS } ONLY ]
;;     [ FOR { UPDATE | NO KEY UPDATE | SHARE | KEY SHARE } [ OF table_name [, ...] ] [ NOWAIT | SKIP LOCKED ] [...] ]

;; where from_item can be one of:

;;     [ ONLY ] table_name [ * ] [ [ AS ] alias [ ( column_alias [, ...] ) ] ]
;;                 [ TABLESAMPLE sampling_method ( argument [, ...] ) [ REPEATABLE ( seed ) ] ]
;;     [ LATERAL ] ( select ) [ AS ] alias [ ( column_alias [, ...] ) ]
;;     with_query_name [ [ AS ] alias [ ( column_alias [, ...] ) ] ]
;;     [ LATERAL ] function_name ( [ argument [, ...] ] )
;;                 [ WITH ORDINALITY ] [ [ AS ] alias [ ( column_alias [, ...] ) ] ]
;;     [ LATERAL ] function_name ( [ argument [, ...] ] ) [ AS ] alias ( column_definition [, ...] )
;;     [ LATERAL ] function_name ( [ argument [, ...] ] ) AS ( column_definition [, ...] )
;;     [ LATERAL ] ROWS FROM( function_name ( [ argument [, ...] ] ) [ AS ( column_definition [, ...] ) ] [, ...] )
;;                 [ WITH ORDINALITY ] [ [ AS ] alias [ ( column_alias [, ...] ) ] ]
;;     from_item [ NATURAL ] join_type from_item [ ON join_condition | USING ( join_column [, ...] ) ]

;; and grouping_element can be one of:

;;     ( )
;;     expression
;;     ( expression [, ...] )
;;     ROLLUP ( { expression | ( expression [, ...] ) } [, ...] )
;;     CUBE ( { expression | ( expression [, ...] ) } [, ...] )
;;     GROUPING SETS ( grouping_element [, ...] )

;; and with_query is:

;;     with_query_name [ ( column_name [, ...] ) ] AS ( select | values | insert | update | delete )

;; TABLE [ ONLY ] table_name [ * ]

(defmethod to-sql :ql/select
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
     (-> acc
         (to-sql v)
         (conj-sql (name k))))
   (conj-sql acc "FROM") (dissoc expr :ql/type)))

(defmethod to-sql :ql/predicate
  [acc expr]
  (cond
    (map? expr)
    (reduce-separated
     (or (:ql/comp expr) "AND")
     (fn [acc [k v]]
       (-> acc
           (conj-sql "/**" (name k) "**/" "(")
           (to-sql v)
           (conj-sql ")")))
     acc
     (dissoc expr :ql/type :ql/comp))

    (vector? expr)
    (to-sql expr)))

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
      true (to-sql (if (map? on)
                          (update on :ql/type (fn [x] (if x x :ql/predicate)))
                          on))))

(defmethod to-sql :ql/joins
  [acc expr]
  (reduce
   (fn [acc [k v]]
     (to-sql acc (-> v
                      (assoc :ql/alias k)
                      (update :ql/type (fn [x] (if x x :ql/join))))))
   acc (dissoc expr :ql/type)))


(defmethod to-sql :ql/query
  [acc expr]
  (reduce (fn [acc k]
            (if-let [v (get expr k)]
              (to-sql acc (if (and (map? v) (not (:ql/type v)))
                            (assoc v :ql/type k)
                             v))
              acc))
          acc [:ql/select
               :ql/from
               :ql/where
               :ql/joins
               :ql/order-by
               :ql/limit
               :ql/offset]))

(defmethod to-sql :ql/limit
  [acc expr]
  (if-let [v (:ql/value expr)]
    (-> (conj-sql acc "LIMIT")
        (to-sql v))
    acc))

(defmethod to-sql :ql/order-by
  [acc expr]
  (assert (map? expr) ":ql/order-by should be hash-map")
  (let [acc (conj-sql acc "ORDER" "BY")]
    (reduce (fn [acc [i x]]
              (to-sql acc x))
            acc (->> 
                 (dissoc expr :ql/type)
                 (filter (fn [[x _]] (integer? x)))
                 (sort-by first)))))


(defmethod to-sql :ql/offset
  [acc expr]
  (if-let [v (:ql/value expr)]
    (-> (conj-sql acc "OFFSET")
        (to-sql v))
    acc))
