(ns els-toolkit.best-table-for-els-in-cylinder-test
  (:use els-toolkit.optimize expectations))

(let [f (partial best-table-for-els-in-cylinder {:word "ww" :start 0 :skip 4})]
  ; w
  ; -
  ; -
  ; -
  ; w
  (expect '{:x 0 :y 0 :w 1 :h 5} (f 1))

  ; w-
  ; --
  ; w-
  (expect '{:x 0 :y 0 :w 1 :h 3} (f 2))

  ; w--
  ; -w-
  (expect '{:x 0 :y 0 :w 2 :h 2} (f 3))

  ; w---
  ; w---
  (expect '{:x 0 :y 0 :w 1 :h 2} (f 4))

  ; w---w
  (expect '{:x 0 :y 0 :w 5 :h 1} (f 5)))

(let [f (partial best-table-for-els-in-cylinder {:word "www" :start 0 :skip 2})]
  ; w
  ; -
  ; w
  ; -
  ; w
  (expect '{:x 0 :y 0 :w 1 :h 5} (f 1))

  ; w-
  ; w-
  ; w-
  (expect '{:x 0 :y 0 :w 1 :h 3} (f 2))

  ; w-w
  ; -w-
  (expect '{:x 0 :y 0 :w 3 :h 2} (f 3))

  ; w-w-
  ; w---
  (expect '{:x 0 :y 0 :w 3 :h 2} (f 4))

  ; w-w-w
  (expect '{:x 0 :y 0 :w 5 :h 1} (f 5)))

(let [f (partial best-table-for-els-in-cylinder {:word "ww" :start 1 :skip 2})]
  ; -
  ; w
  ; -
  ; w
  (expect '{:x 0 :y 1 :w 1 :h 3} (f 1))

  ; -w
  ; -w
  (expect '{:x 1 :y 0 :w 1 :h 2} (f 2))

  ; -w-
  ; w--
  (expect '{:x 0 :y 0 :w 2 :h 2} (f 3))

  ; -w-w
  (expect '{:x 1 :y 0 :w 3 :h 1} (f 4)))

(let [f (partial best-table-for-els-in-cylinder {:word "ww" :start 3 :skip 2})]
  ; -
  ; -
  ; -
  ; w
  ; -
  ; w
  (expect '{:x 0 :y 3 :w 1 :h 3} (f 1))

  ; --
  ; -w
  ; -w
  (expect '{:x 1 :y 1 :w 1 :h 2} (f 2))

  ; ---
  ; w-w
  (expect '{:x 0 :y 1 :w 3 :h 1} (f 3))

  ; ---w
  ; -w--
  (expect '{:x 1 :y 0 :w 3 :h 2} (f 4))

  ; ---w-
  ; w----
  (expect '{:x 0 :y 0 :w 4 :h 2} (f 5))

  ; ---w-w
  (expect '{:x 3 :y 0 :w 3 :h 1} (f 6)))

(let [f (partial best-table-for-els-in-cylinder {:word "www" :start 3 :skip 2})]
  ; -
  ; -
  ; -
  ; w
  ; -
  ; w
  ; -
  ; w
  (expect '{:x 0 :y 3 :w 1 :h 5} (f 1))

  ; --
  ; -w
  ; -w
  ; -w
  (expect '{:x 1 :y 1 :w 1 :h 3} (f 2))

  ; ---
  ; w-w
  ; -w-
  (expect '{:x 0 :y 1 :w 3 :h 2} (f 3))

  ; ---w
  ; -w-w
  (expect '{:x 1 :y 0 :w 3 :h 2} (f 4))

  ; ---w-
  ; w-w--
  (expect '{:x 0 :y 0 :w 4 :h 2} (f 5))

  ; ---w-w
  ; -w----
  (expect '{:x 1 :y 0 :w 5 :h 2} (f 6))

  ; ---w-w-
  ; w------
  (expect '{:x 0 :y 0 :w 6 :h 2} (f 7))

  ; ---w-w-w
  (expect '{:x 3 :y 0 :w 5 :h 1} (f 8)))

(let [f (partial best-table-for-els-in-cylinder {:word "wwww" :start 34 :skip 16})]
  ; --------------
  ; --------------
  ; ------w-------
  ; --------w-----
  ; ----------w---
  ; ------------w-
  (expect '{:x 6 :y 2 :w 7 :h 4} (f 14)))

(let [f (partial best-table-for-els-in-cylinder {:word "wwwww" :start 35 :skip 17})]
  ; --------------
  ; --------------
  ; -------w------
  ; ----------w---
  ; -------------w
  ; --------------
  ; --w-----------
  ; -----w--------
  (expect '{:x 2 :y 2 :w 12 :h 6} (f 14)))

(let [f (partial best-table-for-els-in-cylinder {:word "ww" :start 2 :skip 4})]
  ; --w---w-------------
  (expect '{:x 2 :y 0 :w 5 :h 1} (f 20)))