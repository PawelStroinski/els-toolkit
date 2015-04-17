(ns els-toolkit.look-for-synonyms-test
  (:use els-toolkit.search expectations))

(def spec {:min-skip 2 :max-skip 2})

(expect [{:word "f" :start 0 :skip 2}
         {:word "oo" :start 1 :skip 2}
         {:word "fo" :start 0 :skip 2}]
        (look-for-synonyms spec [["f" "oo" "foo"] ["z" "fo"]] "fooo"))

(expect nil
        (look-for-synonyms spec [["f" "oo" "foo"] ["z" "Z"]] "fooo"))