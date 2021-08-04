(ns historia.core
  (:require [mount.core :as mount]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]
            [defrag.core :as defrag])
  (:gen-class))

(def *db-uri (atom {:connection-uri "jdbc:sqlite:.historia.db"}))

(defn set-db!
  "Call with something jdbc can use as a db spec before using defn-historia, or calling `mount/start`.

  For reference: https://github.com/clojure/java.jdbc#example-usage "
  [uri]
  (reset! *db-uri uri))

(defn on-start []
  (let [spec @*db-uri
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
 if not exists calls
(id INTEGER PRIMARY KEY,
 fn_name TEXT,
 full_name TEXT,
 arguments TEXT,
 argument_values TEXT,
 body TEXT,
 end_time INTEGER,
 start_time INTEGER,
 output TEXT)"))

(defn clear-db! [yes]
  (if (= :yes yes)
    (do
      (mount/start #'historia.core/db)
      (jdbc/execute! db "drop table calls"))
    (println "Call this with :yes if you want to clear it.")))

(defn insert-start!
  [db fn-name
   args arg-values
   body start-time]
  (mount/start #'historia.core/db)
  (init-db)
  (let [d {:start_time start-time
           :fn_name fn-name
           :full_name (str *ns* "/" fn-name)
           :arguments (pr-str args)
           :argument_values (pr-str arg-values)
           :body (pr-str body)
           ;; fill in unfilled values:
           :output ":historia/unfilled"
           :end_time 0}]
    (jdbc/insert! db :calls d)))

(defn insert-end! [db id output end-time]
  (println "updatting db at id: " id)
  (jdbc/update! db
                :calls
                {:output output :end_time end-time}
                ["id = ?" id]))

(defn def-history [name args body]
  `((let [start-time# (System/currentTimeMillis)
          _# (insert-start! db ~name
                            '~args ~args
                            '~body start-time#)
          id# (first (vals (first _#)))
          out# (do ~@body)
          end-time# (System/currentTimeMillis)]
      (insert-end! db id# out# end-time#)
      out#)))

(defrag/defrag! defn-historia def-history)

;; Reading back the history:

(defn format-on-read [row] (let [r (fnil read-string "nil")]
                             (-> row
                                 (update :arguments r)
                                 (update :argument_values r)
                                 (update :output r)
                                 (update :body r))))

(defn one [fn-name] (some->
                     (jdbc/query db ["select * from calls
                                      where fn_name = ?
                                      order by start_time desc
                                      limit 1" fn-name])
                     first
                     format-on-read))

(defn full [full-name] (some->
                        (jdbc/query db ["select * from calls
                                         where full_name = ?
                                         order by start_time desc
                                         limit 1" full-name])
                        first
                        format-on-read))

(defn many [fn-name & [limit]] (let [limit (or limit 10)]
                                 (some->>
                                  (jdbc/query db ["select * from calls
                                                   where fn_name = ?
                                                   order by start_time desc
                                                   limit ?" fn-name limit])
                                  (mapv format-on-read))))

(defn many-full [full-name & [limit]] (let [limit (or limit 10)]
                                        (some->>
                                         (jdbc/query db ["select * from calls
                                                          where full_name = ?
                                                          order by start_time desc
                                                          limit ?" full-name limit])
                                         (mapv format-on-read))))

(comment

  ;; usage:

  (defn-historia ^:private
    ink
    ([] (rand))
    ([x] (* 10 x)))

  (ink "2")
  (=
   (one "ink")
   (one-full "historia.core/ink"))
  ;;=> true

  (one "ink")

  ;;=>  {:fn_name "ink"
  ;;     :arguments [x]
  ;;     :start_time 1609713723393
  ;;     :argument_values [2]
  ;;     :output 20
  ;;     :end_time 1609713723393
  ;;     :id 2
  ;;     :full_name "historia.core/ink"
  ;;     :body [(* 10 x)]}

  (defn-historia fakt [x]
    (if (<= x 1)
      1
      (+ (fakt (dec x)) x)))

  (fakt 20)

  (fakt "??")

  (mapv (juxt :argument_values :output) (many "fakt" 3))
  ;;=> [[["??"] :historia/unfilled]
  ;;    [[1]    1]
  ;;    [[2]    3]]

  )
