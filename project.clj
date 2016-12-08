(defproject els-toolkit "0.2.0"
  :description "Equidistant Letter Sequences Monte Carlo experiment"
  :url "https://github.com/PawelStroinski/els-toolkit"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.zip "0.1.2"]
                 [org.apache.mahout/mahout-math "0.12.2"]
                 [com.taoensso/timbre "4.7.4"]
                 [primitive-math "0.1.5"]
                 [org.clojure/core.async "0.2.395"]
                 [progrock "0.1.1"]]
  :java-source-paths ["java"]
  :main ^:skip-aot els-toolkit.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev     {:dependencies [[expectations "2.1.9"]
                                      [criterium "0.4.4"]
                                      [org.clojure/test.check "0.9.0"]
                                      [com.climate/claypoole "1.1.4"]
                                      [org.clojure/math.numeric-tower "0.0.4"]]
                       :source-paths ["dev"]
                       :repl-options {:init-ns user2
                                      :init (set! *warn-on-reflection* true)}}}
  :plugins [[lein-autoexpect "1.4.2"]])
