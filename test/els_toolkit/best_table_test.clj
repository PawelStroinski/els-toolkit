(ns els-toolkit.best-table-test
  (:use els-toolkit.optimize expectations))

; w
(expect '{:cylinder 1 :x 0 :y 0 :w 1 :h 1 :elses #{{:word "w" :start 0 :skip 1}}}
        (best-table [{:word "w" :start 0 :skip 1}] [["w"]]))

; -
; w
(expect '{:cylinder 1 :x 0 :y 1 :w 1 :h 1 :elses #{{:word "w" :start 1 :skip 1}}}
        (best-table [{:word "w" :start 1 :skip 1}]
                    [["w"]]))

; -w
; -w
(expect '{:cylinder 2 :x 1 :y 0 :w 1 :h 2 :elses #{{:word "ww" :start 1 :skip 2}}}
        (best-table [{:word "ww" :start 0 :skip 4}
                     {:word "ww" :start 1 :skip 2}]
                    [["ww"]]))

; w
; u
; w
; u
(expect '{:cylinder 1 :x 0 :y 0 :w 1 :h 4 :elses #{{:word "ww" :start 0 :skip 2}
                                                   {:word "uu" :start 1 :skip 2}}}
        (best-table [{:word "ww" :start 0 :skip 2}
                     {:word "uu" :start 1 :skip 2}]
                    [["ww"] ["uu"]]))

; wu-
; wu-
(expect '{:cylinder 3 :x 0 :y 0 :w 2 :h 2 :elses #{{:word "ww" :start 0 :skip 3}
                                                   {:word "uu" :start 1 :skip 3}}}
        (best-table [{:word "ww" :start 0 :skip 3}
                     {:word "uu" :start 1 :skip 3}]
                    [["ww"] ["uu"]]))

; -wu
; -wu
(expect '{:cylinder 3 :x 1 :y 0 :w 2 :h 2 :elses #{{:word "ww" :start 1 :skip 3}
                                                   {:word "uu" :start 2 :skip 3}}}
        (best-table [{:word "ww" :start 1 :skip 3}
                     {:word "uu" :start 2 :skip 3}
                     {:word "ww" :start 10 :skip 3}
                     {:word "uu" :start 20 :skip 3}]
                    [["ww"] ["uu"]]))

; -wu-
; -wu-
(expect '{:cylinder 4 :x 1 :y 0 :w 2 :h 2 :elses #{{:word "ww" :start 1 :skip 4}
                                                   {:word "uu" :start 2 :skip 4}}}
        (best-table [{:word "ww" :start 1 :skip 4}
                     {:word "uu" :start 2 :skip 4}
                     {:word "ww" :start 1 :skip 10}
                     {:word "uu" :start 2 :skip 10}]
                    [["ww"] ["uu"]]))

; ----
; -wu-
; -wu-
(expect '{:cylinder 4 :x 1 :y 1 :w 2 :h 2 :elses #{{:word "ww" :start 5 :skip 4}
                                                   {:word "uu" :start 6 :skip 4}}}
        (best-table [{:word "ww" :start 21 :skip 4}
                     {:word "ww" :start 20 :skip 4}
                     {:word "ww" :start 5 :skip 4}
                     {:word "uu" :start 6 :skip 4}]
                    [["ww"] ["uu"]]))

; -----
; -w-u-
; -w-u-
(expect '{:cylinder 5 :x 1 :y 1 :w 3 :h 2 :elses #{{:word "ww" :start 6 :skip 5}
                                                   {:word "uu" :start 8 :skip 5}}}
        (best-table [{:word "ww" :start 16 :skip 5}
                     {:word "uu" :start 8 :skip 15}
                     {:word "ww" :start 6 :skip 5}
                     {:word "ww" :start 26 :skip 5}
                     {:word "uu" :start 8 :skip 35}
                     {:word "uu" :start 8 :skip 5}
                     {:word "uu" :start 8 :skip 25}
                     {:word "ww" :start 36 :skip 5}]
                    [["ww"] ["uu"]]))

; ----
; ----
; --w-
; --u-
; --w-
; --u-
(expect '{:cylinder 4 :x 2 :y 2 :w 1 :h 4 :elses #{{:word "ww" :start 10 :skip 8}
                                                   {:word "uu" :start 14 :skip 8}}}
        (best-table [{:word "ww" :start 10 :skip 8}
                     {:word "ww" :start 1000 :skip 100}
                     {:word "uu" :start 1400 :skip 100}
                     {:word "uu" :start 14 :skip 8}]
                    [["ww"] ["uu"]]))

