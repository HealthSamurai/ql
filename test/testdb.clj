(ns testdb
  (:require [cheshire.core :as json]
            [clojure.java.jdbc :as jdbc]
            [clojure.test :refer [deftest]]
            [matcho.core :as matcho])
  (:import org.postgresql.util.PGobject))

(def cfg
  {:connection-uri
   (or (System/getenv "DATABASE_URL")
       "jdbc:postgresql://localhost:5447/ql?user=postgres&password=verysecret")})

(extend-protocol jdbc/IResultSetReadColumn
  ;; (result-set-read-column [v _ _] (vec (.getArray v)))

  PGobject
  (result-set-read-column [pgobj _metadata _index]
    (let [type  (.getType pgobj)
          value (.getValue pgobj)]
      (case type
        "json"      (json/parse-string value keyword)
        "jsonb"     (json/parse-string value keyword)
        value))))


(defn query [q]
  (jdbc/query cfg q))

(defn execute [q]
  (jdbc/execute! cfg q))

(deftest test-query
  (matcho/match
   (query ["select '[1,3]'::jsonb json_array"])
   [{:json_array [1 3]}]))

