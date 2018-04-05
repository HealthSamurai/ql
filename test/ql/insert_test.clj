(ns ql.insert-test
  (:require [ql.core :as ql]
            [ql.insert :as sut]
            [clojure.test :refer :all]
            [matcho.core :as matcho]))


(deftest test-dataq
  (testing "insert with returning"
    (matcho/match
     (ql/sql {:ql/type :ql/insert
              :ql/table_name :user
              :ql/value {:id "1"
                         :name {:ql/type :ql/jsonb
                                :name "name"}}
              :ql/returning :*})
     {:sql "INSERT INTO user ( id , name ) VALUES ( '1' , $JSON${\"name\":\"name\"}$JSON$ ) RETURNING *"})))
