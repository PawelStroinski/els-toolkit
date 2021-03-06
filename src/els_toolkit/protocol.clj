(ns els-toolkit.protocol
  (:require [clojure.xml :refer (parse)]
            [clojure.zip :refer (xml-zip)]
            [clojure.data.zip.xml :refer (xml-> xml1-> text)]
            [clojure.string :as str]))

(def text&trim (comp str/trim text))

(defn zipper->word [zipper]
  (if-let [synonyms (seq (xml-> zipper :synonym))]
    (vec (map text&trim synonyms))
    (vector (text&trim zipper))))

(defn add-reversed [synonyms reverse]
  (if reverse
    (map #(into %
                (map str/reverse %))
         synonyms)
    synonyms))

(defn xml->map [input]
  (let [zipper (-> input parse xml-zip)
        text1 #(xml1-> zipper % text&trim)
        read-strings (fn [& keys] (zipmap keys (map #(read-string (text1 %)) keys)))
        replace-max #(if (= % 'max) Double/POSITIVE_INFINITY %)]
    (-> {:text      (slurp (text1 :text))
         :synonyms  (map zipper->word (xml-> zipper :word))
         :shuffling (keyword (text1 :shuffling))}
        (merge (read-strings :min-skip :max-skip :max-cylinder :text-population :seed))
        (update-in [:max-skip] replace-max)
        (update-in [:max-cylinder] replace-max)
        (update-in [:synonyms] add-reversed (read-string (text1 :reverse))))))