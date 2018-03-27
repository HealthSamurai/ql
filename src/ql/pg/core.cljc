(ns ql.pg.core
  (:require
   [clojure.string :as str]
   [ql.method :refer [to-sql conj-sql conj-param reduce-separated]]))


(defn comma-separated [ks]
  (->> ks
       (mapv (fn [x] (if (keyword? x) (name x) (str x))))
       (str/join ",")))

;; (comma-separated [:a :b 0])

(defn operator-args [opts]
  (if (map? opts)
    [(:ql/type opts)
     (or (:left opts) (get opts 0))
     (or (:right opts) (get opts 1))]
    opts))


;; ->	int	Get JSON array element (indexed from zero, negative integers count from the end)	'[{"a":"foo"},{"b":"bar"},{"c":"baz"}]'::json->2	{"c":"baz"}
;; ->	text	Get JSON object field by key	'{"a": {"b":"foo"}}'::json->'a'	{"b":"foo"}
(defmethod to-sql :jsonb/->
  [acc args]
  (let [[_ col k] (operator-args args)]
    (-> acc
        (to-sql col)
        (conj-sql (str "->'" (name k) "'")))))

;; ->>	int	Get JSON array element as text	'[1,2,3]'::json->>2	3
;; ->>	text	Get JSON object field as text	'{"a":1,"b":2}'::json->>'b'	2

(defmethod to-sql :jsonb/->>
  [acc args]
  (let [[_ col k] (operator-args args)]
    (-> acc
        (to-sql col)
        (conj-sql (str "->>'" (name k) "'")))))

;; #>	text[]	Get JSON object at specified path	'{"a": {"b":{"c": "foo"}}}'::json#>'{a,b}'	{"c": "foo"}

(defmethod to-sql :jsonb/#>
  [acc args]
  (let [[_ col ks] (operator-args args)]
    (-> acc
        (to-sql col)
        (conj-sql (str "#>'{" (comma-separated ks)  "}'")))))

;; #>>	text[]	Get JSON object at specified path as text	'{"a":[1,2,3],"b":[4,5,6]}'::json#>>'{a,2}'	3
(defmethod to-sql :jsonb/#>>
  [acc args]
  (let [[_ col ks] (operator-args args)]
    (-> acc
        (to-sql col)
        (conj-sql (str "#>>'{" (comma-separated ks)  "}'")))))


(defmethod to-sql :jsonb/merge
  [acc args]
  (let [[_ a b] (operator-args args)]
    (-> acc
        (to-sql a)
        (conj-sql "||")
        (to-sql b))))

(defmethod to-sql :jsonb/build-object
  [acc obj]
  (let [strip-nulls? (:jsonb/strip-nulls obj)
        acc (if strip-nulls? (conj-sql acc "jsonb_strip_nulls(") acc)
        acc (-> acc (conj-sql "jsonb_build_object("))]
    (->
     (reduce-separated
      ","
      (fn [acc [k v]]
        (-> acc
            (to-sql (if (keyword? k) (name k) k))
            (conj-sql ",")
            (to-sql v)))
      acc (dissoc obj :ql/type :jsonb/strip-nulls))
     (conj-sql (if strip-nulls? "))" ")")))))
