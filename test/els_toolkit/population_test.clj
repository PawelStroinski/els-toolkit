(ns els-toolkit.population-test
  (:use els-toolkit.population expectations)
  (:require [els-toolkit.optimize :refer (els-letter-positions)]
            [els-toolkit.search :refer (look-for-words)]
            [clojure.string :refer (join)]))

(defn call-fisher–yates-shuffle [input distribution]
  (let [randoms (fisher–yates-shuffle-randoms distribution (count input))]
    (fisher–yates-shuffle input randoms)))

(defn stringify [nums]
  (join (map char nums)))

(expect
  (stringify
    [19 31 90 25 33 34 103 87 45 52 74 76 41 101 75 94 37 69 96 85 51 92 24 23 30 59 89 102 57 42 12 88 27 63
     95 13 97 61 40 71 11 55 8 67 9 7 54 44 68 60 20 99 26 43 39 100 81 65 58 70 66 21 10 15 83 28 104 22 53
     29 98 5 48 86 73 72 50 78 32 93 62 77 14 80 18 84 38 35 49 91 79 17 47 56 36 82 64 6 46 16])
  (call-fisher–yates-shuffle (stringify (range 5 105)) (new-distribution 1)))

(expect "tesurteeffldslh" (call-fisher–yates-shuffle "shuffledletters" (new-distribution 1)))

(def norm-elses #{{:word "uuu" :start 3 :skip 7}
                  {:word "jjjj" :start 1 :skip 7}
                  {:word "wwww" :start 4 :skip 7}
                  {:word "iii" :start 12 :skip 7}
                  {:word "uwi" :start 3 :skip 8}
                  {:word "juim" :start 1 :skip 9}})

(expect nil (els-conflicts? (first norm-elses) norm-elses))
(expect true (els-conflicts? {:word "ujw" :start 3 :skip 7} norm-elses))
(expect true (els-conflicts? {:word "iii" :start 11 :skip 7} norm-elses))
(expect nil (els-conflicts? {:word "jjz" :start 1 :skip 21} norm-elses))
(expect true (els-conflicts? {:word "jzj" :start 1 :skip 21} norm-elses))
(expect nil (els-conflicts? {:word "zzwz" :start 0 :skip 9} norm-elses))

(defn render [elses text-len]
  (let [dashes (vec (repeat text-len "-"))
        letters (interleave (mapcat els-letter-positions elses)
                            (mapcat :word elses))]
    (join (apply assoc dashes letters))))

(expect "-j-uw---j-uwi--j-uwi--j--wi-m-" (render norm-elses 30))

(defn remove-starts [elses]
  (set (map #(dissoc % :start) elses)))

(def norm (remove-starts norm-elses))

(defn remove-extra [elses]
  (set (filter norm elses)))

(def look-for-words-spec (partial look-for-words {:min-skip 7 :max-skip 9} (map :word norm-elses)))
(def distribution (new-distribution 8176753))
(def random-placements (doall (repeatedly 100 #(els-random-placement norm-elses 30 distribution))))

(expect (into () (repeat (count random-placements) norm))
        (pmap #(-> % (render 30) look-for-words-spec remove-starts remove-extra) random-placements))

(expect true (every? (partial not= norm-elses) random-placements))