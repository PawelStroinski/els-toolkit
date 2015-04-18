(ns els-toolkit.optimize
  (:require [clojure.math.numeric-tower :refer (ceil)]
            [taoensso.timbre :as timbre]))
(timbre/refer-timbre)

(defn els-size [{:keys [word skip]}]
  (inc (* (dec (count word))
          skip)))

(defn els-letter-positions [{:keys [word start skip]}]
  (map #(+ start (* % skip)) (range (count word))))

(defn best-table-for-els-in-cylinder [{:keys [start] :as els} cylinder]
  (let [y (if (>= start cylinder) (quot start cylinder) 0)
        positions (map #(rem % cylinder) (els-letter-positions els))
        leftmost (apply min positions)
        rightmost (apply max positions)
        w (inc (- rightmost leftmost))
        h (long (ceil (/ (+ (first positions) (els-size els)) cylinder)))]
    {:x leftmost :y y :w w :h h}))

(defn one-of-each [sets]
  (if (seq sets)
    (loop [[set1 & next-sets] sets
           result [#{}]]
      (if set1
        (recur next-sets
               (mapcat (fn [result1]
                         (map #(conj result1 %) set1))
                       result))
        (set result)))
    #{}))

(defn els-synonyms [synonyms {:keys [word]}]
  (first (filter #(some #{word} %)
                 synonyms)))

(defn sum-tables [tables]
  (letfn [(xywh [xy-key wh-key]
                (let [xy-val (apply min
                                    (map xy-key tables))]
                  {xy-key xy-val
                   wh-key (- (apply max
                                    (map #(+ (xy-key %) (wh-key %)) tables))
                             xy-val)}))]
    (merge (xywh :x :w) (xywh :y :h))))

(defn cylinders [elses max-cylinder]
  (let [calculated-max (- (apply max
                                 (map #(+ (:start %) (els-size %)) elses))
                          (apply min
                                 (map :start elses)))]
    (range 1 (inc (min max-cylinder calculated-max)))))

(defn table-area [{:keys [w h]}]
  (* w h))

(defn best-table-iter [prev {:keys [elses cylinder] :as iter}]
  (let [table (sum-tables (map #(best-table-for-els-in-cylinder % cylinder) elses))
        area (table-area table)
        prev-area (:area prev Double/POSITIVE_INFINITY)]
    (if (or (< area prev-area)
            (and (= area prev-area) (< cylinder (:cylinder prev))))
      (assoc (merge table iter) :area area)
      prev)))

(defn best-table [elses synonyms max-cylinder]
  (trace "best-table" (count elses))
  (let [els-mixes (one-of-each (vals (group-by (partial els-synonyms synonyms) elses)))
        iters (for [els-mix els-mixes
                    cylinder (cylinders els-mix max-cylinder)]
                {:elses els-mix :cylinder cylinder})]
    (reduce best-table-iter nil iters)))