(ns ql.pretty-sql
  (:require [clojure.string :as str]))


(def ^:private
  example-input ["SELECT" ::newline ::ident "a" "alias" "," ::newline "b" "aliasb" ::newline ::deident
                 "FROM" ::newline ::ident "user" ::newline ::deident])

(def
  pretty-operations
  #{::newline
    ::ident
    ::deident})

(defn- update-result [result state term]
  (-> result
      (conj (if (:newlined state)
              (reduce str "\n" (repeat (:ident-level state) \space))
              \space))
      (conj term)))

(defn- update-state [state op]
  (case op
    ::newline (assoc state :newlined true)
    ::ident (update state :ident-level + 2)
    ::deident (update state :ident-level - 2)))

(defn- reset-newline [state]
  (assoc state :newlined false))

(defn- pretty-reducer [acc token]
  (if (contains? pretty-operations token)
    (update acc :state update-state token)
    (-> acc
        (update :result update-result (:state acc) token)
        (update :state reset-newline))))


(defn make-pretty-sql
  "Convert vector of terminals and print control operations into pretty sql string"
  [input-code]
  (->> (reduce pretty-reducer
               {:result []
                :state  {:newlined    true
                         :ident-level 0}}
               input-code)
       :result
       rest
       str/join))
