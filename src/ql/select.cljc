(ns ql.select
  (:require [clojure.string :as str]
            [ql.method :refer [to-sql conj-sql conj-param reduce-separated clear-ql-keys]]))



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
         true (conj-sql "AS" (cond (keyword? k) (name k) :else k)))))
   acc
   (dissoc expr :ql/type)))

(defmethod to-sql :ql/from
  [acc expr]
  (reduce-separated
   ","
   (fn [acc [k v]]
     (-> acc
         (to-sql v)
         (conj-sql (name k))))
   acc (dissoc expr :ql/type)))

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



(defmethod to-sql :ql/join
  [acc {tp :ql/join-type rel :ql/rel on :ql/on a :ql/alias :as expr}]
  (cond-> acc
      true (conj-sql "\n")
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
     (-> acc 
         (to-sql (-> v
                     (assoc :ql/alias k)
                     (update :ql/type (fn [x] (if x x :ql/join)))))))
   acc (dissoc expr :ql/type)))


(defmethod to-sql :ql/list
  [acc expr]
  (let [xs (if (map? expr)
             (->> expr
                  (ql.method/clear-ql-keys)
                  (sort-by first)
                  (mapv second))
             (rest expr))]
    (reduce-separated "," to-sql acc xs)))

(defmethod to-sql :ql/with
  [acc expr]
  (reduce-separated
   ","
   (fn [acc [k v]]
     (-> acc
         (conj-sql (name k) "AS")
         (assoc-in [:opts :nested] true)
         (to-sql (-> v
                     (update :ql/type (fn [x] (if x x :ql/select)))
                     (dissoc :ql/weight)))
         (conj-sql "\n")))
   acc
   (->>
    (clear-ql-keys expr)
    (sort-by :ql/weight))))

(defn parens-if-nested [acc f]
  (let [n (get-in acc [:opts :nested])]
    (cond-> acc
      n (conj-sql "(")
      n (assoc-in [:opts :nested]  false)
      true (f)
      n (conj-sql ")"))))

(defmethod to-sql :ql/select
  [acc expr]
  (parens-if-nested
   acc
   (fn [acc]
     (reduce (fn [acc {k :key tk :token tp :default-type opts :opts}]
               (if-let [v (get expr k)]
                 (cond-> acc
                   opts (update :opts merge opts)
                   tk   (conj-sql tk)
                   true (to-sql (cond
                                  (and (map? v) (not (:ql/type v)) tp)
                                  (assoc v :ql/type tp)
                                  :else v)))
                 acc))
             acc [{:key          :ql/with
                   :token        "WITH"
                   :default-type :ql/with}
                  {:key          :ql/select
                   :token        "SELECT"
                   :default-type :ql/projection}
                  {:key          :ql/from
                   :token        "FROM"
                   :opts         {:nested true}
                   :default-type :ql/from}
                  {:key          :ql/where
                   :token        "WHERE"
                   :default-type :ql/predicate}
                  {:key          :ql/joins
                   :default-type :ql/joins}
                  {:key          :ql/group-by
                   :token        "GROUP BY"
                   :default-type :ql/projection}
                  {:key          :ql/order-by
                   :token        "ORDER BY"
                   :default-type :ql/list}
                  {:key          :ql/limit
                   :token        "LIMIT"
                   :default-type :ql/param}
                  {:key          :ql/offset
                   :token        "OFFSET"
                   :default-type :ql/param}]))))

(defmethod to-sql :ql/limit
  [acc expr]
  (if-let [v (:ql/value expr)]
    (-> (conj-sql acc "LIMIT")
        (to-sql v))
    acc))

