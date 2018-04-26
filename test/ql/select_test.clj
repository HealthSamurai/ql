(ns ql.select-test
  (:require [clojure.test :refer :all]
            [matcho.core :as matcho]
            [ql.core :as ql]
            ql.method))


(defmethod ql.method/to-sql :mssql/options
  [acc expr]
  (ql.method/reduce-separated
   ","
   (fn [acc [k v]]
     (-> acc
         (ql.method/conj-sql (name k) "=")
         (ql.method/to-sql v)))
   acc (dissoc expr :ql/type)))

;; (ql/sql {:ql/type :mssql/options
;;          :a 1})

(deftest test-select 

  (matcho/match
   (ql/sql
    {;; node type
     :ql/type     :ql/select
     ;; selection
     :ql/select   {:alias :u.column}
     ;; form {alias tbl | expression} 
     :ql/from     {:u :user}
     ;; named conditions
     :ql/where    {:by-id {:ql/type :ql/= :left :u.id :right 5}}
     :ql/order-by [:ql/list :u.name]
     ;; :ql/group-by [:ql/group-by :name]
     :ql/limit    1
     :ql/offset   10})
   {:sql
    "SELECT u.column AS alias FROM user u WHERE /** by-id **/ ( u.id = 5 ) ORDER BY u.name LIMIT 1 OFFSET 10",
    :params []})

  (matcho/match
   (ql/sql {:ql/with   {:_user  #:ql {:select :*
                                      :from   :user
                                      :where  [:ql/= :id [:ql/param 5]]}
                        :_group {:ql/select :g.*
                                 :ql/from   {:u :user}
                                 :ql/joins  {:g {:ql/rel :group
                                                 :ql/on  [:ql/= :g.user_id :u.id]}}}}
            :ql/select {:name :u.name :group :g.name} 
            :ql/from   {:u :_user :g :_group} 
            :ql/where  [:ql/= :g.user_id :u.id]})
   {:sql    "WITH _user AS ( SELECT * FROM user WHERE id = ? ) \n , _group AS ( SELECT g.* FROM user u \n JOIN group g ON g.user_id = u.id ) \n SELECT u.name AS name , g.name AS group FROM _user u , _group g WHERE g.user_id = u.id",
    :params [5]})


  (matcho/match
   (ql/sql
    {:ql/type   :ql/select
     :ql/select :*
     :ql/from   :user
     :ql/where  [:ql/= :u.id 1]
     :ql/limit  10
     :ql/offset 20})
   {:sql "SELECT * FROM user WHERE u.id = 1 LIMIT 10 OFFSET 20", :params []})

  (ql/sql
   {:ql/select :*
    :ql/from   {:x {:ql/type   :ql/select
                    :ql/select :*
                    :ql/from   :user
                    :ql/where  [:ql/= 1 1]}}
    :ql/where  [:ql/= :u.id 1]
    :ql/limit  10
    :ql/offset 20})

  (matcho/match
   (ql/sql
    {:ql/type       :ql/select
     :ql/select     :*
     :ql/from       :user
     :mssql/options {:a 1}}
    (ql.method/add-clause ql/default-opts
                          :ql/select
                          :before
                          :ql/order-by
                          {:key          :mssql/options
                           :default-type :mssql/options
                           :token        "OPTIONS"}))
   {:sql "SELECT * FROM user OPTIONS a = 1"}))

