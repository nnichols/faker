(ns faker.core-test
  (:require [faker.core :as sut]
            [clojure.test :refer [deftest is testing]]))

(deftest sample-test
  (testing "I don't test anything useful!"
    (is (= 1 1))))
