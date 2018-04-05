(ns scratchpad-test
  (:require
   [ql.core :as ql]
   [testdb :as db]
   [clojure.test :refer :all]
   [matcho.core :as matcho]))


(deftest scratchpad
  (db/query ["select 1"])

  (db/query ["select from user"])


  (db/execute (ql/sql {:ql/type :ql/drop-table
                       :ql/if-exists {}
                       :ql/table_name [:ql/ident :User]}
                      {:format :jdbc}))

  (db/execute (ql/sql {:ql/type :ql/create-table
                       :ql/table_name [:ql/ident :User]
                       :ql/columns {:id {:ql/primary-key true
                                         :ql/weight 0
                                         :ql/column-type :text}
                                    :resource {:ql/column-type :jsonb
                                               :ql/weight 0}}}
                      {:format :jdbc}))

  (db/execute (ql/sql {:ql/type :ql/truncate
                       :ql/table_name [:ql/ident :User]}
                      {:format :jdbc}))

  (db/query (ql/sql {:ql/type :ql/insert
                     :ql/table_name {:ql/type :ql/ident
                                     :ql/value :User}
                     :ql/value {:id "user-1" :resource {:ql/type :ql/jsonb :name "Nicola"}}
                     :ql/returning :*}

                    {:format :jdbc}))

  #_(db/query (ql/sql {:ql/type :ql/insert
                     :ql/table_name {:ql/type :ql/ident
                                     :ql/value :User}
                     :ql/value {:id "user-1" :resource {:ql/type :ql/jsonb
                                                        :email "nicola@health-samurai.io"
                                                        :name "Nicola"}}
                     :ql/on-conflict {:??? :TODO} 
                     :ql/returning :*}

                    {:format :jdbc}))

  (matcho/match
   (db/query (ql/sql {:ql/type :ql/select
                      :ql/select {:resource :u.resource
                                  :id :u.id}
                      :ql/from {:u {:ql/type :ql/ident
                                    :ql/value :User}}}

                     {:format :jdbc}))
   [{:id #(not (nil? %))
     :resource {:name "Nicola"}}])


  )

