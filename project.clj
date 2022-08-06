(defproject nnichols/faker "1.0.0"
  :description "A clojure library for creating fake requests"
  :url "https://github.com/nnichols/faker"
  :license {:name "MIT"
            :url  "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/data.xml "0.2.0-alpha7"]
                 [org.clojure/clojure "1.10.3"]
                 [ring/ring-mock "0.4.0"]]
  :profiles {:uberjar {:aot :all}
             :dev     {:dependencies [[circleci/bond "0.6.0"]
                                      [cheshire "5.10.0"]
                                      [compojure "1.6.2"]
                                      [ring/ring-core "1.9.4"]
                                      [ring/ring-json "0.5.1"]]}}
  :min-lein-version "2.5.3")
