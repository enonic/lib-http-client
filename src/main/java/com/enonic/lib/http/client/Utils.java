package com.enonic.lib.http.client;

import java.io.IOException;
import java.math.BigInteger;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.mizosoft.methanol.MediaType;

import static java.util.Objects.requireNonNullElse;

final class Utils
{
    /**
     * Last millisecond in Dec 31 9999 minus 12 hours so writers/parsers can assume 4 digit year.
     */
    private static final long MAX_COOKIE_EXPIRES = 253402257599999L;

    private Utils()
    {
    }

    public static long getContentLength( final HttpHeaders headers )
    {
        try
        {
            return headers.firstValueAsLong( "content-length" ).orElse( -1L );
        }
        catch ( NumberFormatException e )
        {
            return -1;
        }
    }

    public static String getContentType( final HttpHeaders headers )
    {
        return headers.firstValue( "content-type" ).orElse( null );
    }

    public static Charset guessTextCharset( final HttpHeaders headers )
    {
        final String contentType = getContentType( headers );
        if ( requireNonNullElse( contentType, "" ).isBlank() )
        {
            return null;
        }

        final MediaType mediaType;
        try
        {
            mediaType = MediaType.parse( contentType );
        }
        catch ( IllegalArgumentException e )
        {
            return null;
        }

        final String subType = mediaType.subtype();
        final String type = mediaType.type();

        return type.equals( "text" ) || subType.equals( "javascript" ) || subType.contains( "xml" ) || subType.contains( "json" )
            ? mediaType.charsetOrDefault( StandardCharsets.UTF_8 )
            : null;
    }

    public static List<HttpCookie> getCookies( HttpResponse<?> response )
        throws IOException
    {
        final CookieManager cookieHandler = new CookieManager( null, CookiePolicy.ACCEPT_ALL );

        cookieHandler.put( response.request().uri(), response.headers().map() );
        return cookieHandler.getCookieStore().getCookies();
    }

    public static Long calculateCookieExpires( final long currentTime, final long maxAge )
    {
        if ( maxAge < 0 )
        {
            return null;
        }
        final BigInteger expiresBig =
            BigInteger.valueOf( maxAge ).multiply( BigInteger.valueOf( 1000 ) ).add( BigInteger.valueOf( currentTime ) );
        return expiresBig.compareTo( BigInteger.valueOf( MAX_COOKIE_EXPIRES ) ) < 0 ? expiresBig.longValue() : MAX_COOKIE_EXPIRES;
    }

    /**
     * @param data data to be encoded
     * @return string with application/x-www-form-urlencoded data
     */
    public static String formUrlEncoded( final Map<String, String> data )
    {
        return data.entrySet()
            .stream()
            .map( entry -> URLEncoder.encode( entry.getKey(), StandardCharsets.UTF_8 ) + "=" +
                URLEncoder.encode( entry.getValue(), StandardCharsets.UTF_8 ) )
            .collect( Collectors.joining( "&" ) );
    }

    /**
     * @param uri    original URI
     * @param params query parameters to be added
     * @return URI with added query parameters
     */
    public static URI addQueryParams( final URI uri, final Map<String, String> params )
    {
        if ( params.isEmpty() )
        {
            return uri;
        }

        try
        {
            URI base = new URI( uri.getScheme(), uri.getAuthority(), uri.getPath(), null, null );

            final String encodedParams = Utils.formUrlEncoded( params );
            final String query = "?" + ( uri.getRawQuery() == null ? encodedParams : uri.getRawQuery() + "&" + encodedParams );
            final String fragment = uri.getRawFragment() != null ? "#" + uri.getRawFragment() : "";

            return URI.create( base + query + fragment );
        }
        catch ( URISyntaxException e )
        {
            throw new IllegalArgumentException( e.getMessage(), e );
        }
    }

    /**
     * HTTP/2 does not have status messages. There is no simple way to get them from HttpClient.
     * Returns known status message for the code, otherwise empty string.
     *
     * @param code http status code
     * @return known status message for the code, otherwise empty string
     */
    public static String httpCodeToMessage( final int code )
    {
        switch ( code )
        {
            case 200:
                return "OK";
            case 201:
                return "Created";
            case 202:
                return "Accepted";
            case 203:
                return "Non-Authoritative Information";
            case 204:
                return "No Content";
            case 205:
                return "Reset Content";
            case 206:
                return "Partial Content";
            case 300:
                return "Multiple Choices";
            case 301:
                return "Moved Permanently";
            case 302:
                return "Found";
            case 303:
                return "See Other";
            case 304:
                return "Not Modified";
            case 305:
                return "Use Proxy";
            case 307:
                return "Temporary Redirect";
            case 308:
                return "Permanent Redirect";
            case 400:
                return "Bad Request";
            case 401:
                return "Unauthorized";
            case 402:
                return "Payment Required";
            case 403:
                return "Forbidden";
            case 404:
                return "Not Found";
            case 405:
                return "Method Not Allowed";
            case 406:
                return "Not Acceptable";
            case 407:
                return "Proxy Authentication Required";
            case 408:
                return "Request Timeout";
            case 409:
                return "Conflict";
            case 410:
                return "Gone";
            case 411:
                return "Length Required";
            case 412:
                return "Precondition Failed";
            case 413:
                return "Payload Too Large";
            case 414:
                return "URI Too Long";
            case 415:
                return "Unsupported Media Type";
            case 500:
                return "Internal Server Error";
            case 501:
                return "Not Implemented";
            case 502:
                return "Bad Gateway";
            case 503:
                return "Service Unavailable";
            case 504:
                return "Gateway Timeout";
            case 505:
                return "HTTP Version Not Supported";
            default:
                return "";
        }
    }
}
