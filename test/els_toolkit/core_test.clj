(ns els-toolkit.core-test
  (:use els-toolkit.core expectations)
  (:require [els-toolkit.protocol-test :as pt]))

(defn protocol
  ([shuffling]
   (protocol shuffling []))
  ([shuffling kvs]
   (protocol shuffling kvs 10))
  ([shuffling kvs text-population]
   (apply assoc pt/protocol (into [:shuffling shuffling :text-population text-population] kvs))))

(def order-food-gary-table {:elses    #{{:start 10, :word "ORDER", :skip 10}
                                        {:start 9, :word "FOOD", :skip 10}
                                        {:start 27, :word "GARY", :skip 10}},
                            :cylinder 9, :h 6, :y 1, :x 0, :w 6})
(expect {:table order-food-gary-table :same-or-better 0} (run (protocol :letter-shuffling)))
(expect {:table order-food-gary-table :same-or-better 6} (run (protocol :els-random-placement)))
; VERY SLOW!
; (expect {:table order-food-gary-table :same-or-better 0} (run (protocol :letter-shuffling [] 1500)))
; SLOW!
; (expect {:table order-food-gary-table :same-or-better 32503} (run (protocol :els-random-placement [] 50000)))

(def nil-kvs '(:text "f--o--o" :synonyms [["of"]]))
(expect nil (run (protocol :letter-shuffling nil-kvs)))
(expect nil (run (protocol :els-random-placement nil-kvs)))

(def ww-table {:elses    #{{:word "ww", :start 0, :skip 2}},
               :cylinder 2, :h 2, :y 0, :x 0, :w 1})
(def ww-kvs '(:text "w-w" :synonyms [["ww"]]))
(expect {:table ww-table :same-or-better 9} (run (protocol :letter-shuffling ww-kvs)))
(expect {:table ww-table :same-or-better 9} (run (protocol :els-random-placement ww-kvs)))

(def foo-table {:elses    #{{:word "foo", :start 0, :skip 3}},
                :cylinder 3, :h 3, :y 0, :x 0, :w 1})
(def foo-kvs '(:text "f--o--o" :synonyms [["foo" "of"]]))
(expect {:table foo-table, :same-or-better 7} (run (protocol :letter-shuffling foo-kvs)))
(expect {:table foo-table, :same-or-better 9} (run (protocol :els-random-placement foo-kvs)))

(def longer-synonyms-table {:elses    #{{:word "II", :start 2, :skip 2}
                                        {:word "STF", :start 3, :skip 3}},
                            :cylinder 1, :h 8, :y 2, :x 0, :w 1})
(def longer-synonyms-kvs '(:synonyms [["II" "TSF" "UMF" "DFG" "ADI" "LDT" "ESS" "AYD" "MFO" "XEP"]
                                      ["STF" "EIMS" "DRCS" "EITS" "LCAT" "ORRM"]]))
(expect {:table longer-synonyms-table :same-or-better 2} (run (protocol :letter-shuffling longer-synonyms-kvs 30)))
(expect {:table longer-synonyms-table :same-or-better 5} (run (protocol :els-random-placement longer-synonyms-kvs 30)))