(ns ql.core-test
  (:require [ql.core :as sut]
            [clojure.test :refer :all]
            [matcho.core :as matcho]
            [ql.core :as ql]))

(deftest test-ql

  (testing "select"

    (matcho/match
     (sut/sql {:ql/type :ql/projection
               :alias :column
               :constant "string"
               :param {:ql/type :ql/param
                       :ql/value 10}})
     {:sql "column AS alias , 'string' AS constant , ( ? ) AS param"
      :params [10]}))


  (matcho/match
   (sut/sql {:ql/type :ql/param :ql/value 10})
   {:sql "?" :params [10]})

  (matcho/match
   (sut/sql {:ql/type :ql/projection
             :alias :column})
   {:sql "column AS alias"})

  (matcho/match
   (sut/sql {:ql/type :ql/projection
             :alias {:ql/type :ql/string
                     :ql/value "const"}})
   {:sql "( $str$const$str$ ) AS alias"})

  (matcho/match
   (sut/sql {:ql/type :ql/projection
             :alias {:ql/type :ql/param
                     :ql/value "const"}})
   {:sql "( ? ) AS alias" :params ["const"]})

  (matcho/match
   (sut/sql {:ql/type :ql/projection
             :alias {:ql/type :ql/select
                     :ql/select {:ql/type :ql/projection :x 1}}})
   {:sql "( SELECT 1 AS x ) AS alias" :params []})

  (matcho/match
   (sut/sql {:ql/type :ql/predicate
             :ql/comp "AND"
             :cond-1 [:ql/= :user.id 1]
             :cond-2 [:ql/<> :user.role "admin"]})

   {:sql "/** cond-1 **/ ( user.id = 1 ) AND /** cond-2 **/ ( user.role <> 'admin' )"})

  (matcho/match
   (sut/sql {:ql/type :ql/predicate
             :cond-1 [:ql/= :user.id 1]
             :cond-2 [:ql/<> :user.role "admin"]})

   {:sql "/** cond-1 **/ ( user.id = 1 ) AND /** cond-2 **/ ( user.role <> 'admin' )"})

  (matcho/match
   (sut/sql {:ql/type :ql/predicate
             :ql/comp "OR"
             :cond-1 [:ql/= :user.id 1]
             :cond-2 [:ql/<> :user.role "admin"]})

   {:sql "/** cond-1 **/ ( user.id = 1 ) OR /** cond-2 **/ ( user.role <> 'admin' )"})

  (matcho/match
   (sut/sql {:ql/type :ql/select
             :ql/select {:name :name
                         :bd :birthDate}
             :ql/from {:ql/type :ql/from
                       :user :user}
             :ql/where {:user-ids [:ql/= :user.id 5]}
             :ql/limit 10})

   {:sql "SELECT name AS name , birthDate AS bd FROM user user WHERE /** user-ids **/ ( user.id = 5 ) LIMIT 10" :params []})

  (matcho/match
   (sut/sql {:ql/type :ql/select
             :ql/select {:name :name :bd :birthDate}
             :ql/from {:user :user}
             :ql/where {:user-ids [:ql/= :user.id 5]}
             :ql/limit 10})

   

   {:sql "SELECT name AS name , birthDate AS bd FROM user user WHERE /** user-ids **/ ( user.id = 5 ) LIMIT 10" :params []})

  (matcho/match
   (sut/sql {:ql/select :*
             :ql/from {:u :user
                       :g :group}
             :ql/where {:user-ids [:ql/= :u.id :g.user_id]
                        :group-type [:ql/= :g.name "admin"]}})
   {:sql "SELECT * FROM user u , group g WHERE /** user-ids **/ ( u.id = g.user_id ) AND /** group-type **/ ( g.name = 'admin' )", :params []})

  (matcho/match
   (sut/sql 
    {:ql/select :*
     :ql/from {:post :post}
     :ql/joins {:u {:ql/join-type "LEFT"
                    :ql/rel :user
                    :ql/on  {:by-ids [:ql/= :u.id :post.user_id]}}}})
   {:sql "SELECT * FROM post post \n LEFT JOIN user u ON /** by-ids **/ ( u.id = post.user_id )"})

  (matcho/match
   (sut/sql 
    {:ql/select :*
     :ql/from {:post :post}
     :ql/joins {:u {:ql/join-type "LEFT"
                    :ql/rel :user
                    :ql/on  [:ql/= :u.id :post.user_id]}}})
   {:sql "SELECT * FROM post post \n LEFT JOIN user u ON u.id = post.user_id"})


  (matcho/match
   (sut/sql
    {:ql/type :ql/projection
     :resource {:ql/type :jsonb/build-object
                :name :user.name
                :address [:jsonb/||
                          [:jsonb/-> :resource :address]
                          {:ql/type :jsonb/build-object
                           :city "NY"
                           :zip  :address.zip}]}})

   {:sql "( jsonb_build_object( 'name' , user.name , 'address' , resource ->'address' || jsonb_build_object( 'city' , 'NY' , 'zip' , address.zip ) ) ) AS resource"})

  (matcho/match
   (sut/sql
    {:ql/type :ql/projection
     :resource {:ql/type :jsonb/build-object
                :name :user.name
                :address [:jsonb/||
                          [:jsonb/-> :resource :address]
                          {:ql/type :jsonb/build-object
                           :city {:ql/type :ql/param
                                  :ql/value "NY"}
                           :zip  :address.zip}]}})

   {:sql "( jsonb_build_object( 'name' , user.name , 'address' , resource ->'address' || jsonb_build_object( 'city' , ? , 'zip' , address.zip ) ) ) AS resource"
    :params ["NY"]})

  (matcho/match
   (sut/sql {:ql/type :ql/with
             :users  {:ql/weight 0
                      :ql/select {:name :name}
                      :ql/from   {:users :users}}
             :roles  {:ql/weight 1
                      :ql/select {:name :name}
                      :ql/from   {:group :group}
                      :ql/joins  {:u {:ql/rel :users
                                      :ql/on {:join-cond [:ql/= :u.id :g.user_id]}}}}
             :ql/select {:u :u.name :g :g.name}
             :ql/from {:u :user
                       :r :roles}})

   {:sql
    "WITH users AS ( SELECT name AS name FROM users users )\n , roles AS ( SELECT name AS name FROM group group \n JOIN users u ON /** join-cond **/ ( u.id = g.user_id ) )\n SELECT u.name AS u , g.name AS g FROM user u , roles r",
    :params [],
    :opts nil})



  (testing "group-by"
    (matcho/match
     (sut/sql {:ql/select {:a :expr :b :other}
               :ql/from {:t :t}
               :ql/group-by [:ql/list :expr :other]})
     {:sql "SELECT expr AS a , other AS b FROM t t GROUP BY expr , other"}))

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
