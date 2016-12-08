(ns els-toolkit.population
  (:require [els-toolkit.optimize :refer (els-size els-letter-positions)]
            [clojure.string :refer (join)]
            [taoensso.timbre :as timbre]
            [primitive-math :as p])
  (:import [org.apache.mahout.math.jet.random Uniform]
           [els_toolkit.population FisherYatesShuffle]))
(timbre/refer-timbre)

(defn fisher–yates-shuffle-randoms [distribution input-length]
  (FisherYatesShuffle/makeRandoms distribution input-length))

(defn fisher–yates-shuffle [input randoms]
  (FisherYatesShuffle/shuffle input randoms))

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

(defn els-random-placement [elses text-len ^Uniform distribution]
  (letfn
    [(f []
       (let [random (map #(assoc % :start (->> (- text-len (els-size %))
                                               (.nextIntFromTo distribution 0)))
                         elses)]
         (if (elses-conflict? random)
           (recur)
           random)))]
    (trace "els-random-placement")
    (locking distribution
      (f))))

(defn new-distribution [^long seed]
  (Uniform. 0.0 0.0 seed))