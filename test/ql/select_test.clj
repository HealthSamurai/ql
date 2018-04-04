(ns ql.select-test
  (:require
   [ql.core :as ql]
   [ql.select :as sut]
   [clojure.test :refer :all]
   [matcho.core :as matcho]))



(deftest test-select 
  (ql/sql {:ql/type :ql/projection
           :alias :u.column})

  (matcho/match
   (ql/sql
    {;; node type
     :ql/type :ql/select
     ;; selection
     :ql/select {:alias :u.column}
     ;; form {alias tbl | expression} 
     :ql/from {:u :user}
     ;; named conditions
     :ql/where {:by-id {:ql/type :ql/= :left :u.id :right 5}}
     :ql/order-by [:ql/list :u.name]
     ;; :ql/group-by [:ql/group-by :name]
     :ql/limit 1
     :ql/offset 10})
   {:sql
    "SELECT u.column AS alias FROM user u WHERE /** by-id **/ ( u.id = 5 ) ORDER BY u.name LIMIT 1 OFFSET 10",
    :params []})


  (matcho/match
   (ql/sql
    {:ql/type :ql/select
     :ql/select :*
     :ql/from :user
     :ql/where [:ql/= :u.id 1]
     :ql/limit 10
     :ql/offset 20}
    )
   ))

