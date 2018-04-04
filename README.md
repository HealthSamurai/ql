# ql

[![Build Status](https://travis-ci.org/niquola/ql.svg?branch=master)](https://travis-ci.org/niquola/ql)

[![Clojars Project](https://img.shields.io/clojars/v/ql.svg)](https://clojars.org/ql)


`honeysql` is awesome idea, but....

* composability
* extendibility
* pure fn implementation
* implicit params

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

## Dev

```
source .env
docker-compose up -d
start repl
```

## License

Copyright © 2018 niquola

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
