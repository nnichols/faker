(ns faker.core
  (:refer-clojure :exclude [get])
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
      :form     (mock/body request body)
      :json     (mock/json-body request body)
      :xml      (xml-body request body)
      :raw      (assoc request :body body))
    request))

(defn ->uri
  [scheme hostname port resource]
  (str (name scheme) "://" hostname ":" port resource))

(defn mock-request
  "Create a mock HTTP-style request map, as expected by Ring.
   Expects the following arguments:
     `http-method`: A keyword representing the RFC 7231/5789 HTTP method the request will execute. #{:get, :head, :post, :put, :delete, :connect, :options, :trace, :patch)
     `resource`:    The string resource/route you wish to request. For example, \"/v1/recipe\" to access http://localhost:8080/v1/recipe
   Additionally, a map supporting the following options may be supplied as an optional third argument:
     `protocol`:         The HTTP protocol version to use. Defaults to HTTP/1.1
     `scheme`:           The URI request scheme to use. Defaults to :http
     `hostname`:         The URI request target hostname. Defaults to localhost
     `port`:             The URI request port number. If the scheme is :https, default is set to 443. Otherwise, 8080
     `custom-route?`:    A flag to determine if `resource` should be treated as a fully qualified route. 
                         Defaults to false, meaning the URI will be constructed from the `scheme`, `hostname`, `port`, and `resource`
     `headers`:          A map of HTTP headers to add onto the request.
     `cookies`:          A map of HTTP cookies to add onto the request.
     `content-type`:     The HTTP content type the request should be sent with. 
                         Defaults to `application/json` for `body-type` :json, `application/xml` for `body-type` :xml, `application/x-www-form-urlencoded` for `body-type` :form.
                         For `body-type` inferred, rely on content-type detection in `ring-mock`
     `content-length`:   The HTTP content length in bytes for the request.
                         If `body-type` != :raw, `ring-mock` will calculate this value for you.
     `body-type`:        The keyword type of content in the request body, which is used to determine how to serialize EDN into the appropriate content-type and to set the `content-type`.
                         Expects one of :raw, :xml, :json, :form, :inferred.
                         For :raw, assoc the `body` onto the request as is
                         For :json, serialize the `body` with `cheshire` and set `content-type` to `application/json`
                         For :xml, serialize the `body` with `clojure.data.xml` and set `content-type` to `application/xml`
                         For :form, serialize the `body` as with `ring-mock` and set `content-type` to `application/x-www-form-urlencoded`
                         For :inferred, use the automatic parsing in `ring-mock` and have it set the appropriate `content-type`
                         Defaults to :inferred
     `body`:             The body of the request to serialize according to `body-type`
     `query-parameters`: A map of query parameters to encode and add onto the request"
  ([http-method resource]
   (mock-request http-method resource {}))

  ([http-method resource {:keys [protocol scheme hostname port custom-route? headers cookies content-type content-length body-type body query-parameters]}]
   (let [scheme       (or scheme :http)
         hostname     (or hostname "localhost")
         port         (or port (if (= scheme :https) 443 8080))
         uri          (if custom-route?
                        resource
                        (->uri scheme hostname port resource))
         protocol     (or protocol "HTTP/1.1")
         base-request (mock/request http-method uri)
         body-type    (or body-type :inferred)]
     (cond-> base-request
       protocol         (assoc :protocol protocol)
       body             (add-body body body-type)
       query-parameters (add-query-parameters query-parameters)
       headers          (add-headers headers)
       cookies          (add-cookies cookies)
       content-type     (add-content-type content-type)
       content-length   (add-content-length content-length)))))

;; Convenience wrappers for common HTTP methods

