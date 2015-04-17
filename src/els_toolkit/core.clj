(ns els-toolkit.core
  (:require [els-toolkit.optimize :refer (best-table table-area)]
            [els-toolkit.population :refer (new-distribution fisher–yates-shuffle els-random-placement)]
            [els-toolkit.protocol :refer (xml->map)]
            [els-toolkit.search :refer (look-for-synonyms)]
            [clojure.pprint :refer (pprint)])
  (:gen-class))

(defn find-table [protocol text]
  (when-let [elses (look-for-synonyms protocol (:synonyms protocol) text)]
    (best-table elses (:synonyms protocol))))

(defn area-or-nil [table]
  (when table
    (table-area table)))

(defn same-or-better [table areas-from-shuffled]
  (let [area>= (partial >= (table-area table))]
    (count (filter area>= areas-from-shuffled))))

(defmulti run :shuffling)

(defmethod run :letter-shuffling [protocol]
  (when-let [table (find-table protocol (:text protocol))]
    (let [distribution (new-distribution (:seed protocol))
          shuffled-texts (doall (repeatedly (dec (:text-population protocol))
                                            #(fisher–yates-shuffle (:text protocol) distribution)))
          areas-from-shuffled (keep identity (pmap (comp area-or-nil (partial find-table protocol)) shuffled-texts))]
      {:table table :same-or-better (same-or-better table areas-from-shuffled)})))

(defmethod run :els-random-placement [protocol]
  (when-let [elses (look-for-synonyms protocol (:synonyms protocol) (:text protocol))]
    (let [text-len (count (:text protocol))
          distribution (new-distribution (:seed protocol))
          shuffled-elses (doall (repeatedly (dec (:text-population protocol))
                                            #(els-random-placement elses text-len distribution)))
          areas-from-shuffled (pmap (comp table-area #(best-table % (:synonyms protocol))) shuffled-elses)
          table (best-table elses (:synonyms protocol))]
      {:table table :same-or-better (same-or-better table areas-from-shuffled)})))

(defn -main [& args]
  (let [protocol-file (or (first args) "protocol.xml")]
    (printf "\nUsing '%s' protocol file.%s\n\n" protocol-file
            (if (first args) "" " (Different can be passed as an argument.)"))
    (pprint (run (xml->map protocol-file)))))