(ns ql.pg.jsonb-test
  (:require [ql.pg.jsonb :as sut]
            [ql.core :as ql]
            [ql.pg.jsonb]
            [matcho.core :as matcho]
            [clojure.test :refer :all]))

(deftest pg-jsonb-test
  (testing "jsonb_agg"
    (matcho/match
     (ql/sql [:jsonb/agg :table.*])
     {:sql "jsonb_agg( table.* )", :params []})

    (matcho/match
     (ql/sql {:ql/type :jsonb/agg :expression :table.*})
     {:sql "jsonb_agg( table.* )", :params []})
    )
  )