; wu-
; -w-
; u--
(expect '{:cylinder 3 :x 0 :y 0 :w 2 :h 3 :elses #{{:word "ww" :start 0 :skip 4}
                                                   {:word "uu" :start 1 :skip 5}}}
        (best-table [{:word "ww" :start 0 :skip 4}
                     {:word "ww" :start 0 :skip 14}
                     {:word "ww" :start 0 :skip 15}
                     {:word "ww" :start 0 :skip 16}
                     {:word "ww" :start 0 :skip 17}
                     {:word "uu" :start 1 :skip 5}]
                    [["ww"] ["uu"]]))

; w---
; w---
; ----
; u---
; u---
(expect '{:cylinder 4 :x 0 :y 0 :w 1 :h 5 :elses #{{:word "ww" :start 0 :skip 4}
                                                   {:word "uu" :start 12 :skip 4}}}
        (best-table [{:word "ww" :start 0 :skip 4}
                     {:word "uu" :start 2 :skip 3}
                     {:word "uu" :start 12 :skip 4}]
                    [["ww"] ["uu"]]))

; -w---
; -u---
; -----
; w-u--
(expect '{:cylinder 5 :x 0 :y 0 :w 3 :h 4 :elses #{{:word "ww" :start 1 :skip 14}
                                                    {:word "uu" :start 6 :skip 11}}}
        (best-table [{:word "ww" :start 1 :skip 14}
                     {:word "ww" :start 20 :skip 5}
                     {:word "uu" :start 6 :skip 11}]
                    [["ww"] ["uu"]]))

; -
; w
; u
; w
; u
(expect '{:cylinder 1 :x 0 :y 1 :w 1 :h 4 :elses #{{:word "ww" :start 1 :skip 2}
                                                   {:word "uu" :start 2 :skip 2}}}
        (best-table [{:word "ww" :start 1 :skip 2}
                     {:word "uu" :start 2 :skip 2}
                     {:word "ww" :start 3 :skip 2}]
                    [["ww"] ["uu"]]))

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
        (best-table [{:word "ww" :start 5 :skip 2}
                     {:word "uu" :start 6 :skip 2}
                     {:word "ww" :start 1000 :skip 1000}
                     {:word "uu" :start 1001 :skip 1000}]
                    [["ww"] ["uu"]]))

; u-----
; uw----
; uw----
; uw----
; uw----
; -w----
(expect '{:cylinder 6 :x 0 :y 0 :w 2 :h 6 :elses #{{:word "wwwww" :start 7 :skip 6}
                                                   {:word "uuuuu" :start 0 :skip 6}}}
        (best-table [{:word "wwwww" :start 70 :skip 16}
                     {:word "uuuuu" :start 90 :skip 16}
                     {:word "wwwww" :start 7 :skip 16}
                     {:word "uuuuu" :start 9 :skip 16}
                     {:word "uuuuu" :start 0 :skip 6}
                     {:word "wwwww" :start 7 :skip 6}
                     {:word "uuuuu" :start 9 :skip 6}]
                    [["wwwww"] ["uuuuu"]]))

; -j-uw--
; -jwuwi-
; -j-uwi-
; -j--wi-
(expect '{:cylinder 7 :x 1 :y 0 :w 5 :h 4 :elses #{{:word "uuu" :start 3 :skip 7}
                                                   {:word "ww" :start 9 :skip 2}
                                                   {:word "jjjj" :start 1 :skip 7}
                                                   {:word "wwww" :start 4 :skip 7}
                                                   {:word "iii" :start 12 :skip 7}}}
        (best-table [{:word "uuu" :start 30 :skip 7}
                     {:word "ww" :start 90 :skip 2}
                     {:word "uuu" :start 3 :skip 7}
                     {:word "ww" :start 9 :skip 2}
                     {:word "jjjj" :start 1 :skip 7}
                     {:word "wwww" :start 4 :skip 7}
                     {:word "iii" :start 12 :skip 7}
                     {:word "jjjj" :start 10 :skip 8}
                     {:word "wwww" :start 40 :skip 7}
                     {:word "iii" :start 120 :skip 7}]
                    [["uuu"] ["ww"] ["jjjj"] ["wwww"] ["iii"]]))

; ----
; -wu-
; -wu-
(expect '{:cylinder 4 :x 1 :y 1 :w 2 :h 2 :elses #{{:word "ww" :start 5 :skip 4}
                                                   {:word "uu" :start 6 :skip 4}}}
        (best-table [{:word "wwwwww" :start 21 :skip 4}
                     {:word "ww" :start 5 :skip 4}
                     {:word "wwww" :start 20 :skip 4}
                     {:word "uu" :start 6 :skip 4}]
                    [["wwww" "ww" "wwwwww"] ["uu"]]))