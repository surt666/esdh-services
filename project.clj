(defproject esdh-services "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [yada "1.1.41"]
                 [aleph "0.4.1"]
                 [bidi "2.0.13"]
                 [clj-time "0.12.0"]
                 [danlentz/clj-uuid "0.1.6"]
                 [ring "1.4.0"]
                 [org.apache.tinkerpop/tinkergraph-gremlin "3.2.2"]
                 [com.datastax.cassandra/dse-driver "1.1.0" :exclusions [io.netty/netty-handler]]
                 [org.apache.tinkerpop/gremlin-driver "3.2.2"]
                 [com.itextpdf.tool/xmlworker "5.5.8"]])
