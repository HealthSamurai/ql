(ns ql.core-test
  (:require [ql.core :as sut]
            [clojure.test :refer :all]
            [matcho.core :as matcho]))

(deftest test-dataq

  (testing "select"

    (matcho/match
     (sut/sql {:ql/type :ql/select
               :alias :column
               :constant "string"
               :param {:ql/type :ql/param
                       :ql/value 10}})
     {:sql "SELECT column AS alias , 'string' AS constant , ( ? ) AS param"
      :params [10]}))


  (matcho/match
   (sut/sql {:ql/type :ql/param :ql/value 10})
   {:sql "?" :params [10]})

  (matcho/match
   (sut/sql {:ql/type :ql/select
             :alias :column})
   {:sql "SELECT column AS alias"})

  (matcho/match
   (sut/sql {:ql/type :ql/select
             :alias {:ql/type :ql/string
                     :ql/value "const"}})
   {:sql "SELECT ( $str$const$str$ ) AS alias"})

  (matcho/match
   (sut/sql {:ql/type :ql/select
             :alias {:ql/type :ql/param
                     :ql/value "const"}})
   {:sql "SELECT ( ? ) AS alias" :params ["const"]})

  (matcho/match
   (sut/sql {:ql/type :ql/select
             :alias {:ql/type :ql/query
                     :ql/select {:ql/type :ql/select
                                 :x 1}}})
   {:sql "SELECT ( SELECT 1 AS x ) AS alias" :params []})

  (matcho/match
   (sut/sql {:ql/type :ql/where
             :ql/comp "AND"
             :cond-1 [:ql/= :user.id 1]
             :cond-2 [:ql/<> :user.role "admin"]})

   {:sql "WHERE /** cond-1 **/ ( user.id = 1 ) AND /** cond-2 **/ ( user.role <> 'admin' )"})

  (matcho/match
   (sut/sql {:ql/type :ql/where
             :cond-1 [:ql/= :user.id 1]
             :cond-2 [:ql/<> :user.role "admin"]})

   {:sql "WHERE /** cond-1 **/ ( user.id = 1 ) AND /** cond-2 **/ ( user.role <> 'admin' )"})

  (matcho/match
   (sut/sql {:ql/type :ql/where
             :ql/comp "OR"
             :cond-1 [:ql/= :user.id 1]
             :cond-2 [:ql/<> :user.role "admin"]})

   {:sql "WHERE /** cond-1 **/ ( user.id = 1 ) OR /** cond-2 **/ ( user.role <> 'admin' )"})

  (matcho/match
   (sut/sql {:ql/type :ql/query
             :ql/select {:ql/type :ql/select
                         :name :name
                         :bd :birthDate}
             :ql/from {:ql/type :ql/from
                       :user :user}
             :ql/where {:ql/type :ql/where
                        :user-ids [:ql/= :user.id 5]}
             :ql/limit {:ql/type :ql/limit
                        :ql/value 10}})

   {:sql "SELECT name AS name , birthDate AS bd FROM user user WHERE /** user-ids **/ ( user.id = 5 ) LIMIT 10" :params []})

  

  (matcho/match
   (sut/sql {:ql/type :ql/query
             :ql/select {:name :name
                         :bd :birthDate}
             :ql/from {:user :user}
             :ql/where {:user-ids [:ql/= :user.id 5]}
             :ql/limit {:ql/value 10}})

   

   {:sql "SELECT name AS name , birthDate AS bd FROM user user WHERE /** user-ids **/ ( user.id = 5 ) LIMIT 10" :params []})

  (matcho/match
   (sut/sql {:ql/select [:ql/*]
             :ql/from {:u :user
                       :g :group}
             :ql/where {:user-ids [:ql/= :u.id :g.user_id]
                        :group-type [:ql/= :g.name "admin"]}})
   {:sql "SELECT * FROM u user , g group WHERE /** user-ids **/ ( u.id = g.user_id ) AND /** group-type **/ ( g.name = 'admin' )", :params []})

  (matcho/match
   (sut/sql 
    {:ql/select [:ql/*]
     :ql/from {:post :post}
     :ql/joins {:u {:ql/join-type "LEFT"
                    :ql/rel :user
                    :ql/on  {:by-ids [:ql/= :u.id :post.user_id]}}}})
   {:sql "SELECT * FROM post post LEFT JOIN user u ON /** by-ids **/ ( u.id = post.user_id )"})


  (matcho/match
   (sut/sql
    {:ql/type :ql/select
     :resource {:ql/type :jsonb/build-object
                :name :user.name
                :address [:jsonb/merge
                          [:jsonb/-> :resource :address]
                          {:ql/type :jsonb/build-object
                           :city "NY"
                           :zip  :address.zip}]}})

   {:sql "SELECT ( jsonb_build_object( 'name' , user.name , 'address' , resource ->'address' || jsonb_build_object( 'city' , 'NY' , 'zip' , address.zip ) ) ) AS resource"})


  (matcho/match
   (sut/sql
    {:ql/type :ql/select
     :resource {:ql/type :jsonb/build-object
                :name :user.name
                :address [:jsonb/merge
                          [:jsonb/-> :resource :address]
                          {:ql/type :jsonb/build-object
                           :city {:ql/type :ql/param
                                  :ql/value "NY"}
                           :zip  :address.zip}]}})

   {:sql "SELECT ( jsonb_build_object( 'name' , user.name , 'address' , resource ->'address' || jsonb_build_object( 'city' , ? , 'zip' , address.zip ) ) ) AS resource"
    :params ["NY"]})
  )

(sut/sql
 #:ql{:select {:name :u.name}
      :from {:u :user}
      :where {:by-id [:ql/= :u.id [:ql/param 5]]}})

(sut/sql
 {:ql/select {:name :u.name}
  :ql/from {:u :user}
  :ql/where {:by-id {:ql/type :ql/=
                     0 :u.id
                     1 {:ql/type :ql/param
                        :ql/value 5}}}})



(sut/sql
 (merge-with
  merge
  {:ql/from {:u :user}}
  {:ql/from {:g :group}}
  {:ql/select {:name :u.name}}
  {:ql/select {:group :g.name}}
  {:ql/where {:join    [:ql/= :g.user_id :u.id]}}
  {:ql/where {:by-role [:ql/= :u.role "admin"]}}
  {:ql/where {:by-id   [:ql/= :u.id [:ql/param 5]]}}))



{:ql/type :ql/fn
 :ql/fn "lower"
 0 "param-1"
 1 "param-2"}


[:ql/fn "lower" "param-1" "param-2"]

{:ql/type :ql/cast
 :ql/cast :pg/timestamptz
 :ql/expression "2011-01-01"}

[:ql/cast :pg/timestamptz "2011-01-01"]

(comment


  )
