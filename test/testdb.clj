(ns testdb
  (:require [clojure.java.jdbc :as jdbc]
            [cheshire.core :as json])
  (:import [org.postgresql.jdbc PgArray]
           org.postgresql.util.PGobject))


(def cfg {:connection-uri (System/getenv "DATABASE_URL")})

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


(query ["select '[1,3]'::jsonb"])

