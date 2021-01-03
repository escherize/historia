(ns historia.core
  (:require [mount.core :as mount]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]
            [defrag.core :as defrag])
  (:gen-class))

(def ^:dynamic *db-uri* "jdbc:sqlite::memory:")

(defn on-start []
  (let [spec {:connection-uri "jdbc:sqlite::memory:"}
        conn (jdbc/get-connection spec)]
    (assoc spec :connection conn)))

(mount/defstate ^{:on-reload :noop}
  db
  :start (on-start)
  :stop (do (-> db :connection .close) nil))

(defn init-db []
  (jdbc/execute!
   db
   "create table
 if not exists historia_calls
(id INTEGER PRIMARY KEY,
 fn_name TEXT,
 arguments TEXT,
 argument_values TEXT,
 body TEXT,
 end_time INTEGER,
 start_time INTEGER,
 output TEXT)"))

(defn clear-db! []
  (mount/start #'historia.core/db)
  (jdbc/execute! db "drop table historia_calls"))

(defn insert!
  [db fn-name
   args arg-values
   body output
   start-time end-time]
  (mount/start #'historia.core/db)
  (init-db)
  (let [d {:start_time start-time
           :end_time end-time
           :fn_name fn-name
           :arguments (pr-str args)
           :argument_values (pr-str arg-values)
           :body (pr-str body)
           :output (pr-str output)}]
    ;;(println (pr-str d))
    (jdbc/insert! db :historia_calls d)))

(defn def-history [name args body]
  `((let [start-time# (System/currentTimeMillis)
          out# (do ~@body)
          end-time# (System/currentTimeMillis)]
      (insert! db ~name
               '~args ~args
               '~body out#
               start-time# end-time#)
      out#)))

(defrag/defrag! defn-historia def-history)

(defn-historia ink [x] (+ 10 x))

(defn format-on-read [row]
  (-> row
      (update :arguments read-string)
      (update :argument_values read-string)
      (update :output read-string)
      (update :body read-string)))

(defn one [fn-name]
  (->
   (jdbc/query db ["select * from historia_calls
                    where fn_name = ?
                    order by start_time desc
                    limit 1" fn-name])
   first
   format-on-read))

(defn many [fn-name & [limit]]
  (let [limit (or limit 10)]
    (->>
     (jdbc/query db ["select * from historia_calls
                      where fn_name = ?
                      order by start_time desc
                      limit ?" fn-name limit])
     (mapv format-on-read))))

(defn-historia ink [x]
  (let [k (let [k (let [k (* 10 x)] k)] k)] k))

(ink 1)
(one "ink")

;;=> {:id 434,
;;    :fn_name "ink",
;;    :arguments [x],
;;    :argument_values [1],
;;    :body [(let [k (let [k (let [k (* 10 x)] k)] k)] k)],
;;    :end_time 1609624096662,
;;    :start_time 1609624096662,
;;    :output 10}

(count (many "ink"))
;;=> 10

(defn-historia fakt [x]
  (if (or (> 0.1 (rand)) (<= x 1))
    1
    (+ (fakt (dec x)) x)))

(time
 (do
   (fakt 20)
   (mapv (juxt :start_time :argument_values) (many "fakt" 20))))
