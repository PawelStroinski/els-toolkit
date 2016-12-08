(ns els-toolkit.search
  (:require [primitive-math :as p]
            [els-toolkit.math :refer [div-and-ceil]])
  (:import [els_toolkit.search IndexOfWithSkip]))

(defn indexes-of [match s start skip]
  (loop [indexes [] from-index start]
    (let [index (IndexOfWithSkip/indexOf s match from-index skip)]
      (if (= index -1)
        indexes
        (recur (conj indexes index) (+ index skip))))))

(defn calculate-max-skip [^long word-len ^long text-len]
  (when (p/> word-len 1)
    (loop [how-many-words (p/div text-len word-len)]
      (if (p/== (div-and-ceil text-len (p/inc how-many-words))
                word-len)
        (recur (p/inc how-many-words))
        how-many-words))))

(defn max-skip-for-word [min-skip max-skip word text]
  (min max-skip
       (or (calculate-max-skip (count word) (count text))
           min-skip)))

(defn look-for-words [{:keys [min-skip max-skip]} words text]
  (for [word words
        skip (range min-skip (inc (max-skip-for-word min-skip max-skip word text)))
        start (range skip)
        i (indexes-of word text start skip)]
    {:word word :start i :skip skip}))

(defn look-for-synonyms [spec synonyms text]
  (let [found (look-for-words spec (flatten synonyms) text)
        found-words (set (map :word found))]
    (when (every? (partial some found-words) synonyms)
      found)))