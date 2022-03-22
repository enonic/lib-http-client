package com.enonic.lib.http.client;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import com.github.mizosoft.methanol.MediaType;
import com.github.mizosoft.methanol.MoreBodyPublishers;
import com.github.mizosoft.methanol.MultipartBodyPublisher;
import com.google.common.io.ByteSource;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

class HttpRequestFactory
{
    private HttpRequestFactory()
    {
    }

    static class RequestParams
    {
        private final String method;

        private final String url;

        private final Map<String, String> form;

        private final Map<String, String> queryParams;

        private final Map<String, String> headers;

        private final String contentType;

        private final Object body;

        private final List<Map<String, Object>> multipart;

        private final boolean formIsQueryParams;

        RequestParams( final Builder builder )
        {
            this.method = requireNonNullElse( builder.method, "GET" ).trim().toUpperCase( Locale.ROOT );
            this.formIsQueryParams = "GET".equals( this.method ) || "HEAD".equals( this.method );
            this.url = requireNonNull( builder.url );
            this.form = builder.form == null
                ? null
                : toStringStringMap(builder.form);
            this.queryParams = builder.queryParams != null ? toStringStringMap( builder.queryParams ) : null;

            this.headers = requireNonNullElse( builder.headers, Map.of() );
            this.contentType = builder.contentType;
            this.body = builder.body;
            this.multipart = builder.multipart;
        }

        private static Map<String, String> toStringStringMap( final Map<String, Object> map )
        {
            return map.entrySet()
                .stream()
                .filter( e -> e.getValue() != null )
                .collect( Collectors.toMap( Map.Entry::getKey, e -> e.getValue().toString() ) );
        }

        static class Builder
        {
            private Builder()
            {
            }

            private String url;

            private Map<String, Object> form;

            private Map<String, Object> queryParams;

            private String method;

            private Map<String, String> headers;

            private String contentType;

            private Object body;

            private List<Map<String, Object>> multipart;

            Builder url( final String url )
            {
                this.url = url;
                return this;
            }

            Builder method( final String method )
            {
                this.method = method;
                return this;
            }

            Builder form( final Map<String, Object> form )
            {
                this.form = form;
                return this;
            }

            Builder queryParams( final Map<String, Object> queryParams )
            {
                this.queryParams = queryParams;
                return this;
            }

            Builder headers( final Map<String, String> headers )
            {
                this.headers = headers;
                return this;
            }

            Builder contentType( final String contentType )
            {
                this.contentType = contentType;
                return this;
            }

            Builder body( final Object body )
            {
                this.body = body;
                return this;
            }

            Builder multipart( final List<Map<String, Object>> multipart )
            {
                this.multipart = multipart;
                return this;
            }

            RequestParams build()
            {
                return new RequestParams( this );
            }
        }
    }

    static RequestParams.Builder params()
    {
        return new RequestParams.Builder();
    }

    static HttpRequest getHttpRequest( final RequestParams params )
    {
        final URI uri;
        if ( params.queryParams != null )
        {
            uri = Utils.addQueryParams( URI.create( params.url ), params.queryParams );
        }
        else if ( params.form != null && params.formIsQueryParams )
        {
            uri = Utils.addQueryParams( URI.create( params.url ), params.form );
        }
        else
        {
            uri = URI.create( params.url );
        }
        final HttpRequest.Builder request = HttpRequest.newBuilder( uri );

        params.headers.forEach( request::header );

        setRequestBody( params, request );

        return request.build();
    }

    private static void setRequestBody( final RequestParams params, final HttpRequest.Builder request )
    {
        final HttpRequest.BodyPublisher requestBody;
        if ( params.form != null && !params.formIsQueryParams )
        {
            request.setHeader( "content-type", "application/x-www-form-urlencoded" );
            requestBody = HttpRequest.BodyPublishers.ofString( Utils.formUrlEncoded( params.form ) );
        }
        else if ( params.body != null )
        {
            if ( params.contentType != null )
            {
                request.setHeader( "content-type", params.contentType );
            }
            if ( params.body instanceof ByteSource )
            {
                requestBody = byteSourceBodyPublisher( (ByteSource) params.body );
            }
            else
            {
                requestBody = HttpRequest.BodyPublishers.ofString( params.body.toString() );
            }
        }
        else if ( params.multipart != null )
        {
            requestBody = buildMultipartBody( params.multipart, params.contentType );
        }
        else
        {
            requestBody = HttpRequest.BodyPublishers.noBody();
        }
        request.method( params.method, requestBody );
    }

    private static HttpRequest.BodyPublisher buildMultipartBody( final List<Map<String, Object>> multipart, String contentType )
    {
        String boundary = new BigInteger( 256, ThreadLocalRandom.current() ).toString( 32 );

        final MultipartBodyPublisher.Builder builder = MultipartBodyPublisher.newBuilder().boundary( boundary );

        if ( contentType != null )
        {
            builder.mediaType( MediaType.parse( contentType ) );
        }

        for ( Map<String, Object> multipartItem : multipart )
        {
            final String name = getValue( multipartItem, "name" );
            final String fileName = getValue( multipartItem, "fileName" );
            final String partContentType = getValue( multipartItem, "contentType" );
            final Object value = multipartItem.get( "value" );
            if ( name == null || value == null || name.isBlank() )
            {
                continue;
            }

            if ( value instanceof ByteSource )
            {
                final HttpRequest.BodyPublisher bodyPublisher;
                if ( partContentType == null )
                {
                    bodyPublisher = byteSourceBodyPublisher( (ByteSource) value );
                }
                else
                {
                    bodyPublisher =
                        MoreBodyPublishers.ofMediaType( byteSourceBodyPublisher( (ByteSource) value ), MediaType.parse( partContentType ) );
                }

                if ( fileName == null )
                {
                    builder.formPart( name, bodyPublisher );
                }
                else
                {
                    builder.formPart( name, fileName, bodyPublisher );
                }
            }
            else
            {
                builder.textPart( name, value );
            }
        }

        return builder.build();
    }

    private static String getValue( final Map<String, Object> object, final String key )
    {
        final Object value = object.get( key );
        return value != null ? value.toString() : null;
    }

    private static HttpRequest.BodyPublisher byteSourceBodyPublisher( final ByteSource byteSource )
    {
        return HttpRequest.BodyPublishers.ofInputStream( () -> {
            try
            {
                return byteSource.openBufferedStream();
            }
            catch ( IOException e )
            {
                throw new UncheckedIOException( e );
            }
        } );
    }
}
