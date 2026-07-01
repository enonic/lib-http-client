import type { ByteSource } from "@enonic-types/core";

declare module "/lib/http-client" {
    export interface HttpRequestAuth {
        /**
         * User name for basic authentication.
         */
        user?: string;

        /**
         * Password for basic authentication.
         */
        password?: string;
    }

    export interface HttpRequestProxy {
        /**
         * Proxy host name or IP address to use.
         */
        host?: string;

        /**
         * Proxy port to use.
         */
        port?: number;

        /**
         * User name for proxy authentication.
         */
        user?: string;

        /**
         * Password for proxy authentication.
         */
        password?: string;
    }

    export interface HttpRequestMultipartPart {
        /**
         * Name of the part.
         */
        name: string;

        /**
         * Value of the part. Can be a string or a stream.
         */
        value: string | ByteSource;

        /**
         * File name of the part.
         */
        fileName?: string;

        /**
         * Content type of the part.
         */
        contentType?: string;
    }

    export interface HttpRequestParams {
        /**
         * URL to which the request is sent.
         */
        url: string;

        /**
         * The HTTP method to use for the request (e.g. `"POST"`, `"GET"`, `"HEAD"`, `"PUT"`, `"DELETE"`, `"PATCH"`).
         *
         * @default "GET"
         */
        method?: string;

        /**
         * Query parameters to be sent with the request.
         */
        queryParams?: Record<string, unknown>;

        /**
         * Body form parameters. Will be encoded according to `application/x-www-form-urlencoded`.
         * For `GET` and `HEAD` request methods params are added to query string, but only if `queryParams` is not provided.
         */
        params?: Record<string, unknown>;

        /**
         * HTTP headers, an object where the keys are header names and the values the header values.
         */
        headers?: Record<string, string>;

        /**
         * Disable use of HTTP/2 protocol. For insecure HTTP connections HTTP/2 is always disabled.
         *
         * @default false
         */
        disableHttp2?: boolean;

        /**
         * The timeout on establishing the connection, in milliseconds.
         *
         * @default 10000
         */
        connectionTimeout?: number;

        /**
         * The timeout on waiting to receive data, in milliseconds.
         *
         * @default 10000
         */
        readTimeout?: number;

        /**
         * Body content to send with the request, usually for `POST` or `PUT` requests.
         * Can be a string or a stream.
         */
        body?: string | ByteSource;

        /**
         * Content type of the request. Only applicable for requests with a body or multipart.
         */
        contentType?: string;

        /**
         * Multipart form data to send with the request, as an array of part objects.
         */
        multipart?: HttpRequestMultipartPart[];

        /**
         * Settings for basic authentication.
         */
        auth?: HttpRequestAuth;

        /**
         * Proxy settings.
         */
        proxy?: HttpRequestProxy;

        /**
         * If set to `false`, redirect responses (`status=3xx`) will not trigger a new internal request,
         * and the function will return directly with the `3xx` status.
         * If `true`, redirects will be handled internally. Default is to handle redirects internally,
         * but not redirect from https to http.
         */
        followRedirects?: boolean;

        /**
         * Stream of PEM encoded certificates. Replaces the host platform's certificate authorities with a custom set.
         */
        certificates?: ByteSource;

        /**
         * Stream is interpreted as PEM encoded certificate: private key (in PKCS #8 format) and the client certificate concatenated.
         */
        clientCertificate?: ByteSource;
    }

    export interface HttpResponseCookie {
        /**
         * Cookie name.
         */
        name: string;

        /**
         * Cookie value.
         */
        value: string;

        /**
         * Cookie path.
         */
        path: string | null;

        /**
         * Cookie domain.
         */
        domain: string | null;

        /**
         * Cookie expiration as epoch milliseconds, or `null` when the cookie is a session cookie.
         */
        expires: number | null;

        /**
         * Whether the cookie is only sent over secure connections.
         */
        secure: boolean;

        /**
         * Whether the cookie is inaccessible to client-side scripts.
         */
        httpOnly: boolean;
    }

    export interface HttpResponse {
        /**
         * HTTP status code returned.
         */
        status: number;

        /**
         * HTTP status message returned.
         */
        message: string;

        /**
         * HTTP headers of the response. A value is a `string` for single-valued headers and a `string[]` for repeated headers.
         */
        headers: Record<string, string | string[]>;

        /**
         * Content type of the response.
         */
        contentType: string | null;

        /**
         * Body of the response as a string. `null` if the response content-type is not of type text.
         */
        body: string | null;

        /**
         * Body of the response as a stream.
         */
        bodyStream: ByteSource;

        /**
         * HTTP cookies set in the response.
         */
        cookies: HttpResponseCookie[];
    }

    /**
     * Sends an HTTP request and returns the response received from the remote server.
     * The request is sent synchronously — execution blocks until the response is received.
     */
    export function request(params: HttpRequestParams): HttpResponse;
}

export {};
