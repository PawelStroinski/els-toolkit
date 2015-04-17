(ns els-toolkit.optimize
  (:require [clojure.math.numeric-tower :refer (ceil)]))

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

(defn cylinders [elses]
  (let [max-cylinder (apply max
                            (map #(+ (:start %) (els-size %)) elses))]
    (range 1 (inc max-cylinder))))

(defn table-area [{:keys [w h]}]
  (* w h))

(defn best-table [elses synonyms]
  (let [els-mixes (one-of-each (vals (group-by (partial els-synonyms synonyms) elses)))
        tables (for [els-mix els-mixes
                     cylinder (cylinders els-mix)]
                 (assoc (sum-tables (map #(best-table-for-els-in-cylinder % cylinder) els-mix))
                   :cylinder cylinder :elses els-mix))]
    (first (sort-by table-area (sort-by :cylinder tables)))))