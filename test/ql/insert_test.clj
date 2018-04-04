(ns ql.insert-test
  (:require [ql.core :as ql]
            [ql.insert :as sut]
            [ql.method :refer [to-sql]]
            [clojure.test :refer :all]
            [matcho.core :as matcho]))

(to-sql {:sql []} :key)

(deftest test-dataq
  (testing "select"
    (matcho/match
     (ql/sql {:ql/type :ql/insert
              :ql/table_name :user
              :ql/value {:id "1" :name "name"}
              :ql/returing :*})
     )))
