(ns esdh-services.graph
  (:require [clojure.walk :refer [keywordize-keys]])
  (:import [org.apache.tinkerpop.gremlin.structure Vertex]
           [com.datastax.driver.dse DseCluster DseSession]
           [com.datastax.driver.dse.graph GraphOptions]
           [com.datastax.driver.dse.graph GraphStatement SimpleGraphStatement]
           [java.util HashMap]))

(def cluster (.. DseCluster (builder) (addContactPoint "127.0.0.1") (withGraphOptions (. (GraphOptions.) (setGraphName "esdh"))) (build)))

(def session (.connect cluster))

;;(.. session (executeGraph "g.V()") (one))

;; (.. session (executeGraph "g.V()") (one) (as Vertex) (property "ice-id") (value))

;; (.. session (executeGraph "g.V().has('sag','type','henvendelse')") (one) (as Vertex) (property "ice-id") (value))

;; g.V().has('ice-id','123').outE('har_bfe').has('tx', lt('2016-02-01')).has('fra',gt('2012-01-01')).has('til',lt('2014-01-01'))

;; (def s (SimpleGraphStatement. "v1 = g.addV(label,'notat','ice-id','345221','dokument',d1)"))
;; #'esdh.core/s
;; esdh.core> (. s (set "d1" (.getBytes "82828")))
;; #object[com.datastax.driver.dse.graph.SimpleGraphStatement 0x53fd1471 "com.datastax.driver.dse.graph.SimpleGraphStatement@53fd1471"]
;; esdh.core> s
;; #object[com.datastax.driver.dse.graph.SimpleGraphStatement 0x53fd1471 "com.datastax.driver.dse.graph.SimpleGraphStatement@53fd1471"]
;; esdh.core> (.. session (executeGraph s))
;; #object[com.datastax.driver.dse.graph.GraphResultSet 0x4ad703be "com.datastax.driver.dse.graph.GraphResultSet@4ad703be"]

(defn find-sager [& id]
  (let [s (SimpleGraphStatement. "g.V().has(label,'sag').valueMap()")
        vertices (.. session (executeGraph s) (all))]
    (map keywordize-keys (map #(into {} (. % (as HashMap))) vertices))))

(defn find-akter [sags-id]
  (let [s (SimpleGraphStatement. "g.V().has('sag','ice-id',sagsid).in('tilhører').valueMap()")
        _ (doto s (.set "sagsid" sags-id))
        vertices (.. session (executeGraph s) (all))]
    (map keywordize-keys (map #(into {} (. % (as HashMap))) vertices))))

(defn find-notat [akt-id]
  (let [s (SimpleGraphStatement. "g.V().has('akt','ice-id',aktid).in('begrunder').valueMap()")
        _ (doto s (.set "aktid" akt-id))
        notat (.. session (executeGraph s) (one))]
    (keywordize-keys (into {} (. notat (as HashMap))))))

(defn find-dokumenter [akt-id]
  (let [s (SimpleGraphStatement. "g.V().has('akt','ice-id',aktid).out('indeholder').valueMap()")
        _ (doto s (.set "aktid" akt-id))
        dokumenter (.. session (executeGraph s) (all))]
    (map keywordize-keys (map #(into {} (. % (as HashMap))) dokumenter))))

(defn find-dokument [dok-id]
  (let [s (SimpleGraphStatement. "g.V().has('dokument','ice-id',dokid).valueMap()")
        _ (doto s (.set "dokid" dok-id))
        dokument (.. session (executeGraph s) (one))]
    (keywordize-keys (into {} (. dokument (as HashMap))))))

(defn opret-notat [ice-id endelig? dokument oprettet akt-id]
  (let [s (SimpleGraphStatement. (str "v1 = g.addV(label,'notat','ice-id',i,'dokument',d,'endelig',e,'oprettet',o).next()\n"
                                      "v2 = g.V().has(label,'akt').has('ice-id',ai).next()\n"
                                      "v1.addEdge('begrunder',v2)"))]
    (doto s (.set "e" endelig?) (.set "d" (.getBytes dokument)) (.set "o" oprettet) (.set "i" ice-id) (.set "ai" akt-id))
    (.. session (executeGraph s))))

(defn opret-dokument [ice-id akt-id endelig? dokument oprettet mime-type titel]
  "dokument er bytearray"
  (let [s (SimpleGraphStatement. (str "v1 = g.addV(label,'dokument','ice-id',i,'dokument',d,'endelig',e,'oprettet',o,'titel',t,'mime-type',m).next()\n"
                                      "v2 = g.V().has(label,'akt').has('ice-id',ai).next()\n"
                                      "v2.addEdge('indeholder',v1)"))]
    (doto s (.set "e" endelig?) (.set "d" dokument) (.set "o" oprettet) (.set "i" ice-id) (.set "t" titel) (.set "m" mime-type) (.set "ai" akt-id))
    (.. session (executeGraph s))))

(defn opret-sag [ice-id myndighed type oprettet sagsbehandler]
  "dokument er bytearray"
  (let [s (SimpleGraphStatement. "g.addV(label,'sag','ice-id',i,'myndighed',m,'type',t,'oprettet',o,'sagsbehandler',s)")]
    (doto s (.set "m" myndighed) (.set "t" type) (.set "o" oprettet) (.set "i" ice-id) (.set "s" sagsbehandler))
    (.. session (executeGraph s))))

(defn opret-akt [sags-id ice-id myndighed type oprettet sagsbehandler]
  "dokument er bytearray"
  (let [s (SimpleGraphStatement. (str "v1 = g.addV(label,'akt','ice-id',i,'myndighed',m,'type',t,'oprettet',o,'sagsbehandler',s).next()\n"
                                      "v2 = g.V().has(label,'sag').has('ice-id',si).next()\n"
                                      "v1.addEdge('tilhører',v2)"))]
    (doto s (.set "m" myndighed) (.set "t" type) (.set "o" oprettet) (.set "i" ice-id) (.set "s" sagsbehandler) (.set "si" sags-id))
    (.. session (executeGraph s))))
