package com.enonic.lib.http.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.*;

public class UtilsTest
{
    @Test
    public void httpCodeToMessage()
    {
        assertEquals( "OK", Utils.httpCodeToMessage( 200 ) );
        assertEquals( "Created", Utils.httpCodeToMessage( 201 ) );
        assertEquals( "Accepted", Utils.httpCodeToMessage( 202 ) );
        assertEquals( "Non-Authoritative Information", Utils.httpCodeToMessage( 203 ) );
        assertEquals( "No Content", Utils.httpCodeToMessage( 204 ) );
        assertEquals( "Reset Content", Utils.httpCodeToMessage( 205 ) );
        assertEquals( "Partial Content", Utils.httpCodeToMessage( 206 ) );

        assertEquals( "Multiple Choices", Utils.httpCodeToMessage( 300 ) );
        assertEquals( "Moved Permanently", Utils.httpCodeToMessage( 301 ) );
        assertEquals( "Found", Utils.httpCodeToMessage( 302 ) );
        assertEquals( "See Other", Utils.httpCodeToMessage( 303 ) );
        assertEquals( "Not Modified", Utils.httpCodeToMessage( 304 ) );
        assertEquals( "Use Proxy", Utils.httpCodeToMessage( 305 ) );
        assertEquals( "Temporary Redirect", Utils.httpCodeToMessage( 307 ) );
        assertEquals( "Permanent Redirect", Utils.httpCodeToMessage( 308 ) );

        assertEquals( "Bad Request", Utils.httpCodeToMessage( 400 ) );
        assertEquals( "Unauthorized", Utils.httpCodeToMessage( 401 ) );
        assertEquals( "Payment Required", Utils.httpCodeToMessage( 402 ) );
        assertEquals( "Forbidden", Utils.httpCodeToMessage( 403 ) );
        assertEquals( "Not Found", Utils.httpCodeToMessage( 404 ) );
        assertEquals( "Method Not Allowed", Utils.httpCodeToMessage( 405 ) );
        assertEquals( "Not Acceptable", Utils.httpCodeToMessage( 406 ) );
        assertEquals( "Proxy Authentication Required", Utils.httpCodeToMessage( 407 ) );
        assertEquals( "Request Timeout", Utils.httpCodeToMessage( 408 ) );
        assertEquals( "Conflict", Utils.httpCodeToMessage( 409 ) );
        assertEquals( "Gone", Utils.httpCodeToMessage( 410 ) );
        assertEquals( "Length Required", Utils.httpCodeToMessage( 411 ) );
        assertEquals( "Precondition Failed", Utils.httpCodeToMessage( 412 ) );
        assertEquals( "Payload Too Large", Utils.httpCodeToMessage( 413 ) );
        assertEquals( "URI Too Long", Utils.httpCodeToMessage( 414 ) );
        assertEquals( "Unsupported Media Type", Utils.httpCodeToMessage( 415 ) );

        assertEquals( "Internal Server Error", Utils.httpCodeToMessage( 500 ) );
        assertEquals( "Not Implemented", Utils.httpCodeToMessage( 501 ) );
        assertEquals( "Bad Gateway", Utils.httpCodeToMessage( 502 ) );
        assertEquals( "Service Unavailable", Utils.httpCodeToMessage( 503 ) );
        assertEquals( "Gateway Timeout", Utils.httpCodeToMessage( 504 ) );
        assertEquals( "HTTP Version Not Supported", Utils.httpCodeToMessage( 505 ) );

        assertEquals( "", Utils.httpCodeToMessage( 999 ) );
    }

    @Test
    public void addQueryParams()
    {
        assertEquals( URI.create( "http://any" ), Utils.addQueryParams( URI.create( "http://any" ), Map.of() ) );
        assertEquals( URI.create( "http://user:pass@any:8080/path/?a=b" ),
                      Utils.addQueryParams( URI.create( "http://user:pass@any:8080/path/" ), Map.of( "a", "b" ) ) );
        assertEquals( URI.create( "http://any/path?a=b&c=1#frag/ment" ),
                      Utils.addQueryParams( URI.create( "http://any/path?a=b#frag/ment" ), Map.of( "c", "1" ) ) );

        assertEquals( URI.create( "http://any?a=b&c+d=e%2Ff" ),
                      Utils.addQueryParams( URI.create( "http://any?a=b" ), Map.of( "c d", "e/f" ) ) );
    }

    @Test
    public void calculateCookieExpires()
    {
        final long currentTime = System.currentTimeMillis();
        assertEquals( (Long) ( currentTime + 5000L ), Utils.calculateCookieExpires( currentTime, 5 ) );
    }

    @Test
    public void calculateCookieExpires_session()
    {
        assertNull( Utils.calculateCookieExpires( System.currentTimeMillis(), -1 ) );
    }

    @Test
    public void calculateCookieExpires_overflow()
    {
        assertEquals( (Long) 253402257599999L, Utils.calculateCookieExpires( System.currentTimeMillis(), Long.MAX_VALUE ) );
    }

    @Test
    public void guessTextCharset()
    {
        assertEquals( StandardCharsets.ISO_8859_1, Utils.guessTextCharset(
            HttpHeaders.of( Map.of( "content-type", List.of( "text/plain; charset=ISO-8859-1" ) ), ( s, s2 ) -> true ) ) );
        assertEquals( StandardCharsets.UTF_8,
                      Utils.guessTextCharset( HttpHeaders.of( Map.of( "content-type", List.of( "text/plain" ) ), ( s, s2 ) -> true ) ) );
        assertEquals( StandardCharsets.UTF_8, Utils.guessTextCharset(
            HttpHeaders.of( Map.of( "content-type", List.of( "text/something" ) ), ( s, s2 ) -> true ) ) );

        assertEquals( StandardCharsets.UTF_8, Utils.guessTextCharset(
            HttpHeaders.of( Map.of( "content-type", List.of( "application/javascript" ) ), ( s, s2 ) -> true ) ) );
        assertEquals( StandardCharsets.UTF_8, Utils.guessTextCharset(
            HttpHeaders.of( Map.of( "content-type", List.of( "application/manifest+json;charset=utf8" ) ), ( s, s2 ) -> true ) ) );
        assertEquals( StandardCharsets.UTF_8, Utils.guessTextCharset(
            HttpHeaders.of( Map.of( "content-type", List.of( "application/soap+xml" ) ), ( s, s2 ) -> true ) ) );

        assertNull( Utils.guessTextCharset( HttpHeaders.of( Map.of(), ( s, s2 ) -> true ) ) );
        assertNull( Utils.guessTextCharset( HttpHeaders.of( Map.of( "content-type", List.of( "mess" ) ), ( s, s2 ) -> true ) ) );
        assertNull( Utils.guessTextCharset( HttpHeaders.of( Map.of( "content-type", List.of( "image/gif" ) ), ( s, s2 ) -> true ) ) );
    }


    private static HttpResponse.ResponseInfo createResponseInfo( final int statusCode, Map<String, List<String>> headers )
    {
        return new HttpResponse.ResponseInfo()
        {
            @Override
            public int statusCode()
            {
                return statusCode;
            }

            @Override
            public HttpHeaders headers()
            {
                return headers == null ? null : HttpHeaders.of( headers, ( s, s2 ) -> true );
            }

            @Override
            public HttpClient.Version version()
            {
                return HttpClient.Version.HTTP_1_1;
            }
        };
    }
}
