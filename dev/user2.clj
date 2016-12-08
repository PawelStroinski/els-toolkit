(ns user2
  (:import (clojure.lang PersistentQueue)
           (java.time LocalTime)
           (java.time.temporal ChronoUnit)))

(defn queue []
  {:generator (agent nil)
   :generated (atom {:batch-size 0
                     :queue      (PersistentQueue/EMPTY)
                     :ret        nil
                     :running    false
                     :run        false})})

(defn fetch [{:keys [generator generated]} generate-batch]
  (loop []
    (let [step (fn [{:keys [batch-size queue running] :as m}]
                 (let [run (and (<= (count queue) batch-size)
                                (not running))]
                   (assoc m
                     :queue (pop queue)
                     :ret (peek queue)
                     :running (or running run)
                     :run run)))
          generate (fn [_]
                     (let [batch (generate-batch)]
                       (swap!
                         generated
                         #(-> (update % :queue into batch)
                              (assoc :batch-size (count batch))
                              (assoc :running false)))))
          {:keys [ret run]} (swap! generated step)]
      (when run
        (send generator generate))
      (or ret (do (await generator) (recur))))))

;

(def counter (atom 0))

(defn gen-batch []
  (doall
    (repeatedly 10 (fn []
                     (swap! counter inc)
                     (Thread/sleep 200)
                     (println "generated" @counter "@" (.. Thread currentThread getName))
                     @counter))))

(def q (queue))

(comment (repeatedly 5 #(fetch q gen-batch)))

;

(require '[clojure.core.async :refer (chan <!! >!! <! >! go go-loop) :as async])
;(def c (chan 1))
;(>!! c "dzien dobry")
;(println (<!! c))

(defn go-print [c]
  (go
    (loop []
      (when-some [v (<! c)]
        (println "Received" v)
        (recur)))))

(def parse-words (map #(set (clojure.string/split % #"\s"))))
(def interesting (filter #(contains? % "Clojure")))
(defn match [search-words message-words]
  (count (clojure.set/intersection search-words message-words)))
(def happy (partial match #{"happy" "awsome"}))
(def sad (partial match #{"sad" "bug"}))
(def score (map (fn [m]
                  (println "Sleeping @" (.. Thread currentThread getName))
                  (Thread/sleep 1000)
                  (hash-map :words m
                            :happy (happy m)
                            :sad (sad m)))))
(defn sentiment-stage [in out]
  (let [xf (comp parse-words interesting score)]
    (async/pipeline 8 out xf in)))
(def results (chan 0))
(def inputs (chan 0))
(sentiment-stage inputs results)
(comment
  (go-loop [] (println "About to put @" (.. Thread currentThread getName))
              (>! inputs "Clojure some bug")
              (>! inputs "Clojure - awsome")
              (>! inputs "Unrelevant some bug")
              (>! inputs "Clojure neutral")
              (println "All put!")
              (recur))
  (async/put! inputs "one")
  (async/close! inputs)
  (go (>! inputs "zocha"))
  (go (>! inputs "malgocha"))
  (go (>! inputs :stop))
  (go-loop [] (println (<! results) "@" (.. Thread currentThread getName))
              (recur))
  (<!! (go-loop [i 10]
         (if (zero? i)
           :done
           (do (println i)
               (Thread/sleep 1000)
               (recur (dec i))))))
  (let [c1 (chan)
        c2 (chan)]
    (async/thread
      (while true
        (let [[v ch] (async/alts!! [c1 c2])]
          (println "read" v "from" ch))))
    (>!! c1 "hi")
    (>!! c2 "there"))
  (async/thread
    (<!! (go-loop [ret []]
           (if-some [v (<! inputs)]
             (do
               (println "adding v")
               (recur (conj ret v)))
             (do
               (print "returning ret")
               ret)))))
  (defn time-difference-avg
    "`s` is a string that looks like this `14:24:11 14:29:43 21:13:10 21:19:25`"
    [s]
    (as-> s %
          (clojure.string/split % #"\s")
          (map (fn [s] (LocalTime/parse s)) %)
          (partition 2 %)
          (map (fn [[t1 t2]] (.between ChronoUnit/SECONDS t1 t2)) %)
          (quot (apply + %) (count %))
          (LocalTime/ofSecondOfDay %)
          (str %)))
  (do
    (def console (atom []))
    (def numbers (chan 0))
    (def echoed (chan 0))
    (future (dotimes [i 20]
              (let [x (- 3 (mod i 3))]
                (>!! numbers x)))
            (async/close! numbers))
    (defn wait-and-echo [x]
      (swap! console conj x)
      (Thread/sleep (* x 300))
      x)
    (async/pipeline 8 echoed (map wait-and-echo) numbers)
    (time (loop []
            (when-let [x (<!! echoed)]
              (println x)
              (recur))))
    (println "done")
    (println @console " - total:" (reduce + @console)))
  )
