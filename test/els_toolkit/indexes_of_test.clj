(ns els-toolkit.indexes-of-test
  (:use els-toolkit.search expectations))

(expect [] (indexes-of "bar" "foo" 0 1))
(expect [0] (indexes-of "foo" "foo" 0 1))
(expect [1 2] (indexes-of "o" "foobar" 0 1))
(expect [0] (indexes-of "fo" "foo" 0 2))
(expect [] (indexes-of "fo" "foo" 1 2))
(expect [2] (indexes-of "or" "foobar" 2 3))
(expect [2 4] (indexes-of "o" "foofoo" 0 2))
