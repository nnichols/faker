(ns faker.core-test
  (:require [clojure.data.xml :as xml]
            [clojure.test :refer [deftest is testing]]
            [faker.core :as sut]
            [ring.mock.request :as mock]))

(def ^:private sample-xml
  (xml/element :foo {} (xml/element :bar {:bar-attr "baz"})))

(defn ByteStream->string!
  "Drain a java.io.ByteArrayInputStream of all bytes, and cast it to a string.
   IMPORTANT: This mutates the state of `stream`, calling this function against the same stream more will always result in \"\""
  [stream]
  (java.lang.String. (java.io.ByteArrayInputStream/readAllBytes stream)))

(deftest ByteStream->string!-demo
  (testing "Demonstrating that ByteStream->string! is not idempotent"
    (let [sample-request (sut/add-body (mock/request :get "https://localhost:443/") {:some "key" :another "value"} :json)]
      (is (= "{\"some\":\"key\",\"another\":\"value\"}" (ByteStream->string! (:body sample-request))))
      (is (= "" (ByteStream->string! (:body sample-request))))
      (is (= "" (ByteStream->string! (:body sample-request)))))))

(deftest update-request-tests
  (let [sample-request         (mock/request :get "https://localhost:443/")
        json-request           (sut/add-content-type sample-request "application/json")
        content-length-request (sut/add-content-length sample-request "12345")
        query-string-request   (sut/add-query-parameters sample-request "?query=scooby")
        form-encode-request    (sut/add-query-parameters sample-request {:results true :size 50})
        headers-request        (sut/add-headers sample-request {:auth "some groovy token" :session "random uuid"})
        cookies-request        (sut/add-cookies sample-request {:session "12" "name" "Nick"})
        json-body-request      (sut/add-body sample-request {:some "key" :another "value"} :json)
        raw-body-request       (sut/add-body sample-request 123 :raw)
        form-body-request      (sut/add-body sample-request {:pog "champ" :id 3} :inferred)
        xml-body-request       (sut/add-body sample-request sample-xml :xml)]
    (testing "This block doesn't actually test our functionality, but surfaces the data from ring-mock for comparison (and to see if their API ever changes)"
      (is (= "HTTP/1.1" (:protocol sample-request)))
      (is (= 443 (:server-port sample-request)))
      (is (= "localhost" (:server-name sample-request)))
      (is (= "127.0.0.1" (:remote-addr sample-request)))
      (is (= "/" (:uri sample-request)))
      (is (= :https (:scheme sample-request)))
      (is (= :get (:request-method sample-request)))
      (is (= {"host" "localhost:443"} (:headers sample-request)))
      (is (empty? (dissoc sample-request :protocol :server-port :server-name :remote-addr :uri :scheme :request-method :headers))))

    (testing "This block tests all of the add-* functions to make sure the update only the relevant keys"
      (is (= "application/json" (:content-type json-request)))
      (is (= "application/json" (get-in json-request [:headers "content-type"])))
      (is (= sample-request (-> json-request (dissoc :content-type) (update :headers dissoc "content-type"))))

      (is (= "12345" (:content-length content-length-request)))
      (is (= "12345" (get-in content-length-request [:headers "content-length"])))
      (is (= sample-request (-> content-length-request (dissoc :content-length) (update :headers dissoc "content-length"))))

      (is (= sample-request (sut/add-query-parameters sample-request nil)))
      (is (= "?query=scooby" (:query-string query-string-request)))
      (is (= sample-request (dissoc query-string-request :query-string)))
      (is (= "results=true&size=50" (:query-string form-encode-request)))
      (is (= sample-request (dissoc form-encode-request :query-string)))

      (is (= sample-request (sut/add-headers sample-request nil)))
      (is (= sample-request (sut/add-headers sample-request {})))
      (is (= {"host" "localhost:443" "auth" "some groovy token" "session" "random uuid"} (:headers headers-request)))
      (is (= sample-request (update headers-request :headers dissoc "auth" "session")))

      (is (= sample-request (sut/add-cookies sample-request nil)))
      (is (= sample-request (sut/add-cookies sample-request {})))
      (is (= {"host" "localhost:443" "cookie" "session=12; name=Nick"} (:headers cookies-request)))
      (is (= sample-request (update cookies-request :headers dissoc "cookie")))

      (is (= sample-request (sut/add-body sample-request nil nil)))
      (is (= sample-request (sut/add-body sample-request nil :raw)))
      (is (= 123 (:body raw-body-request)))
      (is (= sample-request (dissoc raw-body-request :body)))

      (is (= 2 (:content-length (sut/add-body sample-request {} :json))))
      (is (= "application/json" (:content-type (sut/add-body sample-request {} :json))))
      (is (= {"host" "localhost:443", "content-type" "application/json", "content-length" "2"} (:headers (sut/add-body sample-request {} :json))))
      (is (= java.io.ByteArrayInputStream (type (:body (sut/add-body sample-request {} :json)))))
      (is (= "{}" (ByteStream->string! (:body (sut/add-body sample-request {} :json)))))
      (is (= sample-request (-> (sut/add-body sample-request {} :json) (dissoc :content-length :content-type :body) (update :headers dissoc "content-type" "content-length"))))

      (is (= 32 (:content-length json-body-request)))
      (is (= "application/json" (:content-type json-body-request)))
      (is (= {"host" "localhost:443", "content-type" "application/json", "content-length" "32"} (:headers json-body-request)))
      (is (= java.io.ByteArrayInputStream (type (:body json-body-request))))
      (is (= "{\"some\":\"key\",\"another\":\"value\"}" (ByteStream->string! (:body json-body-request))))
      (is (= sample-request (-> json-body-request (dissoc :content-length :content-type :body) (update :headers dissoc "content-type" "content-length"))))

      (is (= 14 (:content-length form-body-request)))
      (is (= "application/x-www-form-urlencoded" (:content-type form-body-request)))
      (is (= {"host" "localhost:443", "content-type" "application/x-www-form-urlencoded", "content-length" "14"} (:headers form-body-request)))
      (is (= java.io.ByteArrayInputStream (type (:body form-body-request))))
      (is (= "pog=champ&id=3" (ByteStream->string! (:body form-body-request))))
      (is (= sample-request (-> form-body-request (dissoc :content-length :content-type :body) (update :headers dissoc "content-type" "content-length"))))

      (is (= 70 (:content-length xml-body-request)))
      (is (= "application/xml" (:content-type xml-body-request)))
      (is (= {"host" "localhost:443", "content-type" "application/xml", "content-length" "70"} (:headers xml-body-request)))
      (is (= java.io.ByteArrayInputStream (type (:body xml-body-request))))
      (is (= "<?xml version=\"1.0\" encoding=\"UTF-8\"?><foo><bar bar-attr=\"baz\"/></foo>" (ByteStream->string! (:body xml-body-request))))
      (is (= sample-request (-> xml-body-request (dissoc :content-length :content-type :body) (update :headers dissoc "content-type" "content-length")))))))

(deftest ->uri-test
  (testing "Demonstrating the route formatting in `->uri`"
    (is (= "https://localhost:443/v1/test" (sut/->uri :https "localhost" 443 "/v1/test")))
    (is (= "ftp://localhost:21/" (sut/->uri :ftp "localhost" 21 "/")))))
