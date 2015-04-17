(ns els-toolkit.els-letter-positions-test
  (:use els-toolkit.optimize expectations))

(expect [0 4] (els-letter-positions {:word "ww" :start 0 :skip 4}))
(expect [0 2 4] (els-letter-positions {:word "www" :start 0 :skip 2}))
(expect [1 3] (els-letter-positions {:word "ww" :start 1 :skip 2}))
(expect [3 5] (els-letter-positions {:word "ww" :start 3 :skip 2}))
(expect [3] (els-letter-positions {:word "w" :start 3 :skip 0}))