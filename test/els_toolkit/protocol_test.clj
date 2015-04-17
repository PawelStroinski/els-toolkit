(ns els-toolkit.protocol-test
  (:use els-toolkit.protocol expectations)
  (:require [clojure.java.io :as io])
  (:import (java.io ByteArrayInputStream)))

(def protocol {:text            (slurp "example-text.txt")
               :synonyms        [["FOOD"] ["ORDER" "REQUEST"] ["GARY"]]
               :min-skip        1
               :max-skip        Double/POSITIVE_INFINITY
               :text-population 100
               :shuffling       :els-random-placement
               :seed            8176753})

(defn replace-in-xml [s repl]
  (ByteArrayInputStream.
    (.getBytes
      (.replace
        (slurp "protocol.xml") s repl))))

(expect protocol (xml->map (io/file "protocol.xml")))
(expect 25 (:max-skip (xml->map (replace-in-xml ">max<" ">25<"))))
(expect protocol (xml->map (replace-in-xml "</" " </")))
(expect protocol (xml->map (replace-in-xml ">" "> ")))