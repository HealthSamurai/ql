# ql 

## data ast for sql, aka honeysql as base for dsl's on top of it

[![Build Status](https://travis-ci.org/niquola/ql.svg?branch=master)](https://travis-ci.org/niquola/ql)

[![Clojars Project](https://img.shields.io/clojars/v/ql.svg)](https://clojars.org/ql)


[honeysql](https://github.com/jkk/honeysql) is an awesome idea, but....

* composability - it should be easy compose expressions into sql query
* extendibility - to extend - just add one multi-method ql.method/to-sql
* pure functional implementation - sql generation as a tree reduction
* implicit params - manage params style jdbc, postgres, inline
* use namespaced keywords
* validation by clojure.spec
* prefer hash-map over vector (support both, where vector is just sugar)
* dsl's on top of it

## Usage

```clj
(require '[ql.core :as ql :refer [sql]])

(ql/sql 
 #:ql{:select {:name :u.name}
      :from  {:u :user}
      :where {:by-id [:ql/= :u.id [:ql/param 5]]}})

;;=> 
{
  :sql = "SELECT u.name AS name FROM u user WHERE /** by-id **/ ( u.id = ? )"
  :params = [ 5 ]
}

```

In the example above `:ql/type :ql/select` is omitted. For root node if no
`:ql/type` provided `:ql/select` is used by default.

Insert with json and string values example:

```clj
(sql
 #:ql{:type       :ql/insert
      :table_name :db_table_name
      :value      {:column_a {:ql/type :ql/jsonb
                              :key     [:some :values]}
                   :column_b "value-b"}
      :returning  :*})
;; =>
{:sql    "INSERT INTO db_table_name ( column_a , column_b ) VALUES ( $JSON${\"key\":[\"some\",\"values\"]}$JSON$ , 'value-b' ) RETURNING *"
 :params []
 :opts   nil}
```

## How it works

`ql` is a data-driven DSL, which converts tree structure into SQL string with
placeholders and vector of params for following usage with db engine.

Main building blocks are hash-maps with metainformation provided by qualified
keywords with `ql` namespace. Also, vectors are supported as a syntax sugar.

Examples:
```clj
(sql {:ql/type :ql/=
      :left    "str"
      :middle  "test"
      :right   123})
;; => {:sql "'str' = 123", :params [], :opts nil}
(sql [:ql/= "str" 123 "another test"])
;; => {:sql "'str' = 123", :params [], :opts nil}
```

As demonstrated in the example above language can contain data of arbitral type,
but this type must be acceptable by `to-sql` multhimethod.

```clj
(sql 123)
;; => {:sql "123", :params [], :opts nil}
(sql :keyword)
;; => {:sql "keyword", :params [], :opts nil}
(sql {:ql/type :ql/jsonb
      :key     :value})
;; => {:sql "$JSON${\"key\":\"value\"}$JSON$", :params [], :opts nil}
```

`to-sql` accepts two parameters `partial-result` and `value-to-parse`.
`partial-result` is a hash-map with two keys `:sql` and `:params`, which
represent current state of parsing (parts of sql string with placeholders and
vector of parameters respectevly).

Parsing process starts from `sql` function, which calls `to-sql` with empty
`partial-result` and root node parameters. For traversing tree structure kind of
dfs is used. On each step type of the node is determined based on the following
info:

- For hash-map `:ql/type` value
- For vector first element
- `type` function for other object

Node type is used to call proper `to-sql` method. It updates current
`partial-result` and calls `to-sql` for child nodes. Order is determined by
internal implementation of each `to-sql` method.

After traversal, tokens in `:sql` are joined using " " and sql string is ready
for use. Using `{:format :jdbc}` result will be converted in format suitable for
jdbc.

```clj
(sql [:ql/= "str" 123] {:style  :honeysql
                        :format :jdbc})
;; => ["? = 123" "str"]
```

More detailed information can be found in [these](./src/ql/core.cljc) [files](./src/ql/method.cljc).

## Development

```
source .env
docker-compose up -d
lein repl
```


## License

Copyright © 2018 niquola

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
