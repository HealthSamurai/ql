(ns ql.core-test
  (:require [ql.core :as sut]
            [clojure.test :refer :all]
            [matcho.core :as matcho]))

(deftest test-dataq

  (testing "projection"

    (matcho/match
     (sut/sql {:ql/type :ql/projection
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
   (sut/sql {:ql/type :ql/projection
                :alias :column})
   {:sql "SELECT column AS alias"})

  (matcho/match
   (sut/sql {:ql/type :ql/projection
                :alias {:ql/type :ql/string
                        :ql/value "const"}})
   {:sql "SELECT ( $str$const$str$ ) AS alias"})

  (matcho/match
   (sut/sql {:ql/type :ql/projection
                :alias {:ql/type :ql/param
                        :ql/value "const"}})
   {:sql "SELECT ( ? ) AS alias" :params ["const"]})

  (matcho/match
   (sut/sql {:ql/type :ql/projection
                :alias {:ql/type :ql/select
                        :select {:ql/type :ql/projection
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
   (sut/sql {:ql/type :ql/select
                :select {:ql/type :ql/projection
                         :name :name
                         :bd :birthDate}
                :from {:ql/type :ql/from
                       :user :user}
                :where {:ql/type :ql/where
                        :user-ids [:ql/= :user.id 5]}
                :limit {:ql/type :ql/limit
                        :ql/value 10}})

   {:sql "SELECT name AS name , birthDate AS bd FROM user user WHERE /** user-ids **/ ( user.id = 5 ) LIMIT 10" :params []})

  (matcho/match
   (sut/sql {:ql/type :ql/select
                :select {:name :name
                         :bd :birthDate}
                :from {:user :user}
                :where {:user-ids [:ql/= :user.id 5]}
                :limit {:ql/value 10}})

   {:sql "SELECT name AS name , birthDate AS bd FROM user user WHERE /** user-ids **/ ( user.id = 5 ) LIMIT 10" :params []})

  (matcho/match
   (sut/sql {:select [:ql/*]
                :from {:u :user
                       :g :group}
                :where {:user-ids [:ql/= :u.id :g.user_id]
                        :group-type [:ql/= :g.name "admin"]}})
  {:sql "SELECT * FROM u user , g group WHERE /** user-ids **/ ( u.id = g.user_id ) AND /** group-type **/ ( g.name = 'admin' )", :params []})

  (matcho/match
   (sut/sql 
    {:select [:ql/*]
     :from {:post :post}
     :joins {:u {:ql/join-type "LEFT"
                 :ql/rel :user
                 :ql/on  {:by-ids [:ql/= :u.id :post.user_id]}}}})
   {:sql "SELECT * FROM post post LEFT JOIN user u ON /** by-ids **/ ( u.id = post.user_id )"})


  (matcho/match
   (sut/sql
    {:ql/type :ql/projection
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
    {:ql/type :ql/projection
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


(comment


  )
