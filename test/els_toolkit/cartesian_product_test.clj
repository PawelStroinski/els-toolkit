(ns els-toolkit.cartesian-product-test
  (:use els-toolkit.optimize expectations))

(defn sets [colls]
  (set
    (for [c colls]
      (set c))))

(expect '#{}
        (sets (cartesian-product [])))

(expect '#{#{0 1}}
        (sets (cartesian-product [#{0} #{1}])))

(expect '#{#{0 1}
           #{0 2}}
        (sets (cartesian-product [#{0} #{1 2}])))

(expect '#{#{0 1}
           #{0 2}
           #{0 3}}
        (sets (cartesian-product [#{0} #{1 2 3}])))

(expect '#{#{0 1 2 3}}
        (sets (cartesian-product [#{0} #{1} #{2} #{3}])))

(expect '#{#{0 1 2 3}
           #{4 1 2 3}}
        (sets (cartesian-product [#{0 4} #{1} #{2} #{3}])))

(expect '#{#{0 1 3 6}
           #{0 1 3 7}
           #{0 1 4 6}
           #{0 1 4 7}
           #{0 1 5 6}
           #{0 1 5 7}
           #{0 2 3 6}
           #{0 2 3 7}
           #{0 2 4 6}
           #{0 2 4 7}
           #{0 2 5 6}
           #{0 2 5 7}}
        (sets (cartesian-product [#{0} #{1 2} #{3 4 5} #{6 7}])))

(expect '#{#{1 5 6}
           #{2 5 6}
           #{3 5 6}
           #{4 5 6}
           #{1 5 7}
           #{2 5 7}
           #{3 5 7}
           #{4 5 7}}
        (sets (cartesian-product [#{1 2 3 4} #{5} #{6 7}])))