(def get
  "Create a mock HTTP-style GET request map, as expected by Ring.
   Expects the following arguments:
     `resource`:    The string resource/route you wish to request. For example, \"/v1/recipe\" to access http://localhost:8080/v1/recipe
   Additionally, a map supporting the following options may be supplied as an optional second argument:
     `protocol`:         The HTTP protocol version to use. Defaults to HTTP/1.1
     `scheme`:           The URI request scheme to use. Defaults to :http
     `hostname`:         The URI request target hostname. Defaults to localhost
     `port`:             The URI request port number. If the scheme is :https, default is set to 443. Otherwise, 8080
     `custom-route?`:    A flag to determine if `resource` should be treated as a fully qualified route. 
                         Defaults to false, meaning the URI will be constructed from the `scheme`, `hostname`, `port`, and `resource`
     `headers`:          A map of HTTP headers to add onto the request.
     `cookies`:          A map of HTTP cookies to add onto the request.
     `content-type`:     The HTTP content type the request should be sent with. 
                         Defaults to `application/json` for `body-type` :json, `application/xml` for `body-type` :xml, `application/x-www-form-urlencoded` for `body-type` :form.
                         For `body-type` inferred, rely on content-type detection in `ring-mock`
     `content-length`:   The HTTP content length in bytes for the request.
                         If `body-type` != :raw, `ring-mock` will calculate this value for you.
     `body-type`:        The keyword type of content in the request body, which is used to determine how to serialize EDN into the appropriate content-type and to set the `content-type`.
                         Expects one of :raw, :xml, :json, :form, :inferred.
                         For :raw, assoc the `body` onto the request as is
                         For :json, serialize the `body` with `cheshire` and set `content-type` to `application/json`
                         For :xml, serialize the `body` with `clojure.data.xml` and set `content-type` to `application/xml`
                         For :form, serialize the `body` as with `ring-mock` and set `content-type` to `application/x-www-form-urlencoded`
                         For :inferred, use the automatic parsing in `ring-mock` and have it set the appropriate `content-type`
                         Defaults to :inferred
     `body`:             The body of the request to serialize according to `body-type`
     `query-parameters`: A map of query parameters to encode and add onto the request"
  (partial mock-request :get))

(def head
  "Create a mock HTTP-style HEAD request map, as expected by Ring.
   Expects the following arguments:
     `resource`:    The string resource/route you wish to request. For example, \"/v1/recipe\" to access http://localhost:8080/v1/recipe
   Additionally, a map supporting the following options may be supplied as an optional second argument:
     `protocol`:         The HTTP protocol version to use. Defaults to HTTP/1.1
     `scheme`:           The URI request scheme to use. Defaults to :http
     `hostname`:         The URI request target hostname. Defaults to localhost
     `port`:             The URI request port number. If the scheme is :https, default is set to 443. Otherwise, 8080
     `custom-route?`:    A flag to determine if `resource` should be treated as a fully qualified route. 
                         Defaults to false, meaning the URI will be constructed from the `scheme`, `hostname`, `port`, and `resource`
     `headers`:          A map of HTTP headers to add onto the request.
     `cookies`:          A map of HTTP cookies to add onto the request.
     `content-type`:     The HTTP content type the request should be sent with. 
                         Defaults to `application/json` for `body-type` :json, `application/xml` for `body-type` :xml, `application/x-www-form-urlencoded` for `body-type` :form.
                         For `body-type` inferred, rely on content-type detection in `ring-mock`
     `content-length`:   The HTTP content length in bytes for the request.
                         If `body-type` != :raw, `ring-mock` will calculate this value for you.
     `body-type`:        The keyword type of content in the request body, which is used to determine how to serialize EDN into the appropriate content-type and to set the `content-type`.
                         Expects one of :raw, :xml, :json, :form, :inferred.
                         For :raw, assoc the `body` onto the request as is
                         For :json, serialize the `body` with `cheshire` and set `content-type` to `application/json`
                         For :xml, serialize the `body` with `clojure.data.xml` and set `content-type` to `application/xml`
                         For :form, serialize the `body` as with `ring-mock` and set `content-type` to `application/x-www-form-urlencoded`
                         For :inferred, use the automatic parsing in `ring-mock` and have it set the appropriate `content-type`
                         Defaults to :inferred
     `body`:             The body of the request to serialize according to `body-type`
     `query-parameters`: A map of query parameters to encode and add onto the request"
  (partial mock-request :head))

