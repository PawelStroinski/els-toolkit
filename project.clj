(defproject els-toolkit "0.1.0-SNAPSHOT"
  :description "Equidistant Letter Sequences Monte Carlo experiment"
  :url "https://github.com/PawelStroinski/els-toolkit"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [org.clojure/data.zip "0.1.1"]
                 [org.apache.mahout/mahout-math "0.9"]
                 [com.taoensso/timbre "3.4.0"]]
  :main ^:skip-aot els-toolkit.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[expectations "2.0.9"]]}}
  :plugins [[lein-autoexpect "1.4.2"]]
  :jvm-opts ["-Xmx4g"])
