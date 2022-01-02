# faker

<a href="https://icons8.com/icon/xbZcFKwb9J7z/fraud"><img src="resources/icons8-fraud.png"></a>
[![Clojars Project](https://img.shields.io/clojars/v/nnichols/faker.svg)](https://clojars.org/nnichols/faker)
[![Dependencies Status](https://versions.deps.co/nnichols/faker/status.svg)](https://versions.deps.co/nnichols/faker)
[![cljdoc badge](https://cljdoc.org/badge/nnichols/faker)](https://cljdoc.org/d/nnichols/faker/CURRENT)
![Github Runner](https://github.com/nnichols/faker/workflows/Clojure%20CI/badge.svg)

A Clojure library for generating fake HTTP requests for [Ring](https://github.com/ring-clojure/ring).

> Faker is a negative word, but it's still cool.
>
> - Lee "Faker" Sang-hyeok from [*SK Telecom T1*](https://en.wikipedia.org/wiki/Faker_(gamer))

## Installation

A deployed copy of the most recent version of [faker can be found on clojars.](https://clojars.org/nnichols/faker)
To use it, add the following as a dependency in your `project.clj` file:

[![Clojars Project](http://clojars.org/nnichols/faker/latest-version.svg)](http://clojars.org/nnichols/faker)

The next time you build your application, [Leiningen](https://leiningen.org/) should pull it automatically.
Alternatively, you may clone or fork the repository to work with it directly.

## Usage

All functions necessary to generate requests are provided in the core namespace.
To create a fake request, simply specify the HTTP method and resource you'd like to access:

```clojure
(:require [faker.core :as faker])

(def get-request
  (faker/mock-request :get "/v1/resource"))

;; => {:protocol "HTTP/1.1",
;;    :server-port 8080,
;;    :server-name "localhost",
;;    :remote-addr "127.0.0.1",
;;    :uri "/v1/resource",
;;    :scheme :http,
;;    :request-method :get,
;;    :headers {"host" "localhost:8080"}}
```

This request is ready to pass directly to your Ring server for testing.
Since your application's middleware and routes are highly configurable, the `mock-request` function also accepts an optional map to customize the request.
The following options are supported:

| Option              | Default Value                  | Description                                                          |
|---------------------|--------------------------------|----------------------------------------------------------------------|
| `:protocol`         | `"HTTP/1.1"`                   | The HTTP protocol version of the generated request |
| `:scheme`           | `:http`                        | The URI request scheme to use |
| `:hostname`         | `"localhost"`                  | The hostname to embed in the URI |
| `:port`             | Inferred based on `:scheme`    | The port number to embed in the URI |
| `:custom-route?`    | `false`                        | A flag to determine if `resource` should be treated as a fully qualified route. When false, generate a URI from the `scheme`, `hostname`, `port`, and `resource` |
| `:headers`          | `{}`                           | A map of HTTP headers to add on to the request |
| `:cookies`          | `{}`                           | A map of HTTP cookies to add on to the request |
| `:content-type`     | Inferred based on `:body-type` | The HTTP content type the request should be sent with. Defaults to `application/json` for `body-type` :json, `application/xml` for `body-type` :xml, `application/x-www-form-urlencoded` for `body-type` :form. For `body-type` inferred, rely on content-type detection in `ring-mock` |
| `:content-length`   | Inferred based on `:body`      | The HTTP content length in bytes for the request. If `body-type` != :raw, `ring-mock` will calculate this value for you. |
| `:body-type`        | `:inferred`                    | The keyword type of content in the request body, which is used to determine how to serialize EDN into the appropriate content-type and to set the `content-type` Expects one of :raw, :xml, :json, :form, :inferred. For :raw, assoc the `body` onto the request as is For :json, serialize the `body` with `cheshire` and set `content-type` to `application/json` For :xml, serialize the `body` with `clojure.data.xml` and set `content-type` to `application/xml` For :form, serialize the `body` as with `ring-mock` and set `content-type` to `application/x-www-form-urlencoded` For :inferred, use the automatic parsing in `ring-mock` and have it set the appropriate `content-type` |
| `:body`             | `nil`                          | The body of the HTTP request to serialize according to `body-type` |
| `:query-parameters` | `{}`                           | A map of query parameters to encode and add onto the request. |

For example:

```clojure
(faker/mock-request :get "/" {:headers   {"auth" "my-secure-token"}
                              :body-type :json
                              :body      {:key 123}
                              :scheme    :https})

;; => {:protocol "HTTP/1.1",
;;     :remote-addr "127.0.0.1",
;;     :headers {"host" "localhost:443", "content-type" "application/json", "content-length" "11", "auth" "my-secure-token"},
;;     :server-port 443,
;;     :content-length 11,
;;     :content-type "application/json",
;;     :uri "/",
;;     :server-name "localhost",
;;     :body #object[java.io.ByteArrayInputStream 0x1f314cee "java.io.ByteArrayInputStream@1f314cee"],
;;     :scheme :https,
;;     :request-method :get}
```

Since most applications will use [standard HTTP methods,](https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods) several aliased versions exist.
These functions support the exact same options as `mock-request`

```clojure
(faker/get "/" {:headers   {"auth" "my-secure-token"}
                :body-type :json
                :body      {:key 123}
                :scheme    :https})

;; => {:protocol "HTTP/1.1",
;;     :remote-addr "127.0.0.1",
;;     :headers {"host" "localhost:443", "content-type" "application/json", "content-length" "11", "auth" "my-secure-token"},
;;     :server-port 443,
;;     :content-length 11,
;;     :content-type "application/json",
;;     :uri "/",
;;     :server-name "localhost",
;;     :body #object[java.io.ByteArrayInputStream 0x1f314cee "java.io.ByteArrayInputStream@1f314cee"],
;;     :scheme :https,
;;     :request-method :get}
```

Currently supported methods are:

- `GET`
- `HEAD`
- `PUT`
- `DELETE`
- `CONNECT`
- `OPTIONS`
- `TRACE`
- `PATCH`

Each of these HTTP methods corresponds 1:1 with a lowercase function name in `faker.core`

This library currently _does not_ attempt to coerce requests or enforce HTTP standards and best practices on generated requests.
For example, you may create a GET request with a body.
This can be helpful when writing middleware/handlers for malformed, malicious, or unusual requests.

## Licensing

Copyright © 2020-2022 [Nick Nichols](https://nnichols.github.io/)

Distributed under the [MIT License](https://github.com/nnichols/faker/blob/master/LICENSE)

[Fraud Icon by Icons8](https://icons8.com/icon/xbZcFKwb9J7z/fraud)

### TODO

- Stand up a Ring server in tests, and test normal consumption
- Support Transit protocol for EDN
