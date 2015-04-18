(ns els-toolkit.protocol-test
  (:use els-toolkit.protocol expectations)
  (:require [clojure.java.io :as io])
  (:import (java.io ByteArrayInputStream)))

(def protocol {:text            (slurp "example-text.txt")
               :synonyms        [["FOOD"] ["ORDER" "REQUEST"] ["GARY"]]
               :min-skip        1
               :max-skip        100
               :max-cylinder    50
               :text-population 200
               :shuffling       :els-random-placement
               :seed            8176753})

(defn replace-in-xml [s repl]
  (ByteArrayInputStream.
    (.getBytes
      (.replace
        (slurp "protocol.xml") s repl))))

(expect protocol (xml->map (io/file "protocol.xml")))
(expect Double/POSITIVE_INFINITY (:max-skip (xml->map (replace-in-xml ">100<" ">max<"))))
(expect Double/POSITIVE_INFINITY (:max-cylinder (xml->map (replace-in-xml ">50<" ">max<"))))
(expect protocol (xml->map (replace-in-xml "</" " </")))
(expect protocol (xml->map (replace-in-xml ">" "> ")))