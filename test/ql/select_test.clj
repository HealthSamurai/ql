(ns ql.select-test
  (:require
   [ql.core :as ql]
   [ql.select :as sut]
   [clojure.test :refer :all]
   [matcho.core :as matcho]))



(deftest test-select 
  (matcho/match
   (ql/sql
    {:ql/type :ql/query
     :ql/select {:alias :u.column}
     ;; form {alias tbl | expression} 
     :ql/from {:u :user}
     ;; named conditions
     :ql/where {:by-id {:ql/type :ql/= :left :u.id :right 5}}
     :ql/order-by {0 :u.name}
     ;; :ql/group-by {:name :name}  ???
     :ql/limit {:ql/value 1}
     :ql/offset {:ql/value 10}})

   {:sql "SELECT u.column AS alias FROM user u WHERE /** by-id **/ ( u.id = 5 ) ORDER BY u.name LIMIT 1 OFFSET 10",
    :params []}))