(def post
  "Create a mock HTTP-style POST request map, as expected by Ring.
   Expects the following arguments:
     `resource`:    The string resource/route you wish to request. For example, \"/v1/recipe\" to access http://localhost:8080/v1/recipe
   Additionally, a map supporting the following options may be supplied as an optional second argument:
     `protocol`:         The HTTP protocol version to use. Defaults to HTTP/1.1
     `scheme`:           The URI request scheme to use. Defaults to :http
     `hostname`:         The URI request target hostname. Defaults to localhost
     `port`:             The URI request port number. If the scheme is :https, default is set to 443. Otherwise, 8080
     `custom-route?`:    A flag to determine if `resource` should be treated as a fully qualified route. 
                         Defaults to false, meaning the URI will be constructed from the `scheme`, `hostname`, `port`, and `resource`
     `headers`:          A map of HTTP headers to add onto the request.
     `cookies`:          A map of HTTP cookies to add onto the request.
     `content-type`:     The HTTP content type the request should be sent with. 
                         Defaults to `application/json` for `body-type` :json, `application/xml` for `body-type` :xml, `application/x-www-form-urlencoded` for `body-type` :form.
                         For `body-type` inferred, rely on content-type detection in `ring-mock`
     `content-length`:   The HTTP content length in bytes for the request.
                         If `body-type` != :raw, `ring-mock` will calculate this value for you.
     `body-type`:        The keyword type of content in the request body, which is used to determine how to serialize EDN into the appropriate content-type and to set the `content-type`.
                         Expects one of :raw, :xml, :json, :form, :inferred.
                         For :raw, assoc the `body` onto the request as is
                         For :json, serialize the `body` with `cheshire` and set `content-type` to `application/json`
                         For :xml, serialize the `body` with `clojure.data.xml` and set `content-type` to `application/xml`
                         For :form, serialize the `body` as with `ring-mock` and set `content-type` to `application/x-www-form-urlencoded`
                         For :inferred, use the automatic parsing in `ring-mock` and have it set the appropriate `content-type`
                         Defaults to :inferred
     `body`:             The body of the request to serialize according to `body-type`
     `query-parameters`: A map of query parameters to encode and add onto the request"
  (partial mock-request :post))

(def put
  "Create a mock HTTP-style PUT request map, as expected by Ring.
   Expects the following arguments:
     `resource`:    The string resource/route you wish to request. For example, \"/v1/recipe\" to access http://localhost:8080/v1/recipe
   Additionally, a map supporting the following options may be supplied as an optional second argument:
     `protocol`:         The HTTP protocol version to use. Defaults to HTTP/1.1
     `scheme`:           The URI request scheme to use. Defaults to :http
     `hostname`:         The URI request target hostname. Defaults to localhost
     `port`:             The URI request port number. If the scheme is :https, default is set to 443. Otherwise, 8080
     `custom-route?`:    A flag to determine if `resource` should be treated as a fully qualified route. 
                         Defaults to false, meaning the URI will be constructed from the `scheme`, `hostname`, `port`, and `resource`
     `headers`:          A map of HTTP headers to add onto the request.
     `cookies`:          A map of HTTP cookies to add onto the request.
     `content-type`:     The HTTP content type the request should be sent with. 
                         Defaults to `application/json` for `body-type` :json, `application/xml` for `body-type` :xml, `application/x-www-form-urlencoded` for `body-type` :form.
                         For `body-type` inferred, rely on content-type detection in `ring-mock`
     `content-length`:   The HTTP content length in bytes for the request.
                         If `body-type` != :raw, `ring-mock` will calculate this value for you.
     `body-type`:        The keyword type of content in the request body, which is used to determine how to serialize EDN into the appropriate content-type and to set the `content-type`.
                         Expects one of :raw, :xml, :json, :form, :inferred.
                         For :raw, assoc the `body` onto the request as is
                         For :json, serialize the `body` with `cheshire` and set `content-type` to `application/json`
                         For :xml, serialize the `body` with `clojure.data.xml` and set `content-type` to `application/xml`
                         For :form, serialize the `body` as with `ring-mock` and set `content-type` to `application/x-www-form-urlencoded`
                         For :inferred, use the automatic parsing in `ring-mock` and have it set the appropriate `content-type`
                         Defaults to :inferred
     `body`:             The body of the request to serialize according to `body-type`
     `query-parameters`: A map of query parameters to encode and add onto the request"
  (partial mock-request :put))

