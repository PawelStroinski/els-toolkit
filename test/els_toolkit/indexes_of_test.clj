(ns els-toolkit.indexes-of-test
  (:use els-toolkit.search expectations))

(expect [] (indexes-of "bar" "foo"))
(expect [0] (indexes-of "foo" "foo"))
(expect [1 2] (indexes-of "o" "foobar"))