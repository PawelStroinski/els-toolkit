(ns els-toolkit.math
  (:require [primitive-math :as p]))

(deftype MinMax [^long min ^long max])

(defn min-max
  ([]
   (->MinMax Long/MAX_VALUE Long/MIN_VALUE))
  ([^MinMax r]
   [(.min r) (.max r)])
  ([^MinMax r ^long x]
   (->MinMax (p/min (.min r) x) (p/max (.max r) x))))

(defn div-and-ceil ^long [^long x ^long y]
  (let [d (p/div x y)
        r (p/rem x y)]
    (if (p/zero? r)
      d
      (p/inc d))))
