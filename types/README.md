# Types for HTTP Client library

Type definitions for the [Enonic XP HTTP Client library](https://github.com/enonic/lib-http-client).

## Install

```bash
npm i --save-dev @enonic-types/lib-http-client
```

## Setup

Add lib-http-client to `compilerOptions.paths` in the `tsconfig.json` file used for your application's server-side TypeScript code:

```json
{
  "compilerOptions": {
    "paths": {
      "/lib/http-client": ["./node_modules/@enonic-types/lib-http-client"]
    }
  }
}
```

## Use

```ts
import {request} from '/lib/http-client';

const response = request({
    url: 'https://api.example.com/users',
    method: 'POST',
    headers: {
        'Accept': 'application/json',
    },
    contentType: 'application/json',
    body: JSON.stringify({name: 'John'}),
    connectionTimeout: 5000,
    readTimeout: 10000,
});

if (response.status === 200 && response.body) {
    const user = JSON.parse(response.body);
}
```

All imports from `/lib/http-client` should now be typed.

## API

### `request(params: HttpRequestParams): HttpResponse`

Sends an HTTP request and returns the response received from the remote server. The request is sent synchronously.

### `HttpRequestParams`

| Property            | Type                         | Description                                                                                                  |
|---------------------|------------------------------|--------------------------------------------------------------------------------------------------------------|
| `url`               | `string`                     | URL to which the request is sent. **Required.**                                                              |
| `method`            | `string`                     | HTTP method, e.g. `"GET"`, `"POST"`, `"PUT"`, `"DELETE"`, `"PATCH"`, `"HEAD"`. Default `"GET"`.              |
| `queryParams`       | `Record<string, unknown>`    | Query parameters to be sent with the request.                                                                |
| `params`            | `Record<string, unknown>`    | Body form parameters, encoded as `application/x-www-form-urlencoded`. Added to the query string for `GET` and `HEAD` if `queryParams` is not provided. |
| `headers`           | `Record<string, string>`     | HTTP headers to send with the request.                                                                       |
| `body`              | `string \| ByteSource`       | Body content, usually for `POST` or `PUT` requests.                                                          |
| `contentType`       | `string`                     | Content type of the request. Only applicable for requests with a body or multipart.                         |
| `multipart`         | `HttpRequestMultipartPart[]` | Multipart form data to send with the request.                                                                |
| `auth`              | `HttpRequestAuth`            | Basic authentication settings (`user`, `password`).                                                          |
| `proxy`             | `HttpRequestProxy`           | Proxy settings (`host`, `port`, `user`, `password`).                                                         |
| `followRedirects`   | `boolean`                    | Whether `3xx` redirects are followed internally. Default is to follow, but never from https to http.         |
| `connectionTimeout` | `number`                     | Timeout on establishing the connection, in milliseconds. Default `10000`.                                    |
| `readTimeout`       | `number`                     | Timeout on waiting to receive data, in milliseconds. Default `10000`.                                        |
| `disableHttp2`      | `boolean`                    | Disable use of HTTP/2. Always disabled for insecure HTTP connections. Default `false`.                       |
| `certificates`      | `ByteSource`                 | Stream of PEM encoded certificates replacing the platform's certificate authorities.                         |
| `clientCertificate` | `ByteSource`                 | Stream of PEM encoded private key (PKCS #8) and client certificate, concatenated.                            |

### `HttpRequestMultipartPart`

| Property      | Type                   | Description                                      |
|---------------|------------------------|--------------------------------------------------|
| `name`        | `string`               | Name of the part. **Required.**                  |
| `value`       | `string \| ByteSource` | Value of the part. **Required.**                 |
| `fileName`    | `string`               | File name of the part.                           |
| `contentType` | `string`               | Content type of the part.                        |

### `HttpResponse`

| Property      | Type                                 | Description                                                                               |
|---------------|--------------------------------------|-------------------------------------------------------------------------------------------|
| `status`      | `number`                             | HTTP status code.                                                                         |
| `message`     | `string`                             | HTTP status message.                                                                      |
| `headers`     | `Record<string, string \| string[]>` | Response headers. A `string` for single-valued headers, a `string[]` for repeated headers. |
| `contentType` | `string \| null`                     | Content type of the response.                                                             |
| `body`        | `string \| null`                     | Body as a string. `null` if the response content type is not text.                        |
| `bodyStream`  | `ByteSource`                         | Body as a stream.                                                                         |
| `cookies`     | `HttpResponseCookie[]`               | Cookies set in the response.                                                              |

### `HttpResponseCookie`

| Property   | Type             | Description                                                             |
|------------|------------------|-------------------------------------------------------------------------|
| `name`     | `string`         | Cookie name.                                                            |
| `value`    | `string`         | Cookie value.                                                           |
| `path`     | `string \| null` | Cookie path.                                                            |
| `domain`   | `string \| null` | Cookie domain.                                                          |
| `expires`  | `number \| null` | Expiration as epoch milliseconds, or `null` for a session cookie.       |
| `secure`   | `boolean`        | Whether the cookie is only sent over secure connections.                |
| `httpOnly` | `boolean`        | Whether the cookie is inaccessible to client-side scripts.              |

`ByteSource` is imported from [`@enonic-types/core`](https://www.npmjs.com/package/@enonic-types/core).

## Documentation

[HTTP Client library](https://developer.enonic.com/docs/http-client-library)
