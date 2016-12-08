(ns user1
  (:require [criterium.core :refer [bench quick-bench]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [els-toolkit.optimize :as o]
            [els-toolkit.math :as math]
            [els-toolkit.search :as search])
  (:import (els_toolkit.optimize MiniTable)))

(defn one-of-each2 [sets]
  (loop [ret [#{}]
         [s & s2] sets]
    (if s
      (recur
        (for [r ret
              e s]
          (conj r e))
        s2)
      (set ret))))
(defn one-of-each3 [sets]               ;; <- second fastest
  (reduce (fn [ret s]
            (for [r ret
                  e s]
              (cons e r)))
          '(()) sets))
(defn one-of-each4 [sets]
  (reduce (fn [ret s]
            (let [ret* (transient #{})]
              (doall (for [r ret
                           e s]
                       (conj! ret* (conj r e))))
              (persistent! ret*)))
          #{#{}} sets))
(defn one-of-each5 [sets]               ;; <- fastest
  (reduce (fn [ret s]
            (let [ret* (transient [])]
              (reduce (fn [_ r]
                        (reduce (fn [_ e]
                                  (conj! ret* (cons e r))
                                  nil)
                                nil s))
                      nil ret)
              (persistent! ret*)))
          ['()] sets))
(defn one-of-each5-fix [sets]           ; fix to 5, as should have used returned transient after each change, http://stackoverflow.com/questions/12385651/clojure-transients-usage
  (reduce
    (fn [rets one-set]
      (persistent!
        (reduce
          (fn [new-rets one-ret]
            (reduce
              (fn [new-rets-inner element]
                (conj! new-rets-inner (cons element one-ret)))
              new-rets
              one-set))
          (transient [])
          rets)))
    ['()]
    sets))
(defmacro one-of-each6 [sets]
  (let [sets (eval sets)
        syms (apply hash-map (mapcat (fn [s] [s (gensym)]) sets))]
    `(for [~@(mapcat (fn [s] [(syms s) s]) sets)] (list ~@(vals syms)))))
(defn cart [colls]                      ; http://stackoverflow.com/a/18248031
  (if (empty? colls)
    '(())
    (for [x (first colls)
          more (cart (rest colls))]
      (cons x more))))
(defn cartesian-product                 ; https://github.com/clojure/math.combinatorics/blob/master/src/main/clojure/clojure/math/combinatorics.clj
  "All the ways to take one item from each sequence"
  [& seqs]
  (let [v-original-seqs (vec seqs)
        step
        (fn step [v-seqs]
          (let [increment
                (fn [v-seqs]
                  (loop [i (dec (count v-seqs)), v-seqs v-seqs]
                    (if (= i -1) nil
                                 (if-let [rst (next (v-seqs i))]
                                   (assoc v-seqs i rst)
                                   (recur (dec i) (assoc v-seqs i (v-original-seqs i)))))))]
            (when v-seqs
              (cons (map first v-seqs)
                    (lazy-seq (step (increment v-seqs)))))))]
    (when (every? seq seqs)
      (lazy-seq (step v-original-seqs)))))
(defn cross-prod [colls]                ; https://groups.google.com/forum/#!topic/trifunc/SLOGZoITB2U
  (->> colls
       (reduce #(for [x %1 y %2] [x y]))
       (map flatten)))

(comment
  (def data (doall (map set (partition 4 (range 40)))))
  (defn sum [colls] (reduce + (map (partial reduce +) colls)))
  (quick-bench (sum (one-of-each3 data)))
  (quick-bench (sum (one-of-each5 data)))
  (quick-bench (sum (one-of-each5-fix data)))
  (quick-bench (sum (one-of-each6 data)))
  (quick-bench (sum (cart data)))
  (quick-bench (sum (apply cartesian-product data)))
  (quick-bench (sum (cross-prod data)))
  (one-of-each5 [#{1 2} #{3 4 5}])
  (one-of-each [#{1 2} #{3 4 5}])
  (cart [#{1 2} #{3 4 5}])
  (def counts [2 3 4 5])
  (def adjectives ["sweet" "ugly"])
  (def animals ["cats" "dogs" "hogs"])
  (defn sets [colls]
    (set
      (for [c colls]
        (set c))))
  (= (sets (o/cartesian-product [counts adjectives animals]))
     (sets (cart [counts adjectives animals]))
     (sets (cartesian-product counts adjectives animals))
     (sets (cross-prod [counts adjectives animals])))
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn text-with-skips [start skip text]
  (->> (drop start text)
       (partition 1 skip)
       (flatten)
       (apply str)))
(defn text-with-skips2 [start skip text] ; <- second fastest
  (->> (drop start text)
       (partition 1 skip)
       (apply concat)
       (apply str)))
(defn text-with-skips3 [start skip text]
  (->> (drop start text)
       (partition 1 skip)
       (apply concat)
       (into-array Character/TYPE)
       (String.)))
(defn text-with-skips4 [start skip text] ; <- second fastest
  (->> (drop start text)
       (partition 1 skip)
       (apply concat)
       (clojure.string/join)))
(defn text-with-skips-size [start skip text]
  (let [c (- (.length text) start)]
    (+ (quot c skip)
       (if (pos? (rem c skip)) 1 0))))
(defn text-with-skips5 [start skip text] ; <- fastest
  (let [size (text-with-skips-size start skip text)
        max-i (dec size)
        ret (transient [])]
    (loop [i 0
           j start]
      (conj! ret (nth text j))
      (if (= i max-i)
        (clojure.string/join (persistent! ret))
        (recur (inc i) (+ j skip))))))
(defn text-with-skips6 [start skip text]
  (let [size (text-with-skips-size start skip text)
        max-i (dec size)
        ret (transient (vec (repeat size nil)))]
    (loop [i 0
           j start]
      (assoc! ret i (nth text j))
      (if (= i max-i)
        (clojure.string/join (persistent! ret))
        (recur (inc i) (+ j skip))))))
(defn text-with-skips7 [start skip text] ; <- fastest
  (let [ret (transient [])
        len (.length text)]
    (loop [i start]
      (if (>= i len)
        (apply str (persistent! ret))
        (do (conj! ret (nth text i))
            (recur (+ i skip)))))))
(comment
  (defn rand-char []
    (char (+ 65 (rand-int 26))))
  (def data (clojure.string/join (repeatedly 300000 rand-char)))
  (quick-bench (text-with-skips 0 1 data))
  (quick-bench (text-with-skips2 0 1 data))
  (quick-bench (text-with-skips3 0 1 data))
  (quick-bench (text-with-skips4 0 1 data))
  (quick-bench (text-with-skips5 0 1 data))
  (quick-bench (text-with-skips6 0 1 data))
  (quick-bench (text-with-skips7 0 1 data))
  (def tmp (for [start (range 5)
                 skip (range 1 500)]
             (let [expe (text-with-skips start skip data)
                   actu (text-with-skips7 start skip data)]
               (= expe actu))))
  (every? true? tmp)
  (= (text-with-skips 0 1 data) (text-with-skips7 0 1 data))
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti fisher–yates-shuffle2 (fn [arg _] (class arg)))

(defmethod fisher–yates-shuffle2 :default [coll distribution]
  "based on http://rosettacode.org/wiki/knuth_shuffle#clojure
  and http://en.wikipedia.org/wiki/fisher%e2%80%93yates_shuffle#the_modern_algorithm"
  (let [v (transient (vec coll))]
    (reduce (fn [_ i] (let [j (.nextIntFromTo distribution 0 i)]
                        (assoc! v i (v j) j (v i))
                        nil))
            nil
            (range (dec (count coll)) 0 -1))
    (persistent! v)))

(defmethod fisher–yates-shuffle2 String [s distribution]
  (clojure.string/join (fisher–yates-shuffle2 (seq s) distribution)))

(defn fisher–yates-shuffle3 [s distribution]
  "based on http://rosettacode.org/wiki/knuth_shuffle#clojure
  and http://en.wikipedia.org/wiki/fisher%e2%80%93yates_shuffle#the_modern_algorithm"
  (let [v (transient (vec s))]
    (reduce (fn [_ i] (let [j (.nextIntFromTo distribution 0 i)]
                        (assoc! v i (v j) j (v i))
                        nil))
            nil
            (range (dec (count v)) 0 -1))
    (clojure.string/join (persistent! v))))

(defn fisher–yates-shuffle3-fix [s distribution] ; fix to 3, as should have used returned transient after each change, http://stackoverflow.com/questions/12385651/clojure-transients-usage
  "based on http://rosettacode.org/wiki/knuth_shuffle#clojure
  and http://en.wikipedia.org/wiki/fisher%e2%80%93yates_shuffle#the_modern_algorithm"
  (clojure.string/join
    (persistent!
      (reduce (fn [v i] (let [j (.nextIntFromTo distribution 0 i)]
                          (assoc! v i (v j) j (v i))))
              (transient (vec s))
              (range (dec (count s)) 0 -1)))))

(comment
  (assert (string? data))
  (require '[els-toolkit.population :as popul])
  (def dist (popul/new-distribution 1))
  (quick-bench (popul/fisher–yates-shuffle data dist))
  (quick-bench (fisher–yates-shuffle2 data dist))
  (quick-bench (fisher–yates-shuffle3 data dist))
  (quick-bench (fisher–yates-shuffle3-fix data dist))
  (let [dist1 (popul/new-distribution 1)
        dist2 (popul/new-distribution 1)]
    (= (popul/fisher–yates-shuffle data dist1)
       (fisher–yates-shuffle2 data dist2)))
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(require '[clojure.math.numeric-tower :refer (ceil)])

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

; the following is same as the above but works around "cannot be cast to clojure.lang.IFn$OLO"
(defn best-table-for-els-in-cylinder-OLO [{:keys [start] :as els} ^long cylinder]
  (let [y (if (>= start cylinder) (quot start cylinder) 0)
        positions (map #(rem % cylinder) (els-letter-positions els))
        leftmost (apply min positions)
        rightmost (apply max positions)
        w (inc (- rightmost leftmost))
        h (long (ceil (/ (+ (first positions) (els-size els)) cylinder)))]
    {:x leftmost :y y :w w :h h}))

; . . . .

(require '[primitive-math :as p])

(defn long-min
  ([x] x)
  ([x y] (p/min ^long x ^long y))
  ([x y & more]
   (reduce long-min (p/min ^long x ^long y) more)))

(defn long-max
  ([x] x)
  ([x y] (p/max ^long x ^long y))
  ([x y & more]
   (reduce long-max (p/max ^long x ^long y) more)))

(defn div-and-ceil ^long [^long x ^long y]
  (let [d (p/div x y)
        r (p/rem x y)]
    (if (p/zero? r)
      d
      (p/inc d))))

(defn els-size2 ^long [{:keys [word ^long skip]}]
  (p/inc (p/* (dec (count word))
              skip)))

(defn els-letter-positions2 [{:keys [word ^long start ^long skip]}]
  (map (fn [^long x] (p/+ start (p/* x skip))) (range (count word))))

(defn best-table-for-els-in-cylinder2 [{:keys [^long start] :as els} ^long cylinder]
  (let [y (if (p/>= start cylinder) (p/div start cylinder) 0)
        positions (map #(p/rem % cylinder) (els-letter-positions2 els))
        ^long leftmost (apply long-min positions)
        ^long rightmost (apply long-max positions)
        w (p/inc (p/- rightmost leftmost))
        h (div-and-ceil (p/+ (^long first positions) (els-size2 els)) cylinder)]
    {:x leftmost :y y :w w :h h}))

; . . . .

(deftype MinMax [^long min ^long max])

(defn min-max [^MinMax r ^long x]
  (->MinMax (p/min (.min r) x) (p/max (.max r) x)))

(def min-max-init (->MinMax Long/MAX_VALUE Long/MIN_VALUE))

(defn min-max->vec [^MinMax r]
  [(.min r) (.max r)])

(defn els-size3 ^long [{:keys [word ^long skip]}]
  (p/inc (p/* (dec (count word))
              skip)))

(defn els-letter-positions3 [{:keys [^long start ^long skip]}]
  (map (fn [^long x] (p/+ start (p/* x skip)))))

(defn best-table-for-els-in-cylinder3 [{:keys [word ^long start] :as els} ^long cylinder]
  (let [y (if (p/>= start cylinder) (p/div start cylinder) 0)
        positions (comp (els-letter-positions3 els) (map #(p/rem % cylinder)))
        [^long leftmost ^long rightmost] (min-max->vec (transduce
                                                         positions
                                                         (completing min-max)
                                                         min-max-init
                                                         (range (count word))))
        [^long first-position] (into [] positions '(0))
        w (p/inc (p/- rightmost leftmost))
        h (div-and-ceil (p/+ first-position (els-size3 els)) cylinder)]
    {:x leftmost :y y :w w :h h}))

; . . . .

(deftype MinMax4 [^long min ^long max])

(defn min-max4
  ([]
   (->MinMax4 Long/MAX_VALUE Long/MIN_VALUE))
  ([^MinMax4 r]
   [(.min r) (.max r)])
  ([^MinMax4 r ^long x]
   (->MinMax4 (p/min (.min r) x) (p/max (.max r) x))))

(defn els-size4 ^long [{:keys [word ^long skip]}]
  (p/inc (p/* (dec (count word))
              skip)))

(defn els-letter-positions4 [{:keys [^long start ^long skip]}]
  (map (fn [^long x] (p/+ start (p/* x skip)))))

(defn best-table-for-els-in-cylinder4 [{:keys [word ^long start] :as els} ^long cylinder]
  (let [y (if (p/>= start cylinder) (p/div start cylinder) 0)
        positions (comp (els-letter-positions4 els) (map #(p/rem % cylinder)))
        [^long leftmost ^long rightmost] (transduce positions min-max4
                                                    (range (count word)))
        [^long first-position] (into [] positions '(0))
        w (p/inc (p/- rightmost leftmost))
        h (div-and-ceil (p/+ first-position (els-size4 els)) cylinder)]
    {:x leftmost :y y :w w :h h}))

; . . . .

(defn els-size5 ^long [{:keys [word ^long skip]}]
  (p/inc (p/* (p/dec (count word))
              skip)))

(defn els-letter-positions5 [{:keys [^long start ^long skip]}]
  (map #(p/+ start (p/* ^long % skip))))

(defn best-table-for-els-in-cylinder5 [{:keys [word ^long start] :as els} ^long cylinder]
  (let [y (if (p/>= start cylinder) (p/div start cylinder) 0)
        positions (comp (els-letter-positions5 els) (map #(p/rem % cylinder)))
        [^long leftmost ^long rightmost] (transduce positions min-max4
                                                    (range (count word)))
        [^long first-position] (into [] positions '(0))
        w (p/inc (p/- rightmost leftmost))
        h (div-and-ceil (p/+ first-position (els-size5 els)) cylinder)]
    {:x leftmost :y y :w w :h h}))

; . . . .

(defn sum-tables [tables]
  (letfn [(xywh [xy-key wh-key]
            (let [xy-val (apply min
                                (map xy-key tables))]
              {xy-key xy-val
               wh-key (- (apply max
                                (map #(+ (xy-key %) (wh-key %)) tables))
                         xy-val)}))]
    (merge (xywh :x :w) (xywh :y :h))))

; . . . .

(defn sum-tables2 [tables]              ; <- fastest
  (letfn [(xywh [xy-key wh-key]
            (let [[^long xy-val ^long wh-val]
                  (reduce (fn [[^long curr-xy-val ^long curr-wh-val] t]
                            (let [^long xy-val (xy-key t)
                                  ^long wh-val (wh-key t)]
                              [(p/min curr-xy-val xy-val)
                               (p/max curr-wh-val (p/+ xy-val wh-val))]))
                          [Long/MAX_VALUE Long/MIN_VALUE]
                          tables)]
              {xy-key xy-val
               wh-key (p/- wh-val xy-val)}))]
    (merge (xywh :x :w) (xywh :y :h))))

; . . . .

(deftype XYWH [^long xy ^long wh])

(defn sum-tables3 [tables]              ; <- same as or slower than 2
  (letfn [(xywh [xy-key wh-key]
            (let [^XYWH ret
                  (reduce (fn [^XYWH r t]
                            (let [^long xy-val (xy-key t)
                                  ^long wh-val (wh-key t)]
                              (->XYWH (p/min (.xy r) xy-val)
                                      (p/max (.wh r) (p/+ xy-val wh-val)))))
                          (->XYWH Long/MAX_VALUE Long/MIN_VALUE)
                          tables)]
              {xy-key (.xy ret)
               wh-key (p/- (.wh ret) (.xy ret))}))]
    (merge (xywh :x :w) (xywh :y :h))))

; . . . .

(defn table-area [{:keys [w h]}]
  (* w h))

;(defn best-table-iter [prev {:keys [elses cylinder] :as iter}]
;  (let [table (o/sum-tables (map #(o/best-table-for-els-in-cylinder % cylinder) elses))
;        area (table-area table)
;        prev-area (:area prev Double/POSITIVE_INFINITY)]
;    (if (or (< area prev-area)
;            (and (= area prev-area) (< cylinder (:cylinder prev))))
;      (assoc (merge table iter) :area area)
;      prev)))
;
;(defn best-table [elses synonyms max-cylinder]
;  (let [els-mixes (o/cartesian-product
;                    (vals (group-by (partial o/els-synonyms synonyms) elses)))
;        iters (for [els-mix els-mixes
;                    cylinder (o/cylinders els-mix max-cylinder)]
;                {:elses els-mix :cylinder cylinder})]
;    (reduce best-table-iter nil iters)))

; . . . .

;(defn best-table-iter2 [best-table-for-els-in-cylinder* prev {:keys [elses cylinder] :as iter}]
;  (let [table (o/sum-tables (map #(best-table-for-els-in-cylinder* % cylinder) elses))
;        area (table-area table)
;        prev-area (:area prev Double/POSITIVE_INFINITY)]
;    (if (or (< area prev-area)
;            (and (= area prev-area) (< cylinder (:cylinder prev))))
;      (assoc (merge table iter) :area area)
;      prev)))
;
;(defn best-table2 [elses synonyms max-cylinder] ; <- faster
;  (let [els-mixes (o/cartesian-product
;                    (vals (group-by (partial o/els-synonyms synonyms) elses)))
;        iters (for [els-mix els-mixes
;                    cylinder (o/cylinders els-mix max-cylinder)]
;                {:elses els-mix :cylinder cylinder})]
;    (reduce (partial best-table-iter2 (memoize o/best-table-for-els-in-cylinder))
;            nil iters)))

; . . . .

(defn sum-tables [tables]
  (letfn [(xywh [xy-key wh-key]
            (let [[^long xy-val ^long wh-val]
                  (reduce (fn [[^long curr-xy-val ^long curr-wh-val] t]
                            (let [^long xy-val (xy-key t)
                                  ^long wh-val (wh-key t)]
                              [(p/min curr-xy-val xy-val)
                               (p/max curr-wh-val (p/+ xy-val wh-val))]))
                          [Long/MAX_VALUE Long/MIN_VALUE]
                          tables)]
              {xy-key xy-val
               wh-key (p/- wh-val xy-val)}))]
    (merge (xywh :x :w) (xywh :y :h))))

(defn best-table-iter
  [best-table-for-els-in-cylinder* prev {:keys [elses cylinder] :as iter}]
  (let [table (sum-tables (map #(best-table-for-els-in-cylinder* % cylinder) elses))
        area (table-area table)
        prev-area (:area prev Double/POSITIVE_INFINITY)]
    (if (or (< area prev-area)
            (and (= area prev-area) (< cylinder (:cylinder prev))))
      (assoc (merge table iter) :area area)
      prev)))

; . . . .

(defn sum-tables-2 [tables elses]
  (letfn [(xywh [^long curr-xy1 ^long curr-xy2 ^long xy ^long wh]
            [(p/min curr-xy1 xy)
             (p/max curr-xy2 (p/+ xy wh))])]
    (transduce
      tables
      (fn
        ([]
         [Long/MAX_VALUE Long/MIN_VALUE Long/MAX_VALUE Long/MIN_VALUE])
        ([[^long x1 ^long x2 ^long y1 ^long y2]]
         {:x x1 :w (p/- x2 x1) :y y1 :h (p/- y2 y1)})
        ([[x1 x2 y1 y2] t]
         (concat (xywh x1 x2 (:x t) (:w t))
                 (xywh y1 y2 (:y t) (:h t)))))
      elses)))

(defn best-table-iter-2                 ; <- slow
  [best-table-for-els-in-cylinder* prev {:keys [elses cylinder] :as iter}]
  (let [table (sum-tables-2 (map #(best-table-for-els-in-cylinder* % cylinder)) elses)
        area (table-area table)
        prev-area (:area prev Double/POSITIVE_INFINITY)]
    (if (or (< area prev-area)
            (and (= area prev-area) (< cylinder (:cylinder prev))))
      (assoc (merge table iter) :area area)
      prev)))

; . . . .

(defn sum-tables-3 [tables]
  (letfn [(xywh [^long curr-xy1 ^long curr-xy2 ^long xy ^long wh]
            [(p/min curr-xy1 xy)
             (p/max curr-xy2 (p/+ xy wh))])]
    (let [[^long x1 ^long x2 ^long y1 ^long y2]
          (reduce
            (fn [[x1 x2 y1 y2] t]
              (concat (xywh x1 x2 (:x t) (:w t))
                      (xywh y1 y2 (:y t) (:h t))))
            [Long/MAX_VALUE Long/MIN_VALUE Long/MAX_VALUE Long/MIN_VALUE]
            tables)]
      {:x x1 :w (p/- x2 x1) :y y1 :h (p/- y2 y1)})))

; . . . .

(deftype Table [^long x1 ^long x2 ^long y1 ^long y2])

(defn sum-tables-4 [tables]
  (let [^Table ret
        (reduce
          (fn [^Table r {:keys [^long x ^long y ^long w ^long h]}]
            (->Table (p/min (.x1 r) x)
                     (p/max (.x2 r) (p/+ x w))
                     (p/min (.y1 r) y)
                     (p/max (.y2 r) (p/+ y h))))
          (->Table Long/MAX_VALUE Long/MIN_VALUE Long/MAX_VALUE Long/MIN_VALUE)
          tables)]
    {:x (.x1 ret)
     :w (p/- (.x2 ret) (.x1 ret))
     :y (.y1 ret)
     :h (p/- (.y2 ret) (.y1 ret))}))

; . . . .

(defn sum-tables-5 [tables elses]
  (transduce
    tables
    (fn
      ([^Table r]
       {:x (.x1 r)
        :w (p/- (.x2 r) (.x1 r))
        :y (.y1 r)
        :h (p/- (.y2 r) (.y1 r))})
      ([^Table r {:keys [^long x ^long y ^long w ^long h]}]
       (->Table (p/min (.x1 r) x)
                (p/max (.x2 r) (p/+ x w))
                (p/min (.y1 r) y)
                (p/max (.y2 r) (p/+ y h)))))
    (->Table Long/MAX_VALUE Long/MIN_VALUE Long/MAX_VALUE Long/MIN_VALUE)
    elses))

(defn best-table-iter-5                 ; <- fastest
  [best-table-for-els-in-cylinder* prev {:keys [elses cylinder] :as iter}]
  (let [table (sum-tables-5 (map #(best-table-for-els-in-cylinder* % cylinder)) elses)
        area (table-area table)
        prev-area (:area prev Double/POSITIVE_INFINITY)]
    (if (or (< area prev-area)
            (and (= area prev-area) (< cylinder (:cylinder prev))))
      (assoc (merge table iter) :area area)
      prev)))

; . . . .

(defn sum-tables-6 [best-table-for-els-in-cylinder* elses cylinder]
  (let [^Table ret
        (reduce
          (fn [^Table r els]
            (let [{:keys [^long x ^long y ^long w ^long h]}
                  (best-table-for-els-in-cylinder* els cylinder)]
              (->Table (p/min (.x1 r) x)
                       (p/max (.x2 r) (p/+ x w))
                       (p/min (.y1 r) y)
                       (p/max (.y2 r) (p/+ y h)))))
          (->Table Long/MAX_VALUE Long/MIN_VALUE Long/MAX_VALUE Long/MIN_VALUE)
          elses)]
    {:x (.x1 ret)
     :w (p/- (.x2 ret) (.x1 ret))
     :y (.y1 ret)
     :h (p/- (.y2 ret) (.y1 ret))}))

(defn best-table-iter-6                 ; <- slightly slower than 5
  [best-table-for-els-in-cylinder* prev {:keys [elses cylinder] :as iter}]
  (let [table (sum-tables-6 best-table-for-els-in-cylinder* elses cylinder)
        area (table-area table)
        prev-area (:area prev Double/POSITIVE_INFINITY)]
    (if (or (< area prev-area)
            (and (= area prev-area) (< cylinder (:cylinder prev))))
      (assoc (merge table iter) :area area)
      prev)))

; . . . .

(deftype Table2 [^long x1 ^long x2 ^long y1 ^long y2 ^long area])

(defn sum-tables-7 [tables elses ^long prev-area]
  (transduce
    tables
    (fn
      ([^Table2 r]
       (when r
         {:x    (.x1 r)
          :w    (p/- (.x2 r) (.x1 r))
          :y    (.y1 r)
          :h    (p/- (.y2 r) (.y1 r))
          :area (.area r)}))
      ([^Table2 r {:keys [^long x ^long y ^long w ^long h]}]
       (let [x1 (p/min (.x1 r) x)
             x2 (p/max (.x2 r) (p/+ x w))
             y1 (p/min (.y1 r) y)
             y2 (p/max (.y2 r) (p/+ y h))
             area (p/* (p/- x2 x1)
                       (p/- y2 y1))]
         (if (p/> area prev-area)
           (reduced nil)
           (->Table2 x1 x2 y1 y2 area)))))
    (->Table2 Long/MAX_VALUE Long/MIN_VALUE Long/MAX_VALUE Long/MIN_VALUE 0)
    elses))

(defn best-table-iter-7                 ; <- fastest, faster than 5
  [best-table-for-els-in-cylinder* prev {:keys [elses cylinder] :as iter}]
  (let [prev-area (:area prev Long/MAX_VALUE)
        table (sum-tables-7 (map #(best-table-for-els-in-cylinder* % cylinder)) elses prev-area)
        area (:area table Long/MAX_VALUE)]
    (if (or (< area prev-area)
            (and (= area prev-area) (< cylinder (:cylinder prev))))
      (merge table iter)
      prev)))

; . . . .

(defn best-table-2 [elses synonyms max-cylinder]
  (let [els-mixes (o/cartesian-product
                    (vals (group-by (partial o/els-synonyms synonyms) elses)))
        iters (for [els-mix els-mixes
                    cylinder (o/cylinders els-mix max-cylinder)]
                {:elses els-mix :cylinder cylinder})]
    (reduce (partial o/best-table-iter (memoize o/best-table-for-els-in-cylinder))
            nil iters)))

(defn volatile-memoize [f]
  (let [mem (volatile! {})]
    (fn [& args]
      (if-let [e (find @mem args)]
        (val e)
        (let [ret (apply f args)]
          (vswap! mem assoc args ret)
          ret)))))

(defn best-table-3 [elses synonyms max-cylinder] ; <- not faster than 2
  (let [els-mixes (o/cartesian-product
                    (vals (group-by (partial o/els-synonyms synonyms) elses)))
        iters (for [els-mix els-mixes
                    cylinder (o/cylinders els-mix max-cylinder)]
                {:elses els-mix :cylinder cylinder})]
    (reduce (partial o/best-table-iter (volatile-memoize o/best-table-for-els-in-cylinder))
            nil iters)))

(defn memoize-best-table-for-els-in-cylinder [max-cylinder]
  (let [mem (atom (vec (repeat (inc max-cylinder) {})))]
    (fn [els cylinder]
      (let [m (nth @mem cylinder)]
        (if-let [e (find m els)]
          (val e)
          (let [ret (o/best-table-for-els-in-cylinder els cylinder)]
            (swap! mem assoc cylinder
                   (assoc m els ret))
            ret))))))

(defn best-table-4 [elses synonyms max-cylinder] ; <- faster than 2
  (let [els-mixes (o/cartesian-product
                    (vals (group-by (partial o/els-synonyms synonyms) elses)))
        iters (for [els-mix els-mixes
                    cylinder (o/cylinders els-mix max-cylinder)]
                {:elses els-mix :cylinder cylinder})]
    (reduce (partial o/best-table-iter (memoize-best-table-for-els-in-cylinder max-cylinder)) ; problem: max-cylinder can be Double/POSITIVE_INFINITY!
            nil iters)))

(defn best-table-4-fix [elses synonyms max-cylinder] ; <- faster than 4
  (let [cylidners (o/cylinders elses max-cylinder)
        els-mixes (o/cartesian-product
                    (vals (group-by (partial o/els-synonyms synonyms) elses)))
        iters (for [els-mix els-mixes
                    cylinder (o/cylinders els-mix max-cylinder)]
                {:elses els-mix :cylinder cylinder})]
    (reduce (partial o/best-table-iter (memoize-best-table-for-els-in-cylinder (count cylidners)))
            nil iters)))

(defn memoize-best-table-for-els-in-cylinder-two-maps []
  (let [mem (atom {})]
    (fn [els cylinder]
      (let [m (@mem cylinder {})]
        (if-let [e (find m els)]
          (val e)
          (let [ret (o/best-table-for-els-in-cylinder els cylinder)]
            (swap! mem assoc cylinder
                   (assoc m els ret))
            ret))))))

(defn best-table-4-two-maps [elses synonyms max-cylinder] ; <- slower than 4
  (let [els-mixes (o/cartesian-product
                    (vals (group-by (partial o/els-synonyms synonyms) elses)))
        iters (for [els-mix els-mixes
                    cylinder (o/cylinders els-mix max-cylinder)]
                {:elses els-mix :cylinder cylinder})]
    (reduce (partial o/best-table-iter (memoize-best-table-for-els-in-cylinder-two-maps))
            nil iters)))

(defn memoize-best-table-for-els-in-cylinder-one-array [^long cylinders-count ^long elses-count]
  (let [mem (atom (vec (repeat (p/* cylinders-count elses-count) nil)))]
    (fn [{:keys [^long id] :as els} ^long cylinder]
      (let [i (p/+ (p/* (p/dec cylinder) elses-count) id)]
        (if-let [v (nth @mem i)]
          v
          (let [ret (o/best-table-for-els-in-cylinder els cylinder)]
            (swap! mem assoc i ret)
            ret))))))

(defn best-table-4-one-array [elses synonyms max-cylinder] ; <- faster than 4-fix
  (let [elses (map-indexed (fn [i m] (assoc m :id i)) elses)
        cylidners (o/cylinders elses max-cylinder)
        els-mixes (o/cartesian-product
                    (vals (group-by (partial o/els-synonyms synonyms) elses)))
        iters (for [els-mix els-mixes
                    cylinder (o/cylinders els-mix max-cylinder)]
                {:elses els-mix :cylinder cylinder})]
    (reduce (partial o/best-table-iter (memoize-best-table-for-els-in-cylinder-one-array (count cylidners) (count elses)))
            nil iters)))

; . . . .

(defn sum-tables-8 [tables ^long prev-area]
  (let [^Table2 r
        (reduce
          (fn [^Table2 r {:keys [^long x ^long y ^long w ^long h]}]
            (let [x1 (p/min (.x1 r) x)
                  x2 (p/max (.x2 r) (p/+ x w))
                  y1 (p/min (.y1 r) y)
                  y2 (p/max (.y2 r) (p/+ y h))
                  area (p/* (p/- x2 x1)
                            (p/- y2 y1))]
              (if (p/> area prev-area)
                (reduced nil)
                (->Table2 x1 x2 y1 y2 area))))
          (->Table2 Long/MAX_VALUE Long/MIN_VALUE Long/MAX_VALUE Long/MIN_VALUE 0)
          tables)]
    (when r
      {:x    (.x1 r)
       :w    (p/- (.x2 r) (.x1 r))
       :y    (.y1 r)
       :h    (p/- (.y2 r) (.y1 r))
       :area (.area r)})))

(defn best-table-iter-8 [prev {:keys [elses cylinder] :as iter}]
  (let [prev-area (:area prev Long/MAX_VALUE)
        table (sum-tables-8 (map #(get-in % [:tables cylinder]) elses) prev-area)
        area (:area table Long/MAX_VALUE)]
    (if (or (< area prev-area)
            (and (= area prev-area) (< cylinder (:cylinder prev))))
      (merge table iter)
      prev)))

(defn tables [els cylinders]
  (reduce (fn [r c]
            (assoc r c (o/best-table-for-els-in-cylinder els c)))
          {}
          cylinders))

(defn best-table-8 [elses synonyms max-cylinder] ; <- slower than 4
  (let [cylinders (o/cylinders elses max-cylinder)
        elses (map #(assoc % :tables (tables % cylinders)) elses)
        els-mixes (o/cartesian-product
                    (vals (group-by (partial o/els-synonyms synonyms) elses)))
        iters (for [els-mix els-mixes
                    cylinder (o/cylinders els-mix max-cylinder)]
                {:elses els-mix :cylinder cylinder})]
    (reduce best-table-iter-8 nil iters)))

; . . . .

(defn sum-tables-9 [tables elses ^long prev-area]
  (transduce
    tables
    (fn
      ([^Table2 r]
       (if r
         (.area r)
         Long/MAX_VALUE))
      ([^Table2 r {:keys [^long x ^long y ^long w ^long h]}]
       (let [x1 (p/min (.x1 r) x)
             x2 (p/max (.x2 r) (p/+ x w))
             y1 (p/min (.y1 r) y)
             y2 (p/max (.y2 r) (p/+ y h))
             area (p/* (p/- x2 x1)
                       (p/- y2 y1))]
         (if (p/>= area prev-area)
           (reduced nil)
           (->Table2 x1 x2 y1 y2 area)))))
    (->Table2 Long/MAX_VALUE Long/MIN_VALUE Long/MAX_VALUE Long/MIN_VALUE 0)
    elses))

(defn best-table-iter-9
  [best-table-for-els-in-cylinder* ^long prev-area {:keys [elses cylinder] :as iter}]
  (let [^long area (sum-tables-9 (map #(best-table-for-els-in-cylinder* % cylinder)) elses prev-area)]
    (if (p/< area prev-area)
      area
      prev-area)))

(defn best-table-9 [elses synonyms max-cylinder] ; <- not faster than best-table-4-one-array
  (let [elses (map-indexed (fn [i m] (assoc m :id i)) elses)
        cylidners (o/cylinders elses max-cylinder)
        els-mixes (o/cartesian-product
                    (vals (group-by (partial o/els-synonyms synonyms) elses)))
        iters (for [els-mix els-mixes
                    cylinder (o/cylinders els-mix max-cylinder)]
                {:elses els-mix :cylinder cylinder})]
    (reduce (partial best-table-iter-9 (o/memoize-best-table-for-els-in-cylinder
                                         (count cylidners) (count elses)))
            Long/MAX_VALUE iters)))

; . . . .

(defn sum-tables-10 [tables ^long prev-area]
  (let [^Table2 r (reduce
                    (fn [^Table2 r {:keys [^long x ^long y ^long w ^long h]}]
                      (let [x1 (p/min (.x1 r) x)
                            x2 (p/max (.x2 r) (p/+ x w))
                            y1 (p/min (.y1 r) y)
                            y2 (p/max (.y2 r) (p/+ y h))
                            area (p/* (p/- x2 x1)
                                      (p/- y2 y1))]
                        (if (p/> area prev-area)
                          (reduced nil)
                          (->Table2 x1 x2 y1 y2 area))))
                    (->Table2 Long/MAX_VALUE Long/MIN_VALUE Long/MAX_VALUE Long/MIN_VALUE 0)
                    tables)]
    (when r
      {:x    (.x1 r)
       :w    (p/- (.x2 r) (.x1 r))
       :y    (.y1 r)
       :h    (p/- (.y2 r) (.y1 r))
       :area (.area r)})))

(defn best-table-iter-10                ; <- slower than best-table-4-one-array
  [best-table-for-els-in-cylinder* prev {:keys [elses cylinder] :as iter}]
  (let [prev-area (:area prev Long/MAX_VALUE)
        table (sum-tables-10 (map #(best-table-for-els-in-cylinder* % cylinder) elses) prev-area)
        area (:area table Long/MAX_VALUE)]
    (if (or (< area prev-area)
            (and (= area prev-area) (< cylinder (:cylinder prev))))
      (merge table iter)
      prev)))

; . . . .

(defn best-table-11 [elses synonyms max-cylinder] ; <- slower than best-table-4-one-array
  (let [elses (map-indexed (fn [i m] (assoc m :id i)) elses)
        cylidners (o/cylinders elses max-cylinder)
        els-mixes (o/cartesian-product
                    (vals (group-by (partial o/els-synonyms synonyms) elses)))
        iters (mapcat (fn [els-mix]
                        (map (fn [cylinder]
                               {:elses els-mix :cylinder cylinder})
                             (o/cylinders els-mix max-cylinder))))
        best-table-for-els-in-cylinder* (memoize-best-table-for-els-in-cylinder-one-array
                                          (count cylidners) (count elses))]
    (transduce
      iters
      (completing
        (fn
          [prev {:keys [elses cylinder] :as iter}]
          (let [prev-area (:area prev Long/MAX_VALUE)
                table (o/sum-tables (map #(best-table-for-els-in-cylinder* % cylinder)) elses prev-area)
                area (:area table Long/MAX_VALUE)]
            (if (or (< area prev-area)
                    (and (= area prev-area) (< cylinder (:cylinder prev))))
              (merge table iter)
              prev))))
      nil
      els-mixes)))

; . . . .

(def ^:dynamic *best-table-for-els-in-cylinder*)

(defn best-table-iter-12 [prev {:keys [elses cylinder] :as iter}]
  (let [prev-area (:area prev Long/MAX_VALUE)
        table (o/sum-tables (map #(*best-table-for-els-in-cylinder* % cylinder)) elses prev-area)
        area (:area table Long/MAX_VALUE)]
    (if (or (< area prev-area)
            (and (= area prev-area) (< cylinder (:cylinder prev))))
      (merge table iter)
      prev)))

(defn best-table-12 [elses synonyms max-cylinder] ; <- slower than best-table-11
  (let [elses (map-indexed (fn [i m] (assoc m :id i)) elses)
        cylidners (o/cylinders elses max-cylinder)
        els-mixes (o/cartesian-product
                    (vals (group-by (partial o/els-synonyms synonyms) elses)))
        iters (mapcat (fn [els-mix]
                        (map (fn [cylinder]
                               {:elses els-mix :cylinder cylinder})
                             (o/cylinders els-mix max-cylinder))))]
    (binding [*best-table-for-els-in-cylinder*
              (memoize-best-table-for-els-in-cylinder-one-array
                (count cylidners) (count elses))]
      (transduce
        iters
        (completing best-table-iter-12)
        nil
        els-mixes))))

; . . . .

(defn best-table-13 [elses synonyms max-cylinder] ; <- slower than best-table-11
  (let [elses (map-indexed (fn [i m] (assoc m :id i)) elses)
        cylidners (o/cylinders elses max-cylinder)
        els-mixes (o/cartesian-product
                    (vals (group-by (partial o/els-synonyms synonyms) elses)))
        iters (for [els-mix els-mixes
                    cylinder (o/cylinders els-mix max-cylinder)]
                {:elses els-mix :cylinder cylinder})
        best-table-for-els-in-cylinder* (memoize-best-table-for-els-in-cylinder-one-array
                                          (count cylidners) (count elses))]
    (reduce
      (fn
        [prev {:keys [elses cylinder] :as iter}]
        (let [prev-area (:area prev Long/MAX_VALUE)
              table (o/sum-tables (map #(best-table-for-els-in-cylinder* % cylinder)) elses prev-area)
              area (:area table Long/MAX_VALUE)]
          (if (or (< area prev-area)
                  (and (= area prev-area) (< cylinder (:cylinder prev))))
            (merge table iter)
            prev)))
      nil
      iters)))

; . . . .

(defn best-table-14 [elses synonyms max-cylinder] ; <- slow
  (let [elses (map-indexed (fn [i m] (assoc m :id i)) elses)
        cylidners (o/cylinders elses max-cylinder)
        els-mixes (o/cartesian-product
                    (vals (group-by (partial o/els-synonyms synonyms) elses)))
        iters (mapcat (fn [els-mix]
                        (map (fn [cylinder]
                               {:elses els-mix :cylinder cylinder})
                             (o/cylinders els-mix max-cylinder))))]
    (transduce
      iters
      (completing
        (partial o/best-table-iter (o/memoize-best-table-for-els-in-cylinder
                                     (count cylidners) (count elses))))
      nil
      els-mixes)))

; . . . .


(defn cylinders [elses max-cylinder]
  (let [calculated-max (- (apply max
                                 (map #(+ (:start %) (o/els-size %)) elses))
                          (apply min
                                 (map :start elses)))]
    (range 1 (inc (min max-cylinder calculated-max)))))

(defn cylinders-2 [elses ^long max-cylinder]
  (let [^long min-start (reduce
                          (fn [^long r {:keys [^long start]}]
                            (p/min r start))
                          Long/MAX_VALUE
                          elses)
        calculated-max (reduce
                         (fn [^long r {:keys [^long start] :as els}]
                           (let [cylinder (p/- (p/+ start (o/els-size els))
                                               min-start)]
                             (if (p/>= cylinder max-cylinder)
                               (reduced max-cylinder)
                               (p/max r cylinder))))
                         Long/MIN_VALUE
                         elses)]
    (range 1 (inc calculated-max))))

; . . . .

(defn best-table-for-els-in-cylinder-11-current
  [{:keys [word ^long start] :as els} ^long cylinder]
  (let [y (if (p/>= start cylinder) (p/div start cylinder) 0)
        positions (comp (o/els-letter-positions' els) (map #(p/rem % cylinder)))
        [^long leftmost ^long rightmost] (transduce positions math/min-max
                                                    (range (count word)))
        [^long first-position] (into [] positions '(0))
        w (p/inc (p/- rightmost leftmost))
        h (div-and-ceil (p/+ first-position (o/els-size els)) cylinder)]
    {:x leftmost :y y :w w :h h}))

(defn sum-tables-11-current [tables elses ^long prev-area]
  (transduce
    tables
    (fn
      ([^Table2 r]
       (when r
         {:x    (.x1 r)
          :w    (p/- (.x2 r) (.x1 r))
          :y    (.y1 r)
          :h    (p/- (.y2 r) (.y1 r))
          :area (.area r)}))
      ([^Table2 r {:keys [^long x ^long y ^long w ^long h]}]
       (let [x1 (p/min (.x1 r) x)
             x2 (p/max (.x2 r) (p/+ x w))
             y1 (p/min (.y1 r) y)
             y2 (p/max (.y2 r) (p/+ y h))
             area (p/* (p/- x2 x1)
                       (p/- y2 y1))]
         (if (p/> area prev-area)
           (reduced nil)
           (->Table2 x1 x2 y1 y2 area)))))
    (->Table2 Long/MAX_VALUE Long/MIN_VALUE Long/MAX_VALUE Long/MIN_VALUE 0)
    elses))

; . . . .

(deftype MiniTable1 [^long x1 ^long x2 ^long y1 ^long y2])

(defn best-table-for-els-in-cylinder-11-updated
  [{:keys [word ^long start] :as els} ^long cylinder]
  (let [y (if (p/>= start cylinder) (p/div start cylinder) 0)
        positions (comp (o/els-letter-positions' els) (map #(p/rem % cylinder)))
        [^long leftmost ^long rightmost] (transduce positions math/min-max
                                                    (range (count word)))
        [^long first-position] (into [] positions '(0))
        h (div-and-ceil (p/+ first-position (o/els-size els)) cylinder)]
    (->MiniTable1 leftmost (p/inc rightmost) y (p/+ y h))))

(defn sum-tables-11-updated [tables elses ^long prev-area] ; <- faster
  (transduce
    tables
    (fn
      ([^Table2 r]
       (when r
         {:x    (.x1 r)
          :w    (p/- (.x2 r) (.x1 r))
          :y    (.y1 r)
          :h    (p/- (.y2 r) (.y1 r))
          :area (.area r)}))
      ([^Table2 r ^MiniTable1 in]
       (let [x1 (p/min (.x1 r) (.x1 in))
             x2 (p/max (.x2 r) (.x2 in))
             y1 (p/min (.y1 r) (.y1 in))
             y2 (p/max (.y2 r) (.y2 in))
             area (p/* (p/- x2 x1)
                       (p/- y2 y1))]
         (if (p/> area prev-area)
           (reduced nil)
           (->Table2 x1 x2 y1 y2 area)))))
    (->Table2 Long/MAX_VALUE Long/MIN_VALUE Long/MAX_VALUE Long/MIN_VALUE 0)
    elses))

; . . . .

(defn sum-tables-11-updated-no-reduced [tables elses ^long _] ; <- slower
  (transduce
    tables
    (fn
      ([^MiniTable1 r]
       (when r
         (let [w (p/- (.x2 r) (.x1 r))
               h (p/- (.y2 r) (.y1 r))]
           {:x    (.x1 r)
            :w    w
            :y    (.y1 r)
            :h    h
            :area (p/* w h)})))
      ([^MiniTable1 r ^MiniTable1 in]
       (let [x1 (p/min (.x1 r) (.x1 in))
             x2 (p/max (.x2 r) (.x2 in))
             y1 (p/min (.y1 r) (.y1 in))
             y2 (p/max (.y2 r) (.y2 in))]
         (->MiniTable1 x1 x2 y1 y2))))
    (->MiniTable1 Long/MAX_VALUE Long/MIN_VALUE Long/MAX_VALUE Long/MIN_VALUE)
    elses))

; . . . .

(defn sum-tables-15-current [tables elses ^long prev-area]
  (transduce
    tables
    (fn
      ([^Table2 r]
       (when r
         {:x    (.x1 r)
          :w    (p/- (.x2 r) (.x1 r))
          :y    (.y1 r)
          :h    (p/- (.y2 r) (.y1 r))
          :area (.area r)}))
      ([^Table2 r ^MiniTable in]
       (let [x1 (p/min (.x1 r) (.x1 in))
             x2 (p/max (.x2 r) (.x2 in))
             y1 (p/min (.y1 r) (.y1 in))
             y2 (p/max (.y2 r) (.y2 in))
             area (p/* (p/- x2 x1)
                       (p/- y2 y1))]
         (if (p/> area prev-area)
           (reduced nil)
           (->Table2 x1 x2 y1 y2 area)))))
    (->Table2 Long/MAX_VALUE Long/MIN_VALUE Long/MAX_VALUE Long/MIN_VALUE 0)
    elses))

(defn best-table-iter-15-current
  [best-table-for-els-in-cylinder* prev {:keys [elses cylinder] :as iter}]
  (let [prev-area (:area prev Long/MAX_VALUE)
        table (sum-tables-15-current (map #(best-table-for-els-in-cylinder* % cylinder)) elses prev-area)
        area (:area table Long/MAX_VALUE)]
    (if (or (< area prev-area)
            (and (= area prev-area) (< cylinder (:cylinder prev))))
      (merge table iter)
      prev)))

(defn best-table-15-current [elses synonyms max-cylinder]
  (let [elses (map-indexed (fn [i m] (assoc m :id i)) elses)
        cylidners (o/cylinders elses max-cylinder)
        els-mixes (o/cartesian-product
                    (vals (group-by (partial o/els-synonyms synonyms) elses)))
        iters (for [els-mix els-mixes
                    cylinder (o/cylinders els-mix max-cylinder)]
                {:elses els-mix :cylinder cylinder})]
    (reduce (partial best-table-iter-15-current (o/memoize-best-table-for-els-in-cylinder
                                                  (count cylidners) (count elses)))
            nil iters)))

; . . . .

(defn sum-tables-15-updated [tables elses ^long prev-area]
  (transduce
    tables
    (fn
      ([r] r)
      ([^Table2 r ^MiniTable in]
       (let [x1 (p/min (.x1 r) (.x1 in))
             x2 (p/max (.x2 r) (.x2 in))
             y1 (p/min (.y1 r) (.y1 in))
             y2 (p/max (.y2 r) (.y2 in))
             area (p/* (p/- x2 x1)
                       (p/- y2 y1))
             sum (->Table2 x1 x2 y1 y2 area)]
         (if (p/> area prev-area)
           (reduced sum)
           sum))))
    (->Table2 Long/MAX_VALUE Long/MIN_VALUE Long/MAX_VALUE Long/MIN_VALUE 0)
    elses))

(deftype IterRet [^Table2 table iter])

(defn best-table-iter-15-updated
  [best-table-for-els-in-cylinder* ^IterRet prev {:keys [elses cylinder] :as iter}]
  (let [^Table2 prev-table (.table prev)
        prev-iter (.iter prev)
        prev-area (.area prev-table)
        ^Table2 table (sum-tables-15-updated (map #(best-table-for-els-in-cylinder* % cylinder)) elses prev-area)
        area (.area table)]
    (if (or (p/< area prev-area)
            (and (p/== area prev-area) (p/< ^long cylinder ^long (:cylinder prev-iter))))
      (->IterRet table iter)
      prev)))

(defn IterRet->map [^IterRet r]
  (let [^Table2 table (.table r)
        iter (.iter r)]
    (merge
      {:x    (.x1 table)
       :w    (p/- (.x2 table) (.x1 table))
       :y    (.y1 table)
       :h    (p/- (.y2 table) (.y1 table))
       :area (.area table)}
      iter)))

(defn best-table-15-updated [elses synonyms max-cylinder]
  (let [elses (map-indexed (fn [i m] (assoc m :id i)) elses)
        cylidners (o/cylinders elses max-cylinder)
        els-mixes (o/cartesian-product
                    (vals (group-by (partial o/els-synonyms synonyms) elses)))
        iters (for [els-mix els-mixes
                    cylinder (o/cylinders els-mix max-cylinder)]
                {:elses els-mix :cylinder cylinder})]
    (IterRet->map
      (reduce (partial best-table-iter-15-updated (o/memoize-best-table-for-els-in-cylinder
                                                    (count cylidners) (count elses)))
              (->IterRet (->Table2 0 0 0 0 Long/MAX_VALUE) nil) iters))))

; . . . .

(comment
  (do
    (def elses1 [{:word "uuu" :start 30 :skip 7}
                 {:word "ww" :start 90 :skip 2}
                 {:word "uuu" :start 3 :skip 7}
                 {:word "ww" :start 9 :skip 2}
                 {:word "jjjj" :start 1 :skip 7}
                 {:word "wwww" :start 4 :skip 7}
                 {:word "iii" :start 12 :skip 7}
                 {:word "jjjj" :start 10 :skip 8}
                 {:word "wwww" :start 40 :skip 7}
                 {:word "iii" :start 120 :skip 7}])
    (defn shift [x els]
      (update els :start + x))
    (def elses (vec (mapcat (fn [x] (map (partial shift x) elses1)) (range 3))))
    (def synonyms [["uuu"] ["ww"] ["jjjj"] ["wwww"] ["iii"]])
    (def max-cylinder 40))
  (do
    (dotimes [_ 3]
      ;(println "\n\nDoing best-table in 1 sec\n")
      ;(Thread/sleep 1000)
      ;(quick-bench (best-table elses synonyms max-cylinder))
      ;(println "\n\nDoing best-table2 in 1 sec\n")
      ;(Thread/sleep 1000)
      ;(quick-bench (best-table2 elses synonyms max-cylinder))
      ;(println "\n\nDoing best-table-iter in 1 sec\n")
      ;(Thread/sleep 1000)
      ;(with-redefs [o/best-table-iter best-table-iter]
      ;  (bench (o/best-table elses synonyms max-cylinder)))
      ;(println "\n\nDoing best-table-iter-2 in 1 sec\n")
      ;(Thread/sleep 1000)
      ;(with-redefs [o/best-table-iter best-table-iter-2]
      ;  (bench (o/best-table elses synonyms max-cylinder)))
      ;(println "\n\nDoing sum-tables-3 in 1 sec\n")
      ;(Thread/sleep 1000)
      ;(with-redefs [o/sum-tables sum-tables-3]
      ;  (bench (o/best-table elses synonyms max-cylinder)))
      ;(println "\n\nDoing sum-tables-4 in 1 sec\n")
      ;(Thread/sleep 1000)
      ;(with-redefs [o/sum-tables sum-tables-4]
      ;  (bench (o/best-table elses synonyms max-cylinder)))
      ;(println "\n\nDoing best-table-iter-5 in 1 sec\n")
      ;(Thread/sleep 1000)
      ;(with-redefs [o/best-table-iter best-table-iter-5]
      ;  (bench (o/best-table elses synonyms max-cylinder)))
      ;(println "\n\nDoing best-table-iter-6 in 1 sec\n")
      ;(Thread/sleep 1000)
      ;(with-redefs [o/best-table-iter best-table-iter-6]
      ;  (bench (o/best-table elses synonyms max-cylinder)))
      ;(println "\n\nDoing best-table-iter-7 in 1 sec\n")
      ;(Thread/sleep 1000)
      ;(with-redefs [o/best-table-iter best-table-iter-7]
      ;  (bench (o/best-table elses synonyms max-cylinder)))
      ;(println "\n\nDoing best-table-2 in 1 sec\n")
      ;(Thread/sleep 1000)
      ;(quick-bench (best-table-2 elses synonyms max-cylinder))
      ;(println "\n\nDoing best-table-3 in 1 sec\n")
      ;(Thread/sleep 1000)
      ;(bench (best-table-3 elses synonyms max-cylinder))
      ;(println "\n\nDoing best-table-4 in 1 sec\n")
      ;(Thread/sleep 1000)
      ;(quick-bench (best-table-4 elses synonyms max-cylinder))
      ;(println "\n\nDoing best-table-4-fix in 1 sec\n")
      ;(Thread/sleep 1000)
      ;(bench (best-table-4-fix elses synonyms max-cylinder))
      ;(println "\n\nDoing best-table-4-two-maps in 1 sec\n")
      ;(Thread/sleep 1000)
      ;(quick-bench (best-table-4-two-maps elses synonyms max-cylinder))
      #_(do (println "\n\nDoing best-table-4-one-array in 1 sec\n")
            (Thread/sleep 1000)
            (bench (best-table-4-one-array elses synonyms max-cylinder)))
      ;(println "\n\nDoing best-table-8 in 1 sec\n")
      ;(Thread/sleep 1000)
      ;(bench (best-table-8 elses synonyms max-cylinder))
      ;(println "\n\nDoing best-table-9 in 1 sec\n")
      ;(Thread/sleep 1000)
      ;(bench (best-table-9 elses synonyms max-cylinder))
      #_(do (println "\n\nDoing best-table-iter-10 in 1 sec\n")
            (Thread/sleep 1000)
            (with-redefs [o/best-table-iter best-table-iter-10]
              (bench (o/best-table elses synonyms max-cylinder))))
      #_(do (println "\n\nDoing best-table-11 in 1 sec\n")
            (Thread/sleep 1000)
            (bench (best-table-11 elses synonyms max-cylinder)))
      #_(do (println "\n\nDoing best-table-12 in 1 sec\n")
            (Thread/sleep 1000)
            (bench (best-table-12 elses synonyms max-cylinder)))
      #_(do (println "\n\nDoing best-table-13 in 1 sec\n")
            (Thread/sleep 1000)
            (bench (best-table-13 elses synonyms max-cylinder)))
      #_(do (println "\n\nDoing best-table-14 in 1 sec\n")
            (Thread/sleep 1000)
            (bench (best-table-14 elses synonyms max-cylinder)))
      #_(with-redefs [o/cylinders cylinders]
          (println "\n\nDoing cylinders in 1 sec\n")
          (Thread/sleep 1000)
          (bench (o/best-table elses synonyms max-cylinder)))
      #_(with-redefs [o/cylinders cylinders-2]
          (println "\n\nDoing cylinders-2 in 1 sec\n")
          (Thread/sleep 1000)
          (bench (o/best-table elses synonyms max-cylinder)))
      #_(with-redefs [o/best-table-for-els-in-cylinder best-table-for-els-in-cylinder-11-current
                      o/sum-tables sum-tables-11-current]
          (println "\n\nDoing sum-tables-11-current in 1 sec\n")
          (Thread/sleep 1000)
          (bench (o/best-table elses synonyms max-cylinder)))
      #_(with-redefs [o/best-table-for-els-in-cylinder best-table-for-els-in-cylinder-11-updated
                      o/sum-tables sum-tables-11-updated]
          (println "\n\nDoing sum-tables-11-updated in 1 sec\n")
          (Thread/sleep 1000)
          (bench (o/best-table elses synonyms max-cylinder)))
      #_(with-redefs [o/best-table-for-els-in-cylinder best-table-for-els-in-cylinder-11-updated
                      o/sum-tables sum-tables-11-updated-no-reduced]
          (println "\n\nDoing sum-tables-11-updated-no-reduced in 1 sec\n")
          (Thread/sleep 1000)
          (bench (o/best-table elses synonyms max-cylinder)))
      #_(do (println "\n\nDoing best-table-15-current in 1 sec\n")
            (Thread/sleep 1000)
            (bench (best-table-15-current elses synonyms max-cylinder)))
      (do (println "\n\nDoing best-table-15-updated in 1 sec\n")
          (Thread/sleep 1000)
          (bench (best-table-15-updated elses synonyms max-cylinder)))
      )
    (println "Bye"))
  (do
    (dotimes [_ 5]
      ;(println "\n\nDoing best-table-for-els-in-cylinder5 in 1 sec\n")
      ;(Thread/sleep 1000)
      ;(with-redefs [o/best-table-for-els-in-cylinder best-table-for-els-in-cylinder5]
      ;  (o/best-table elses synonyms max-cylinder)
      ;  ;(bench (o/best-table elses synonyms max-cylinder))
      ;  )
      ;(println "\n\nDoing best-table-for-els-in-cylinder4 in 1 sec\n")
      ;(Thread/sleep 1000)
      ;(with-redefs [o/best-table-for-els-in-cylinder best-table-for-els-in-cylinder4]
      ;  ;(o/best-table elses synonyms max-cylinder)
      ;  (bench (o/best-table elses synonyms max-cylinder))
      ;  )n
      ;(println "\n\nDoing best-table-for-els-in-cylinder3 in 1 sec\n")
      ;(Thread/sleep 1000)
      ;(with-redefs [o/best-table-for-els-in-cylinder best-table-for-els-in-cylinder3]
      ;  ;(o/best-table elses synonyms max-cylinder)
      ;  (bench (o/best-table elses synonyms max-cylinder))
      ;  )
      ;(println "\n\nDoing best-table-for-els-in-cylinder2 in 1 sec\n")
      ;(Thread/sleep 1000)
      ;(with-redefs [o/best-table-for-els-in-cylinder best-table-for-els-in-cylinder2]
      ;  ;(o/best-table elses synonyms max-cylinder)
      ;  (bench (o/best-table elses synonyms max-cylinder))
      ;  )
      ;(println "\n\nDoing best-table-for-els-in-cylinder in 1 sec\n")
      ;(Thread/sleep 1000)
      ;(with-redefs [o/best-table-for-els-in-cylinder best-table-for-els-in-cylinder-OLO]
      ;  (o/best-table elses synonyms max-cylinder)
      ;  ;(bench (o/best-table elses synonyms max-cylinder))
      ;  )
      ;;;;;;
      ;(println "\n\nDoing sum-tables in 1 sec\n")
      ;(Thread/sleep 1000)
      ;(with-redefs [o/sum-tables sum-tables]
      ;  ;(o/best-table elses synonyms max-cylinder)
      ;  (bench (o/best-table elses synonyms max-cylinder))
      ;  )
      ;(println "\n\nDoing sum-tables2 in 1 sec\n")
      ;(Thread/sleep 1000)
      ;(with-redefs [o/sum-tables sum-tables2]
      ;  ;(o/best-table elses synonyms max-cylinder)
      ;  (bench (o/best-table elses synonyms max-cylinder))
      ;  )
      ;(println "\n\nDoing sum-tables3 in 1 sec\n")
      ;(Thread/sleep 1000)
      ;(with-redefs [o/sum-tables sum-tables3]
      ;  ;(o/best-table elses synonyms max-cylinder)
      ;  (bench (o/best-table elses synonyms max-cylinder))
      ;  )
      )
    (println "Bye"))
  (let [nums (doall (repeatedly 100000 #(long (rand-int 1000000))))]
    (dotimes [_ 2]
      (println "\n\nDoing apply min in 1 sec\n")
      (Thread/sleep 1000)
      (bench (apply min nums))
      (println "\n\nDoing apply long-min in 1 sec\n")
      (Thread/sleep 1000)
      (bench (apply long-min nums)))
    (println "Bye"))
  (let [nums (doall (repeatedly 100000 #(long (rand-int 1000000))))]
    ;(defn min-max [[^long r-min ^long r-max] ^long x]
    ;  [(p/min r-min x) (p/max r-max x)])
    ;(deftype MinMax [min max])
    ;(defn min-max-2 [r ^long x]
    ;  (->MinMax (p/min ^long (.min ^MinMax r) x) (p/max ^long (.max ^MinMax r) x)))
    (dotimes [_ 2]
      ;(println "\n\nDoing min-max-2 in 1 sec\n")
      ;(Thread/sleep 1000)
      ;(bench (reduce min-max-2 (->MinMax Long/MAX_VALUE Long/MIN_VALUE) nums))
      ;(println "\n\nDoing min-max in 1 sec\n")
      ;(Thread/sleep 1000)
      ;(bench (reduce min-max [Long/MAX_VALUE Long/MIN_VALUE] nums))
      (println "\n\nDoing apply long-min & long-max in 1 sec\n")
      (Thread/sleep 1000)
      (bench [(apply long-min nums) (apply long-max nums)])
      (println "\n\nDoing min-max in 1 sec\n")
      (Thread/sleep 1000)
      (bench (min-max->vec (reduce min-max min-max-init nums))))
    (println "Bye"))
  (def div-and-ceil-prop
    (prop/for-all [x (gen/such-that (comp not neg?) gen/large-integer 1000)
                   y gen/s-pos-int]
                  (= (long (ceil (/ x y)))
                     (div-and-ceil x y))))
  (tc/quick-check 100000 div-and-ceil-prop)
  (def best-table-for-els-in-cylinder-prop
    (prop/for-all [max-cylinder (gen/choose 1 1000)
                   xs (gen/vector gen/pos-int 4)]
                  (let [elses (mapcat (fn [[start skip]]
                                        (map (fn [els]
                                               (-> els
                                                   (update :start + start)
                                                   (update :skip + skip)))
                                             elses1))
                                      (partition 2 xs))
                        ;expected (with-redefs [o/best-table-for-els-in-cylinder best-table-for-els-in-cylinder-OLO]
                        ;           (o/best-table elses synonyms max-cylinder))
                        ;actual (o/best-table elses synonyms max-cylinder)
                        expected (with-redefs [o/best-table-for-els-in-cylinder best-table-for-els-in-cylinder-11-current
                                               o/sum-tables sum-tables-11-current]
                                   (o/best-table elses synonyms max-cylinder))
                        actual (with-redefs [o/best-table-for-els-in-cylinder best-table-for-els-in-cylinder-11-updated
                                             o/sum-tables sum-tables-11-updated]
                                 (o/best-table elses synonyms max-cylinder))]
                    (= expected actual))))
  (time (tc/quick-check 1000 best-table-for-els-in-cylinder-prop))
  ;(defrecord ELS [word start skip])
  ;(def elses-recs (mapv map->ELS elses))
  ;(bench (o/best-table elses-recs synonyms max-cylinder)) ; <- no difference
  )

(defn part [l h]
  (/ (- h l) h))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn indexes-of [match s]
  (loop [indexes [] from-index 0]
    (let [index (.indexOf s match from-index)]
      (if (= index -1)
        indexes
        (recur (conj indexes index) (inc index))))))

(defn text-with-skips-current [start skip text]
  (let [ret (transient [])
        len (.length text)]
    (loop [i start]
      (if (>= i len)
        (apply str (persistent! ret))
        (do (conj! ret (nth text i))
            (recur (+ i skip)))))))

(defn look-for-words [{:keys [min-skip max-skip]} words text]
  (let [text-with-skips-memo (memoize text-with-skips-current)]
    (flatten
      (for [word words]
        (map (fn [skip]
               (for [start (range skip)]
                 (map #(-> {:word word :start (+ start (* % skip)) :skip skip})
                      (indexes-of word (text-with-skips-memo start skip text)))))
             (range min-skip (inc (search/max-skip-for-word min-skip max-skip word text))))))))

(import [els_toolkit.search IndexOfWithSkip])

(defn indexes-of-2 [match s start skip]
  (loop [indexes [] from-index start]
    (let [index (IndexOfWithSkip/indexOf s match from-index skip)]
      (if (= index -1)
        indexes
        (recur (conj indexes index) (+ index skip))))))

(defn look-for-words-2 [{:keys [min-skip max-skip]} words text]
  (flatten
    (for [word words]
      (map (fn [skip]
             (for [start (range skip)]
               (map #(-> {:word word :start % :skip skip})
                    (indexes-of-2 word text start skip))))
           (range min-skip (inc (search/max-skip-for-word min-skip max-skip word text)))))))

(defn indexes-of-3 [match s start skip]
  (let [indexes (transient [])]
    (loop [from-index start]
      (let [index (IndexOfWithSkip/indexOf s match from-index skip)]
        (if (= index -1)
          (persistent! indexes)
          (do (conj! indexes index)
              (recur (+ index skip))))))))

(defn look-for-words-3 [{:keys [min-skip max-skip]} words text]
  (for [word words
        skip (range min-skip (inc (search/max-skip-for-word min-skip max-skip word text)))
        start (range skip)
        i (indexes-of-3 word text start skip)]
    {:word word :start i :skip skip}))

(defn look-for-words-2+ [{:keys [min-skip max-skip]} words text] ; <- fastest or as fast as 3
  (for [word words
        skip (range min-skip (inc (search/max-skip-for-word min-skip max-skip word text)))
        start (range skip)
        i (indexes-of-2 word text start skip)]
    {:word word :start i :skip skip}))

(comment
  (do
    (defn rand-char []
      (char (+ 65 (rand-int 26))))
    (def data (clojure.string/join (repeatedly 300000 rand-char)))
    (def args [{:min-skip 1, :max-skip 150} ["ABCD" "EFGHI" "JKLMN" "OPRSTUW" "XYZ"] data]))
  (= (doall (apply look-for-words args))
     (doall (apply look-for-words-2+ args)))
  (bench (doall (apply look-for-words args)))
  (bench (doall (apply look-for-words-2 args)))
  (bench (doall (apply look-for-words-3 args)))
  (bench (doall (apply look-for-words-2+ args)))
  (def look-for-words-prop
    (prop/for-all [d (gen/fmap clojure.string/join (gen/vector gen/char-alphanumeric 100 1000))
                   w (gen/not-empty (gen/vector (gen/not-empty gen/string-alphanumeric)))
                   [m n] (gen/such-that (fn [[m n]] (< m n)) (gen/vector gen/s-pos-int 2) 100)]
                  (let [args [{:min-skip m, :max-skip n} w d]]
                    (= (apply look-for-words args)
                       (apply look-for-words-2+ args)))))
  (tc/quick-check 10000 look-for-words-prop)
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn calculate-max-skip [word-len text-len]
  (when (> word-len 1)
    (loop [how-many-words (quot text-len word-len)]
      (if (= (ceil (/ text-len (inc how-many-words)))
             word-len)
        (recur (inc how-many-words))
        how-many-words))))

(defn calculate-max-skip-2 [^long word-len ^long text-len] ; <- faster
  (when (p/> word-len 1)
    (loop [how-many-words (p/div text-len word-len)]
      (if (p/== (div-and-ceil text-len (p/inc how-many-words))
                word-len)
        (recur (p/inc how-many-words))
        how-many-words))))

(comment
  (quick-bench (calculate-max-skip 2 84))
  (quick-bench (calculate-max-skip-2 2 84)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (require '[clojure.core.reducers :as r])
  (do
    (def threads (atom #{"init"}))
    (r/fold (fn
              ([] 0)
              ([r x]
               (swap! threads conj (.. Thread currentThread getName))
               (+ r x)))
            (doall (range 0 10000)))
    @threads)
  (require '[com.climate.claypoole :as cp]
           '[com.climate.claypoole.lazy :as lazy])
  (do
    (def log-atom (atom []))
    (defn log [s]
      (swap! log-atom conj (str s "\t\t" (.. Thread currentThread getName))))
    (defn my-range
      ([]
       (my-range 0 10000))
      ([start n]
       (lazy-seq
         (when (< start n)
           (log (str "range\t" start))
           (cons
             start
             (my-range (inc start) n))))))
    (def ncpus (cp/ncpus))
    (time
      (reduce
        (fn [r x]
          (log (str "reduce\t" x))
          (+ r x))
        0
        (let [f (fn [x]
                  (if (odd? x)
                    (Thread/sleep 10))
                  (log (str "f\t\t" x))
                  x)]
          #_(cp/upmap ncpus f (my-range))
          #_(cp/upfor ncpus [x (my-range)] (f x))
          #_(pmap f (my-range))
          (lazy/upmap ncpus f (my-range)))))
    (println (clojure.string/join "\n" @log-atom)))
  )