(def delete
  "Create a mock HTTP-style DELETE request map, as expected by Ring.
   Expects the following arguments:
     `resource`:    The string resource/route you wish to request. For example, \"/v1/recipe\" to access http://localhost:8080/v1/recipe
   Additionally, a map supporting the following options may be supplied as an optional second argument:
     `protocol`:         The HTTP protocol version to use. Defaults to HTTP/1.1
     `scheme`:           The URI request scheme to use. Defaults to :http
     `hostname`:         The URI request target hostname. Defaults to localhost
     `port`:             The URI request port number. If the scheme is :https, default is set to 443. Otherwise, 8080
     `custom-route?`:    A flag to determine if `resource` should be treated as a fully qualified route. 
                         Defaults to false, meaning the URI will be constructed from the `scheme`, `hostname`, `port`, and `resource`
     `headers`:          A map of HTTP headers to add onto the request.
     `cookies`:          A map of HTTP cookies to add onto the request.
     `content-type`:     The HTTP content type the request should be sent with. 
                         Defaults to `application/json` for `body-type` :json, `application/xml` for `body-type` :xml, `application/x-www-form-urlencoded` for `body-type` :form.
                         For `body-type` inferred, rely on content-type detection in `ring-mock`
     `content-length`:   The HTTP content length in bytes for the request.
                         If `body-type` != :raw, `ring-mock` will calculate this value for you.
     `body-type`:        The keyword type of content in the request body, which is used to determine how to serialize EDN into the appropriate content-type and to set the `content-type`.
                         Expects one of :raw, :xml, :json, :form, :inferred.
                         For :raw, assoc the `body` onto the request as is
                         For :json, serialize the `body` with `cheshire` and set `content-type` to `application/json`
                         For :xml, serialize the `body` with `clojure.data.xml` and set `content-type` to `application/xml`
                         For :form, serialize the `body` as with `ring-mock` and set `content-type` to `application/x-www-form-urlencoded`
                         For :inferred, use the automatic parsing in `ring-mock` and have it set the appropriate `content-type`
                         Defaults to :inferred
     `body`:             The body of the request to serialize according to `body-type`
     `query-parameters`: A map of query parameters to encode and add onto the request"
  (partial mock-request :delete))

