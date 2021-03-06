(ns els-toolkit.best-table-test
  (:use els-toolkit.optimize expectations))

(defn pre [actual]
  (-> actual
      (dissoc :area)
      (update :elses (comp set (partial map #(dissoc % :id))))))

; w
(expect '{:cylinder 1 :x 0 :y 0 :w 1 :h 1 :elses #{{:word "w" :start 0 :skip 1}}}
        (pre (best-table [{:word "w" :start 0 :skip 1}] [["w"]] 10)))

; -
; w
(expect '{:cylinder 1 :x 0 :y 1 :w 1 :h 1 :elses #{{:word "w" :start 1 :skip 1}}}
        (pre (best-table [{:word "w" :start 1 :skip 1}]
                         [["w"]] 10)))

; -w
; -w
(expect '{:cylinder 2 :x 1 :y 0 :w 1 :h 2 :elses #{{:word "ww" :start 1 :skip 2}}}
        (pre (best-table [{:word "ww" :start 0 :skip 4}
                          {:word "ww" :start 1 :skip 2}]
                         [["ww"]] 10)))

; w
; u
; w
; u
(expect '{:cylinder 1 :x 0 :y 0 :w 1 :h 4 :elses #{{:word "ww" :start 0 :skip 2}
                                                   {:word "uu" :start 1 :skip 2}}}
        (pre (best-table [{:word "ww" :start 0 :skip 2}
                          {:word "uu" :start 1 :skip 2}]
                         [["ww"] ["uu"]] 10)))

; wu-
; wu-
(expect '{:cylinder 3 :x 0 :y 0 :w 2 :h 2 :elses #{{:word "ww" :start 0 :skip 3}
                                                   {:word "uu" :start 1 :skip 3}}}
        (pre (best-table [{:word "ww" :start 0 :skip 3}
                          {:word "uu" :start 1 :skip 3}]
                         [["ww"] ["uu"]] 10)))

; -wu
; -wu
(expect '{:cylinder 3 :x 1 :y 0 :w 2 :h 2 :elses #{{:word "ww" :start 1 :skip 3}
                                                   {:word "uu" :start 2 :skip 3}}}
        (pre (best-table [{:word "ww" :start 1 :skip 3}
                          {:word "uu" :start 2 :skip 3}
                          {:word "ww" :start 10 :skip 3}
                          {:word "uu" :start 20 :skip 3}]
                         [["ww"] ["uu"]] 10)))

; -wu-
; -wu-
(expect '{:cylinder 4 :x 1 :y 0 :w 2 :h 2 :elses #{{:word "ww" :start 1 :skip 4}
                                                   {:word "uu" :start 2 :skip 4}}}
        (pre (best-table [{:word "ww" :start 1 :skip 4}
                          {:word "uu" :start 2 :skip 4}
                          {:word "ww" :start 1 :skip 10}
                          {:word "uu" :start 2 :skip 10}]
                         [["ww"] ["uu"]] 10)))

; ----
; -wu-
; -wu-
(expect '{:cylinder 4 :x 1 :y 1 :w 2 :h 2 :elses #{{:word "ww" :start 5 :skip 4}
                                                   {:word "uu" :start 6 :skip 4}}}
        (pre (best-table [{:word "ww" :start 21 :skip 4}
                          {:word "ww" :start 20 :skip 4}
                          {:word "ww" :start 5 :skip 4}
                          {:word "uu" :start 6 :skip 4}]
                         [["ww"] ["uu"]] 10)))

; -----
; -w-u-
; -w-u-
(expect '{:cylinder 5 :x 1 :y 1 :w 3 :h 2 :elses #{{:word "ww" :start 6 :skip 5}
                                                   {:word "uu" :start 8 :skip 5}}}
        (pre (best-table [{:word "ww" :start 16 :skip 5}
                          {:word "uu" :start 8 :skip 15}
                          {:word "ww" :start 6 :skip 5}
                          {:word "ww" :start 26 :skip 5}
                          {:word "uu" :start 8 :skip 35}
                          {:word "uu" :start 8 :skip 5}
                          {:word "uu" :start 8 :skip 25}
                          {:word "ww" :start 36 :skip 5}]
                         [["ww"] ["uu"]] 10)))

; ----
; ----
; --w-
; --u-
; --w-
; --u-
(expect '{:cylinder 4 :x 2 :y 2 :w 1 :h 4 :elses #{{:word "ww" :start 10 :skip 8}
                                                   {:word "uu" :start 14 :skip 8}}}
        (pre (best-table [{:word "ww" :start 10 :skip 8}
                          {:word "ww" :start 1000 :skip 100}
                          {:word "uu" :start 1400 :skip 100}
                          {:word "uu" :start 14 :skip 8}]
                         [["ww"] ["uu"]] 10)))

