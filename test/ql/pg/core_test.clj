(ns ql.pg.core-test
  (:require [ql.pg.core :as sut]
            [ql.core :as ql]
            [matcho.core :as matcho]
            [clojure.test :refer :all]))

(deftest pg-test


  (testing "jsonb->"
    (matcho/match
     (ql/sql [:jsonb/-> :table.column :key])
     {:sql "table.column ->'key'", :params []}))

  (testing "jsonb->>"
    (matcho/match
     (ql/sql [:jsonb/->> :table.column :key])
     {:sql "table.column ->>'key'", :params []}))

  (testing "jsonb#>"
    (matcho/match
     (ql/sql [:jsonb/#> :table.column [:a 0 :b]])
     {:sql "table.column #>'{a,0,b}'", :params []}))

  (testing "jsonb#>"
    (matcho/match
     (ql/sql {:ql/type :jsonb/#>
              :left :table.column
              :right [:a 0 :b]})
     {:sql "table.column #>'{a,0,b}'", :params []}))

  (testing "jsonb#>>"
    (matcho/match
     (ql/sql [:jsonb/#>> :table.column [:a 0 :b]])
     {:sql "table.column #>>'{a,0,b}'", :params []}))

  )

