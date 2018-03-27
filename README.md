# ql 

## data ast for sql, aka honeysql rethink

[![Build Status](https://travis-ci.org/niquola/ql.svg?branch=master)](https://travis-ci.org/niquola/ql)

[![Clojars Project](https://img.shields.io/clojars/v/ql.svg)](https://clojars.org/ql)


`honeysql` is awesome idea, but....

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
(require '[ql.core :as ql])

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

## License

Copyright © 2018 niquola

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
