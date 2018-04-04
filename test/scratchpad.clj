(ns scratchpad
  (:require
   [ql.core :as ql]
   [testdb :as db]))




(comment
  (db/query ["select 1"])

  (db/execute " create table \"User\" (id text primary key, resource jsonb); ")

  (db/query ["select from user"])

  (db/query (ql/sql {:ql/type :ql/insert
                     :ql/table :user
                     :ql/value {:id "user-1" :resource {:name "Nicola"}}
                     :ql/returing :*}

                    {:format :jdbc})
            )





  )

