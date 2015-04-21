(ns els-toolkit.core
  (:require [els-toolkit.optimize :refer (best-table)]
            [els-toolkit.population :refer (new-distribution fisher–yates-shuffle els-random-placement)]
            [els-toolkit.protocol :refer (xml->map)]
            [els-toolkit.search :refer (look-for-synonyms)]
            [clojure.pprint :refer (pprint)]
            [taoensso.timbre :as timbre])
  (:gen-class))
(timbre/refer-timbre)

(defn find-table [protocol text]
  (when-let [elses (look-for-synonyms protocol (:synonyms protocol) text)]
    (best-table elses (:synonyms protocol) (:max-cylinder protocol))))

(defn same-or-better [table areas-from-shuffled]
  (let [area>= (partial >= (:area table))]
    (count (filter area>= areas-from-shuffled))))

(defmulti run :shuffling)

(defmethod run :letter-shuffling [protocol]
  (when-let [table (find-table protocol (:text protocol))]
    (let [distribution (new-distribution (:seed protocol))
          shuffled-texts (doall (repeatedly (dec (:text-population protocol))
                                            #(fisher–yates-shuffle (:text protocol) distribution)))
          areas-from-shuffled (keep identity (pmap (comp :area (partial find-table protocol))
                                                   shuffled-texts))]
      {:table table :same-or-better (same-or-better table areas-from-shuffled)})))

(defmethod run :els-random-placement [protocol]
  (when-let [elses (look-for-synonyms protocol (:synonyms protocol) (:text protocol))]
    (trace (count elses) elses)
    (let [text-len (count (:text protocol))
          distribution (new-distribution (:seed protocol))
          shuffled-elses (doall (repeatedly (dec (:text-population protocol))
                                            #(els-random-placement elses text-len distribution)))
          areas-from-shuffled (pmap #(:area (best-table % (:synonyms protocol) (:max-cylinder protocol)))
                                    shuffled-elses)
          table (best-table elses (:synonyms protocol) (:max-cylinder protocol))]
      {:table table :same-or-better (same-or-better table areas-from-shuffled)})))

(defn -main [& args]
  (timbre/set-level! (keyword (nth args 1 "debug")))
  (let [protocol-file (or (first args) "protocol.xml")]
    (printf "\nUsing '%s' protocol file.%s\n\n" protocol-file
            (if (first args) "" " (Different can be passed as an argument.)"))
    (flush)
    (pprint (run (xml->map protocol-file)))))