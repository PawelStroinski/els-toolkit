(ns els-toolkit.core
  (:require [els-toolkit.optimize :refer (best-table)]
            [els-toolkit.population :refer
             [new-distribution fisher–yates-shuffle fisher–yates-shuffle-randoms
              els-random-placement]]
            [els-toolkit.protocol :refer (xml->map)]
            [els-toolkit.search :refer (look-for-synonyms)]
            [clojure.pprint :refer (pprint)]
            [taoensso.timbre :as timbre :refer [may-log?]]
            [clojure.core.async :refer (chan <!! >!! pipeline close! thread)]
            [progrock.core :as pr :refer [progress-bar tick]])
  (:gen-class))
(timbre/refer-timbre)

(def nthreads (.availableProcessors (Runtime/getRuntime)))

(defn find-table [protocol text]
  (when-let [elses (look-for-synonyms protocol (:synonyms protocol) text)]
    (best-table elses (:synonyms protocol) (:max-cylinder protocol))))

(defn same-or-better-iter [table c area]
  (if (>= (:area table) area)
    (inc c)
    c))

(defn finalize [{:keys [text-population]} table areas-from-shuffled]
  (let [print-bar (not (may-log? :trace))
        ret (reduce
              (fn [{:keys [same-or-better bar]} area]
                (let [bar (tick bar)]
                  (when print-bar
                    (pr/print bar))
                  {:same-or-better (same-or-better-iter table same-or-better area)
                   :bar            bar}))
              {:same-or-better 0
               :bar            (-> text-population progress-bar tick)}
              areas-from-shuffled)]
    (newline) (newline) ret))

(defn seq!! [ch]
  (lazy-seq
    (when-some [v (<!! ch)]
      (cons v (seq!! ch)))))

(defn populate-randoms [ch protocol]
  (let [distribution (new-distribution (:seed protocol))
        input-length (count (:text protocol))]
    (dotimes [_ (dec (:text-population protocol))]
      (>!! ch (fisher–yates-shuffle-randoms distribution input-length))))
  (close! ch))

(defn populate-shuffled-elses [ch protocol elses]
  (let [text-len (count (:text protocol))
        distribution (new-distribution (:seed protocol))]
    (dotimes [_ (dec (:text-population protocol))]
      (>!! ch (els-random-placement elses text-len distribution))))
  (close! ch))

(defn format-result [table {:keys [same-or-better]}]
  {:table          (update table :elses (comp set (partial map #(dissoc % :id))))
   :same-or-better same-or-better})

(defmulti run :shuffling)

(defmethod run :letter-shuffling [protocol]
  (when-let [table (find-table protocol (:text protocol))]
    (let [randoms (chan (* 20 nthreads))
          areas-from-shuffled (chan nthreads)
          xf (map #(or (:area (find-table protocol
                                          (fisher–yates-shuffle (:text protocol) %)))
                       Long/MAX_VALUE))
          finalize-ret (thread (finalize protocol table (seq!! areas-from-shuffled)))]
      (pipeline nthreads areas-from-shuffled xf randoms)
      (populate-randoms randoms protocol)
      (format-result table (<!! finalize-ret)))))

(defmethod run :els-random-placement [protocol]
  (when-let [elses (doall (look-for-synonyms protocol (:synonyms protocol) (:text protocol)))]
    (trace (count elses) elses)
    (let [shuffled-elses (chan nthreads)
          areas-from-shuffled (chan nthreads)
          xf (map #(:area (best-table % (:synonyms protocol) (:max-cylinder protocol))))
          ret
          (thread
            (let [table (best-table elses (:synonyms protocol) (:max-cylinder protocol))]
              [table
               (finalize protocol table (seq!! areas-from-shuffled))]))]
      (pipeline nthreads areas-from-shuffled xf shuffled-elses)
      (populate-shuffled-elses shuffled-elses protocol elses)
      (apply format-result (<!! ret)))))

(defn -main [& args]
  (timbre/set-level! (keyword (nth args 1 "debug")))
  (let [protocol-file (or (first args) "protocol.xml")]
    (printf "\nUsing '%s' protocol file.%s\n\n" protocol-file
            (if (first args) "" " (A different one can be passed in as an argument.)"))
    (flush)
    (pprint (run (xml->map protocol-file))))
  (shutdown-agents))
