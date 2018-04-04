(defproject ql "0.0.1-SNAPSHOT"
  :description "data dsl for sql generation"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]]
  :repositories  [["clojars"  {:url "https://clojars.org/repo"
                               :sign-releases false}]]
  :profiles {:dev {:dependencies [[matcho "0.1.0-RC6"]
                                  [org.clojure/java.jdbc "0.6.1"]
                                  [cheshire "5.6.3"]
                                  [org.postgresql/postgresql "9.4.1211.jre7"]]}})