(def connect
  "Create a mock HTTP-style CONNECT request map, as expected by Ring.
   Expects the following arguments:
     `resource`:    The string resource/route you wish to request. For example, \"/v1/recipe\" to access http://localhost:8080/v1/recipe
   Additionally, a map supporting the following options may be supplied as an optional second argument:
     `protocol`:         The HTTP protocol version to use. Defaults to HTTP/1.1
     `scheme`:           The URI request scheme to use. Defaults to :http
     `hostname`:         The URI request target hostname. Defaults to localhost
     `port`:             The URI request port number. If the scheme is :https, default is set to 443. Otherwise, 8080
     `custom-route?`:    A flag to determine if `resource` should be treated as a fully qualified route. 
                         Defaults to false, meaning the URI will be constructed from the `scheme`, `hostname`, `port`, and `resource`
     `headers`:          A map of HTTP headers to add onto the request.
     `cookies`:          A map of HTTP cookies to add onto the request.
     `content-type`:     The HTTP content type the request should be sent with. 
                         Defaults to `application/json` for `body-type` :json, `application/xml` for `body-type` :xml, `application/x-www-form-urlencoded` for `body-type` :form.
                         For `body-type` inferred, rely on content-type detection in `ring-mock`
     `content-length`:   The HTTP content length in bytes for the request.
                         If `body-type` != :raw, `ring-mock` will calculate this value for you.
     `body-type`:        The keyword type of content in the request body, which is used to determine how to serialize EDN into the appropriate content-type and to set the `content-type`.
                         Expects one of :raw, :xml, :json, :form, :inferred.
                         For :raw, assoc the `body` onto the request as is
                         For :json, serialize the `body` with `cheshire` and set `content-type` to `application/json`
                         For :xml, serialize the `body` with `clojure.data.xml` and set `content-type` to `application/xml`
                         For :form, serialize the `body` as with `ring-mock` and set `content-type` to `application/x-www-form-urlencoded`
                         For :inferred, use the automatic parsing in `ring-mock` and have it set the appropriate `content-type`
                         Defaults to :inferred
     `body`:             The body of the request to serialize according to `body-type`
     `query-parameters`: A map of query parameters to encode and add onto the request"
  (partial mock-request :connect))

(def options
  "Create a mock HTTP-style OPTIONS request map, as expected by Ring.
   Expects the following arguments:
     `resource`:    The string resource/route you wish to request. For example, \"/v1/recipe\" to access http://localhost:8080/v1/recipe
   Additionally, a map supporting the following options may be supplied as an optional second argument:
     `protocol`:         The HTTP protocol version to use. Defaults to HTTP/1.1
     `scheme`:           The URI request scheme to use. Defaults to :http
     `hostname`:         The URI request target hostname. Defaults to localhost
     `port`:             The URI request port number. If the scheme is :https, default is set to 443. Otherwise, 8080
     `custom-route?`:    A flag to determine if `resource` should be treated as a fully qualified route. 
                         Defaults to false, meaning the URI will be constructed from the `scheme`, `hostname`, `port`, and `resource`
     `headers`:          A map of HTTP headers to add onto the request.
     `cookies`:          A map of HTTP cookies to add onto the request.
     `content-type`:     The HTTP content type the request should be sent with. 
                         Defaults to `application/json` for `body-type` :json, `application/xml` for `body-type` :xml, `application/x-www-form-urlencoded` for `body-type` :form.
                         For `body-type` inferred, rely on content-type detection in `ring-mock`
     `content-length`:   The HTTP content length in bytes for the request.
                         If `body-type` != :raw, `ring-mock` will calculate this value for you.
     `body-type`:        The keyword type of content in the request body, which is used to determine how to serialize EDN into the appropriate content-type and to set the `content-type`.
                         Expects one of :raw, :xml, :json, :form, :inferred.
                         For :raw, assoc the `body` onto the request as is
                         For :json, serialize the `body` with `cheshire` and set `content-type` to `application/json`
                         For :xml, serialize the `body` with `clojure.data.xml` and set `content-type` to `application/xml`
                         For :form, serialize the `body` as with `ring-mock` and set `content-type` to `application/x-www-form-urlencoded`
                         For :inferred, use the automatic parsing in `ring-mock` and have it set the appropriate `content-type`
                         Defaults to :inferred
     `body`:             The body of the request to serialize according to `body-type`
     `query-parameters`: A map of query parameters to encode and add onto the request"
  (partial mock-request :options))

