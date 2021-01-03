(defproject escherize/historia "0.1.1"
  :description "Record and observe what your functions are doing"
  :url "https://github.com/escherize/historia"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.xerial/sqlite-jdbc "3.34.0"]
                 [org.clojure/java.jdbc "0.7.11"]
                 [escherize/defrag "0.1.4"]
                 [mount "0.1.16"]]
  :main ^:skip-aot historia.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