; wu-
; -w-
; u--
(expect '{:cylinder 3 :x 0 :y 0 :w 2 :h 3 :elses #{{:word "ww" :start 0 :skip 4}
                                                   {:word "uu" :start 1 :skip 5}}}
        (pre (best-table [{:word "ww" :start 0 :skip 4}
                          {:word "ww" :start 0 :skip 14}
                          {:word "ww" :start 0 :skip 15}
                          {:word "ww" :start 0 :skip 16}
                          {:word "ww" :start 0 :skip 17}
                          {:word "uu" :start 1 :skip 5}]
                         [["ww"] ["uu"]] 10)))

; w---
; w---
; ----
; u---
; u---
(expect '{:cylinder 4 :x 0 :y 0 :w 1 :h 5 :elses #{{:word "ww" :start 0 :skip 4}
                                                   {:word "uu" :start 12 :skip 4}}}
        (pre (best-table [{:word "ww" :start 0 :skip 4}
                          {:word "uu" :start 2 :skip 3}
                          {:word "uu" :start 12 :skip 4}]
                         [["ww"] ["uu"]] 10)))

; -w---
; -u---
; -----
; w-u--
(expect '{:cylinder 5 :x 0 :y 0 :w 3 :h 4 :elses #{{:word "ww" :start 1 :skip 14}
                                                   {:word "uu" :start 6 :skip 11}}}
        (pre (best-table [{:word "ww" :start 1 :skip 14}
                          {:word "ww" :start 20 :skip 5}
                          {:word "uu" :start 6 :skip 11}]
                         [["ww"] ["uu"]] 10)))

; -
; w
; u
; w
; u
(expect '{:cylinder 1 :x 0 :y 1 :w 1 :h 4 :elses #{{:word "ww" :start 1 :skip 2}
                                                   {:word "uu" :start 2 :skip 2}}}
        (pre (best-table [{:word "ww" :start 1 :skip 2}
                          {:word "uu" :start 2 :skip 2}
                          {:word "ww" :start 3 :skip 2}]
                         [["ww"] ["uu"]] 10)))

; -
; -
; -
; -
; -
; w
; u
; w
; u
(expect '{:cylinder 1 :x 0 :y 5 :w 1 :h 4 :elses #{{:word "ww" :start 5 :skip 2}
                                                   {:word "uu" :start 6 :skip 2}}}
        (pre (best-table [{:word "ww" :start 5 :skip 2}
                          {:word "uu" :start 6 :skip 2}
                          {:word "ww" :start 1000 :skip 1000}
                          {:word "uu" :start 1001 :skip 1000}]
                         [["ww"] ["uu"]] 10)))

; u-----
; uw----
; uw----
; uw----
; uw----
; -w----
(expect '{:cylinder 6 :x 0 :y 0 :w 2 :h 6 :elses #{{:word "wwwww" :start 7 :skip 6}
                                                   {:word "uuuuu" :start 0 :skip 6}}}
        (pre (best-table [{:word "wwwww" :start 70 :skip 16}
                          {:word "uuuuu" :start 90 :skip 16}
                          {:word "wwwww" :start 7 :skip 16}
                          {:word "uuuuu" :start 9 :skip 16}
                          {:word "uuuuu" :start 0 :skip 6}
                          {:word "wwwww" :start 7 :skip 6}
                          {:word "uuuuu" :start 9 :skip 6}]
                         [["wwwww"] ["uuuuu"]] 10)))

; -j-uw--
; -jwuwi-
; -j-uwi-
; -j--wi-
(expect '{:cylinder 7 :x 1 :y 0 :w 5 :h 4 :elses #{{:word "uuu" :start 3 :skip 7}
                                                   {:word "ww" :start 9 :skip 2}
                                                   {:word "jjjj" :start 1 :skip 7}
                                                   {:word "wwww" :start 4 :skip 7}
                                                   {:word "iii" :start 12 :skip 7}}}
        (pre (best-table [{:word "uuu" :start 30 :skip 7}
                          {:word "ww" :start 90 :skip 2}
                          {:word "uuu" :start 3 :skip 7}
                          {:word "ww" :start 9 :skip 2}
                          {:word "jjjj" :start 1 :skip 7}
                          {:word "wwww" :start 4 :skip 7}
                          {:word "iii" :start 12 :skip 7}
                          {:word "jjjj" :start 10 :skip 8}
                          {:word "wwww" :start 40 :skip 7}
                          {:word "iii" :start 120 :skip 7}]
                         [["uuu"] ["ww"] ["jjjj"] ["wwww"] ["iii"]] 10)))

; ----
; -wu-
; -wu-
(expect '{:cylinder 4 :x 1 :y 1 :w 2 :h 2 :elses #{{:word "ww" :start 5 :skip 4}
                                                   {:word "uu" :start 6 :skip 4}}}
        (pre (best-table [{:word "wwwwww" :start 21 :skip 4}
                          {:word "ww" :start 5 :skip 4}
                          {:word "wwww" :start 20 :skip 4}
                          {:word "uu" :start 6 :skip 4}]
                         [["wwww" "ww" "wwwwww"] ["uu"]] 10)))
