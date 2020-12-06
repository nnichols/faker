(ns faker.core
  (:require [clojure.data.xml :as xml]
            [ring.mock.request :as mock]))

(def add-content-type mock/content-type)
(def add-content-length mock/content-length)
(def add-query-parameters mock/query-string)

(defn add-headers
  [request headers]
  (reduce-kv mock/header request headers))

(defn add-cookies
  [request cookies]
  (reduce-kv mock/cookie request cookies))

(defn xml-body
  [request body]
  (-> request
      (add-content-type "application/xml")
      (mock/body (xml/emit-str body))))

(defn add-body
  [request body body-type]
  (if body
    (case body-type
      :inferred (mock/body request body)
      :json     (mock/json-body request body)
      :xml      (xml-body request body)
      :raw      (assoc request :body body))
    request))

(defn ->uri
  [scheme hostname port route]
  (str (name scheme) "://" hostname ":" port route))

(defn mock-request
  ([server http-verb route]
   (mock-request server http-verb route {}))

  ([server http-verb route {:keys [protocol scheme hostname port custom-route headers cookies content-type content-length body-type body query-parameters]}]
   (let [scheme       (or scheme :http)
         hostname     (or hostname "localhost")
         port         (or port (if (= scheme :http) 8080 443))
         uri          (if custom-route
                        route
                        (->uri scheme hostname port route))
         protocol     (or protocol "HTTP/1.1")
         base-request (mock/request http-verb uri)
         body-type    (or body-type :inferred)]
     (cond-> base-request
       protocol         (assoc :protocol protocol)
       body             (add-body body body-type)
       query-parameters (add-query-parameters query-parameters)
       headers          (add-headers headers)
       cookies          (add-cookies cookies)
       content-type     (add-content-type content-type)
       content-length   (add-content-length content-length)     
       :always          (server)))))
