(ns els-toolkit.optimize
  (:require [taoensso.timbre :as timbre]
            [primitive-math :as p]
            [els-toolkit.math :refer [min-max div-and-ceil]]))
(timbre/refer-timbre)

(defn els-size ^long [{:keys [word ^long skip]}]
  (p/inc (p/* (p/dec (count word))
              skip)))

(defn els-letter-positions' [{:keys [^long start ^long skip]}]
  (map #(p/+ start (p/* ^long % skip))))

(defn els-letter-positions [{:keys [word] :as els}]
  (sequence (els-letter-positions' els) (range (count word))))

(deftype MiniTable [^long x1 ^long x2 ^long y1 ^long y2])

(defn best-table-for-els-in-cylinder
  [{:keys [word ^long start] :as els} ^long cylinder]
  (let [y (if (p/>= start cylinder) (p/div start cylinder) 0)
        positions (comp (els-letter-positions' els) (map #(p/rem % cylinder)))
        [^long leftmost ^long rightmost] (transduce positions min-max
                                                    (range (count word)))
        [^long first-position] (into [] positions '(0))
        h (div-and-ceil (p/+ first-position (els-size els)) cylinder)]
    (->MiniTable leftmost (p/inc rightmost) y (p/+ y h))))

(defn cartesian-product [sets]
  (if (seq sets)
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
      sets)
    []))

(defn els-synonyms [synonyms {:keys [word]}]
  (first (filter #(some #{word} %)
                 synonyms)))

(deftype Table [^long x1 ^long x2 ^long y1 ^long y2 ^long area])

(defn sum-tables [tables elses ^long prev-area]
  (transduce
    tables
    (fn
      ([^Table r]
       (when r
         {:x    (.x1 r)
          :w    (p/- (.x2 r) (.x1 r))
          :y    (.y1 r)
          :h    (p/- (.y2 r) (.y1 r))
          :area (.area r)}))
      ([^Table r ^MiniTable in]
       (let [x1 (p/min (.x1 r) (.x1 in))
             x2 (p/max (.x2 r) (.x2 in))
             y1 (p/min (.y1 r) (.y1 in))
             y2 (p/max (.y2 r) (.y2 in))
             area (p/* (p/- x2 x1)
                       (p/- y2 y1))]
         (if (p/> area prev-area)
           (reduced nil)
           (->Table x1 x2 y1 y2 area)))))
    (->Table Long/MAX_VALUE Long/MIN_VALUE Long/MAX_VALUE Long/MIN_VALUE 0)
    elses))

(defn cylinders [elses max-cylinder]
  (let [calculated-max (- (apply max
                                 (map #(+ (:start %) (els-size %)) elses))
                          (apply min
                                 (map :start elses)))]
    (range 1 (inc (min max-cylinder calculated-max)))))

(defn best-table-iter
  [best-table-for-els-in-cylinder* prev {:keys [elses cylinder] :as iter}]
  (let [prev-area (:area prev Long/MAX_VALUE)
        table (sum-tables (map #(best-table-for-els-in-cylinder* % cylinder)) elses prev-area)
        area (:area table Long/MAX_VALUE)]
    (if (or (< area prev-area)
            (and (= area prev-area) (< cylinder (:cylinder prev))))
      (merge table iter)
      prev)))

(defn memoize-best-table-for-els-in-cylinder [^long cylinders-count ^long elses-count]
  (let [mem (atom (vec (repeat (p/* cylinders-count elses-count) nil)))]
    (fn [{:keys [^long id] :as els} ^long cylinder]
      (let [i (p/+ (p/* (p/dec cylinder) elses-count) id)]
        (if-let [v (nth @mem i)]
          v
          (let [ret (best-table-for-els-in-cylinder els cylinder)]
            (swap! mem assoc i ret)
            ret))))))

(defn best-table [elses synonyms max-cylinder]
  (trace "best-table" (count elses))
  (let [elses (map-indexed (fn [i m] (assoc m :id i)) elses)
        cylidners (cylinders elses max-cylinder)
        els-mixes (cartesian-product
                    (vals (group-by (partial els-synonyms synonyms) elses)))
        iters (for [els-mix els-mixes
                    cylinder (cylinders els-mix max-cylinder)]
                {:elses els-mix :cylinder cylinder})]
    (reduce (partial best-table-iter (memoize-best-table-for-els-in-cylinder
                                       (count cylidners) (count elses)))
            nil iters)))
