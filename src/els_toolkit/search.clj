(ns els-toolkit.search
  (:require [clojure.math.numeric-tower :refer (ceil)]))

(defn indexes-of [match s]
  (loop [indexes [] from-index 0]
    (let [index (.indexOf s match from-index)]
      (if (= index -1)
        indexes
        (recur (conj indexes index) (inc index))))))

(defn text-with-skips [start skip text]
  (->> (drop start text)
       (partition 1 skip)
       (flatten)
       (apply str)))

(def text-with-skips-memo (memoize text-with-skips))

(defn calculate-max-skip [word-len text-len]
  (when (> word-len 1)
    (loop [how-many-words (quot text-len word-len)]
      (if (= (ceil (/ text-len (inc how-many-words)))
             word-len)
        (recur (inc how-many-words))
        how-many-words))))

(defn max-skip-for-word [min-skip max-skip word text]
  (min max-skip
       (or (calculate-max-skip (count word) (count text))
           min-skip)))

(defn look-for-words [{:keys [min-skip max-skip]} words text]
  (flatten
    (for [word words
          skip (range min-skip (inc (max-skip-for-word min-skip max-skip word text)))
          start (range skip)]
      (map #(-> {:word word :start (+ start (* % skip)) :skip skip})
           (indexes-of word (text-with-skips-memo start skip text))))))

(defn look-for-synonyms [spec synonyms text]
  (let [found (look-for-words spec (flatten synonyms) text)
        found-words (set (map :word found))]
    (when (every? (partial some found-words) synonyms)
      found)))