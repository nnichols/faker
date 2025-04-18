(defproject nnichols/faker "1.0.0"
  :description "A clojure library for creating fake requests"
  :url "https://github.com/nnichols/faker"
  :license {:name "MIT"
            :url  "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/data.xml "0.2.0-alpha9"]
                 [org.clojure/clojure "1.12.0"]
                 [ring/ring-mock "0.4.0"]]
  :profiles {:uberjar {:aot :all}
             :dev     {:dependencies [[circleci/bond "0.6.0"]
                                      [cheshire "6.0.0"]
                                      [compojure "1.7.1"]
                                      [ring/ring-core "1.13.0"]
                                      [ring/ring-json "0.5.1"]]}}
  :min-lein-version "2.5.3")