(def trace
  "Create a mock HTTP-style TRACE request map, as expected by Ring.
   Expects the following arguments:
     `resource`:    The string resource/route you wish to request. For example, \"/v1/recipe\" to access http://localhost:8080/v1/recipe
   Additionally, a map supporting the following options may be supplied as an optional second argument:
     `protocol`:         The HTTP protocol version to use. Defaults to HTTP/1.1
     `scheme`:           The URI request scheme to use. Defaults to :http
     `hostname`:         The URI request target hostname. Defaults to localhost
     `port`:             The URI request port number. If the scheme is :https, default is set to 443. Otherwise, 8080
     `custom-route?`:    A flag to determine if `resource` should be treated as a fully qualified route. 
                         Defaults to false, meaning the URI will be constructed from the `scheme`, `hostname`, `port`, and `resource`
     `headers`:          A map of HTTP headers to add onto the request.
     `cookies`:          A map of HTTP cookies to add onto the request.
     `content-type`:     The HTTP content type the request should be sent with. 
                         Defaults to `application/json` for `body-type` :json, `application/xml` for `body-type` :xml, `application/x-www-form-urlencoded` for `body-type` :form.
                         For `body-type` inferred, rely on content-type detection in `ring-mock`
     `content-length`:   The HTTP content length in bytes for the request.
                         If `body-type` != :raw, `ring-mock` will calculate this value for you.
     `body-type`:        The keyword type of content in the request body, which is used to determine how to serialize EDN into the appropriate content-type and to set the `content-type`.
                         Expects one of :raw, :xml, :json, :form, :inferred.
                         For :raw, assoc the `body` onto the request as is
                         For :json, serialize the `body` with `cheshire` and set `content-type` to `application/json`
                         For :xml, serialize the `body` with `clojure.data.xml` and set `content-type` to `application/xml`
                         For :form, serialize the `body` as with `ring-mock` and set `content-type` to `application/x-www-form-urlencoded`
                         For :inferred, use the automatic parsing in `ring-mock` and have it set the appropriate `content-type`
                         Defaults to :inferred
     `body`:             The body of the request to serialize according to `body-type`
     `query-parameters`: A map of query parameters to encode and add onto the request"
  (partial mock-request :trace))

(def patch
  "Create a mock HTTP-style PATCH request map, as expected by Ring.
   Expects the following arguments:
     `resource`:    The string resource/route you wish to request. For example, \"/v1/recipe\" to access http://localhost:8080/v1/recipe
   Additionally, a map supporting the following options may be supplied as an optional second argument:
     `protocol`:         The HTTP protocol version to use. Defaults to HTTP/1.1
     `scheme`:           The URI request scheme to use. Defaults to :http
     `hostname`:         The URI request target hostname. Defaults to localhost
     `port`:             The URI request port number. If the scheme is :https, default is set to 443. Otherwise, 8080
     `custom-route?`:    A flag to determine if `resource` should be treated as a fully qualified route. 
                         Defaults to false, meaning the URI will be constructed from the `scheme`, `hostname`, `port`, and `resource`
     `headers`:          A map of HTTP headers to add onto the request.
     `cookies`:          A map of HTTP cookies to add onto the request.
     `content-type`:     The HTTP content type the request should be sent with. 
                         Defaults to `application/json` for `body-type` :json, `application/xml` for `body-type` :xml, `application/x-www-form-urlencoded` for `body-type` :form.
                         For `body-type` inferred, rely on content-type detection in `ring-mock`
     `content-length`:   The HTTP content length in bytes for the request.
                         If `body-type` != :raw, `ring-mock` will calculate this value for you.
     `body-type`:        The keyword type of content in the request body, which is used to determine how to serialize EDN into the appropriate content-type and to set the `content-type`.
                         Expects one of :raw, :xml, :json, :form, :inferred.
                         For :raw, assoc the `body` onto the request as is
                         For :json, serialize the `body` with `cheshire` and set `content-type` to `application/json`
                         For :xml, serialize the `body` with `clojure.data.xml` and set `content-type` to `application/xml`
                         For :form, serialize the `body` as with `ring-mock` and set `content-type` to `application/x-www-form-urlencoded`
                         For :inferred, use the automatic parsing in `ring-mock` and have it set the appropriate `content-type`
                         Defaults to :inferred
     `body`:             The body of the request to serialize according to `body-type`
     `query-parameters`: A map of query parameters to encode and add onto the request"
  (partial mock-request :patch))
