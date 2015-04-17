(ns els-toolkit.look-for-words-test
  (:use els-toolkit.search expectations))

(def spec {:min-skip 1 :max-skip 15})
(def look-for-words-spec (partial look-for-words spec))

(expect []
        (look-for-words (assoc spec :min-skip 2) ["foo"] "foo"))

(expect [{:word "foo" :start 0 :skip 1}]
        (look-for-words-spec ["foo"] "foo"))

(expect [{:word "foo" :start 1 :skip 2}]
        (look-for-words-spec ["foo"] "_f_o_o_"))

(expect [{:word "foo" :start 0 :skip 2}
         {:word "foo" :start 6 :skip 2}
         {:word "foo" :start 0 :skip 4}]
        (look-for-words-spec ["foo"] "f_o_o_f_o_o_"))

(expect [{:word "foo" :start 4 :skip 3}]
        (look-for-words-spec ["foo"] "bar_f__o__o"))

(expect [{:word "oo" :start 2 :skip 2}
         {:word "oo" :start 4 :skip 2}
         {:word "oo" :start 9 :skip 2}
         {:word "oo" :start 6 :skip 3}]
        (look-for-words (assoc spec :min-skip 2 :max-skip 3) ["oo"] "f_o_o_o__oooOOO"))

(expect [{:word "oo", :start 2, :skip 8}
         {:word "oo", :start 2, :skip 9}]
        (look-for-words (assoc spec :min-skip 8 :max-skip Double/POSITIVE_INFINITY) ["oo"] "f_o_o_o__oooOOO"))

(expect [{:word "FOOD" :start 9 :skip 10}
         {:word "ORDER" :start 10 :skip 10}
         {:word "GARY" :start 27 :skip 10}]
        (look-for-words-spec ["FOOD" "ORDER" "GARY"]
                             "THISISTHEFORMWEUSEFORFINDINGCODESANDIADDEDLETTERSPRECISELYPLACEDTOFORMALONGEREXAMPLE"))

(expect [{:word "f" :start 0 :skip 1}
         {:word "fo" :start 0 :skip 1}
         {:word "fo" :start 0 :skip 2}]
        (look-for-words-spec ["f" "fo"] "foo"))

(expect [{:word "f" :start 0 :skip 2}
         {:word "fo" :start 0 :skip 2}]
        (look-for-words (assoc spec :min-skip 2 :max-skip Double/POSITIVE_INFINITY) ["f" "fo"] "foo"))