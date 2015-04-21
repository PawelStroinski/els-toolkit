(ns els-toolkit.population
  (:require [els-toolkit.optimize :refer (els-size els-letter-positions)]
            [clojure.string :refer (join)]
            [taoensso.timbre :as timbre])
  (:import org.apache.mahout.math.jet.random.Uniform))
(timbre/refer-timbre)

(defmulti fisher–yates-shuffle (fn [arg _] (class arg)))

(defmethod fisher–yates-shuffle :default [coll distribution]
  "Based on http://rosettacode.org/wiki/Knuth_shuffle#Clojure
  and http://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle#The_modern_algorithm"
  (reduce (fn [v i] (let [j (.nextIntFromTo distribution 0 i)]
                      (assoc v i (v j) j (v i))))
          (vec coll)
          (range (dec (count coll)) 0 -1)))

(defmethod fisher–yates-shuffle String [s distribution]
  (join (fisher–yates-shuffle (seq s) distribution)))

(defn els-letters [els]
  (apply hash-map (interleave (els-letter-positions els)
                              (:word els))))

(def ^:dynamic els-letters-memo els-letters)

(defn one-els-conflicts? [l r]
  (let [l-letters (els-letters-memo l)
        r-letters (els-letters-memo r)]
    (some (fn [[left-pos left-letter]] (if-let [right-letter (r-letters left-pos)]
                                         (not= left-letter right-letter)))
          l-letters)))

(defn els-conflicts? [l rs]
  (some #(one-els-conflicts? l %) rs))

(defn elses-conflict? [elses]
  (binding [els-letters-memo (memoize els-letters)]
    (some #(els-conflicts? % elses) elses)))

(defn els-random-placement [elses text-len distribution]
  (trace "els-random-placement")
  (let [random (map #(assoc % :start (->> (- text-len (els-size %))
                                          (.nextIntFromTo distribution 0)))
                    elses)]
    (if (elses-conflict? random)
      (recur elses text-len distribution)
      random)))

(defn new-distribution [seed]
  (Uniform. 0.0 0.0 seed))