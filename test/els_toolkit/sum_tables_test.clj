(ns els-toolkit.sum-tables-test
  (:use els-toolkit.optimize expectations))

(defn conv [{:keys [x y w h]}]
  (->MiniTable x (+ x w) y (+ y h)))

(defn call [tables]
  (dissoc (sum-tables (map conv) tables Long/MAX_VALUE)
          :area))

(expect {:x 2 :y 2 :w 12 :h 6} (call [{:x 2 :y 2 :w 12 :h 6}]))

(expect {:x 2 :y 2 :w 14 :h 9} (call [{:x 5 :y 7 :w 11 :h 4}
                                      {:x 2 :y 2 :w 12 :h 6}]))

(expect {:x 0 :y 0 :w 11 :h 20} (call [{:x 1 :y 2 :w 10 :h 5}
                                       {:x 1 :y 2 :w 5 :h 5}
                                       {:x 0 :y 0 :w 5 :h 20}]))