(ns ql.pg.jsonb
  (:require
   [clojure.string :as str]
   [ql.method :refer [to-sql conj-sql conj-param reduce-separated operator-args comma-separated]]))


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



;; @>	jsonb	Does the left JSON value contain the right JSON path/value entries at the top level?	'{"a":1, "b":2}'::jsonb @> '{"b":2}'::jsonb
;; <@	jsonb	Are the left JSON path/value entries contained at the top level within the right JSON value?	'{"b":2}'::jsonb <@ '{"a":1, "b":2}'::jsonb
;; ?	text	Does the string exist as a top-level key within the JSON value?	'{"a":1, "b":2}'::jsonb ? 'b'
;; ?|	text[]	Do any of these array strings exist as top-level keys?	'{"a":1, "b":2, "c":3}'::jsonb ?| array['b', 'c']
;; ?&	text[]	Do all of these array strings exist as top-level keys?	'["a", "b"]'::jsonb ?& array['a', 'b']
;; ||	jsonb	Concatenate two jsonb values into a new jsonb value	'["a", "b"]'::jsonb || '["c", "d"]'::jsonb

(defmethod to-sql :jsonb/||
  [acc args]
  (let [[_ a b] (operator-args args)]
    (-> acc
        (to-sql a)
        (conj-sql "||")
        (to-sql b))))

;; -	text	Delete key/value pair or string element from left operand. Key/value pairs are matched based on their key value.	'{"a": "b"}'::jsonb - 'a'
;; -	integer	Delete the array element with specified index (Negative integers count from the end). Throws an error if top level container is not an array.	'["a", "b"]'::jsonb - 1
;; -	text[]	Delete multiple key/value pairs or string elements from left operand. Key/value pairs are matched based on their key value.	'{"a": "b", "c": "d"}'::jsonb - '{a,c}'::text[]
;; #-	text[]	Delete the field or element with specified path (for JSON arrays, negative integers count from the end)	'["a", {"b":1}]'::jsonb #- '{1,b}'


;; to_json(anyelement)

;; to_jsonb(anyelement)

;; Returns the value as json or jsonb. Arrays and composites are converted (recursively) to arrays and objects; otherwise, if there is a cast from the type to json, the cast function will be used to perform the conversion; otherwise, a scalar value is produced. For any scalar type other than a number, a Boolean, or a null value, the text representation will be used, in such a fashion that it is a valid json or jsonb value.	to_json('Fred said "Hi."'::text)	"Fred said \"Hi.\""
;; array_to_json(anyarray [, pretty_bool])	Returns the array as a JSON array. A PostgreSQL multidimensional array becomes a JSON array of arrays. Line feeds will be added between dimension-1 elements if pretty_bool is true.	array_to_json('{{1,5},{99,100}}'::int[])	[[1,5],[99,100]]
;; row_to_json(record [, pretty_bool])	Returns the row as a JSON object. Line feeds will be added between level-1 elements if pretty_bool is true.	row_to_json(row(1,'foo'))	{"f1":1,"f2":"foo"}
;; json_build_array(VARIADIC "any")

;; jsonb_build_array(VARIADIC "any")

;; Builds a possibly-heterogeneously-typed JSON array out of a variadic argument list.	json_build_array(1,2,'3',4,5)	[1, 2, "3", 4, 5]
;; json_build_object(VARIADIC "any")

;; jsonb_build_object(VARIADIC "any")


;; jsonb_object(text[])
;; Builds a JSON object out of a text array. The array must have either exactly one dimension with an even number of members, in which case they are taken as alternating key/value pairs, or two dimensions such that each inner array has exactly two elements, which are taken as a key/value pair.	
;; json_object('{a, 1, b, "def", c, 3.5}')
;; json_object('{{a, 1},{b, "def"},{c, 3.5}}')

;; {"a": "1", "b": "def", "c": "3.5"}
;; json_object(keys text[], values text[])

;; jsonb_object(keys text[], values text[]) This form of json_object takes keys
;; and values pairwise from two separate arrays. In all other respects it is
;; identical to the one-argument form. json_object('{a, b}', '{1,2}')
;; {"a": "1", "b": "2"}

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
