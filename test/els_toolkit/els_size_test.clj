(ns els-toolkit.els-size-test
  (:use els-toolkit.optimize expectations))

(expect 1 (els-size {:word "w" :skip 1}))
(expect 2 (els-size {:word "ww" :skip 1}))
(expect 3 (els-size {:word "ww" :skip 2}))
(expect 4 (els-size {:word "ww" :skip 3}))