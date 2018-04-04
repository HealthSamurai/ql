(ns ql.pg.core
  (:require
   [clojure.string :as str]
   [ql.method :refer [to-sql conj-sql conj-param reduce-separated]]
   [ql.pg.jsonb]
   [ql.pg.string]))


(defn comma-separated [ks]
  (->> ks
       (mapv (fn [x] (if (keyword? x) (name x) (str x))))
       (str/join ",")))

;; (comma-separated [:a :b 0])

(defn operator-args [opts]
  (if (map? opts)
    [(:ql/type opts)
     (or (:left opts) (get opts 0))
     (or (:right opts) (get opts 1))]
    opts))

