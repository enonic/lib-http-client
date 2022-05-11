package com.enonic.lib.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.github.mizosoft.methanol.MoreBodySubscribers;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;

import com.enonic.xp.trace.Trace;
import com.enonic.xp.trace.Tracer;

import static java.util.Objects.requireNonNullElse;

@SuppressWarnings("WeakerAccess")
public final class HttpRequestHandler
{
    private static final long DEFAULT_READ_TIMEOUT = 10_000;

    static final int MAX_IN_MEMORY_BODY_STREAM_BYTES = 10 * 1024 * 1024;

    private String url;

    private Map<String, Object> params;

    private Map<String, Object> queryParams;

    private String method;

    private Map<String, String> headers;

    private Long readTimeout;

    private String contentType;

    private Object body;

    private List<Map<String, Object>> multipart;

    private Long connectionTimeout;

    private String authUser;

    private String authPassword;

    private String proxyHost;

    private Integer proxyPort;

    private String proxyUser;

    private String proxyPassword;

    private Boolean followRedirects;

    private ByteSource certificates;

    private ByteSource clientCertificate;

    @SuppressWarnings("unused")
    public ResponseMapper request()
        throws Exception
    {
        final Trace trace = startTracing();
        final HttpResponse<Supplier<ByteSource>> response = Tracer.traceEx( trace, this::executeRequest );
        endTracing( trace, response );

        return new ResponseMapper( response );
    }

    private HttpResponse<Supplier<ByteSource>> executeRequest()
        throws IOException, InterruptedException
    {
        final HttpRequest request = HttpRequestFactory.getHttpRequest( HttpRequestFactory.params()
                                                                           .method( method )
                                                                           .url( url )
                                                                           .headers( headers )
                                                                           .contentType( contentType )
                                                                           .form( params )
                                                                           .queryParams( queryParams )
                                                                           .body( body )
                                                                           .multipart( multipart )
                                                                           .build() );

        final HttpClient client = HttpClientFactory.getHttpClient( HttpClientFactory.params()
                                                                       .disableHttp2( !url.startsWith( "https://" ) )
                                                                       .connectTimeout( connectionTimeout )
                                                                       .authUser( authUser )
                                                                       .authPassword( authPassword )
                                                                       .proxyUser( proxyUser )
                                                                       .proxyPassword( proxyPassword )
                                                                       .proxyHost( proxyHost )
                                                                       .proxyPort( proxyPort )
                                                                       .followRedirects( followRedirects )
                                                                       .certificates( certificates )
                                                                       .clientCertificate( clientCertificate )
                                                                       .build() );

        return client.send( request, mapToFullyReadByteSource( request.method(), MoreBodySubscribers.withReadTimeout(
            HttpResponse.BodySubscribers.ofInputStream(),
            Duration.ofMillis( requireNonNullElse( readTimeout, DEFAULT_READ_TIMEOUT ) ) ) ) );
    }

    private Trace startTracing()
    {
        final Trace trace = Tracer.newTrace( "httpClient" );
        if ( trace == null )
        {
            return null;
        }
        trace.put( "traceName", "HttpClient" );
        trace.put( "url", url );
        trace.put( "method", method );
        return trace;
    }

    private void endTracing( final Trace trace, final HttpResponse<?> response )
    {
        if ( trace == null )
        {
            return;
        }

        trace.put( "method", response.request().method() );
        trace.put( "status", response.statusCode() );
        trace.put( "type", response.headers().firstValue( "content-type" ).orElse( null ) );
        final long contentLength = Utils.getContentLength( response.headers() );
        if ( contentLength >= 0 )
        {
            trace.put( "size", contentLength );
        }
    }

    public static HttpResponse.BodyHandler<Supplier<ByteSource>> mapToFullyReadByteSource( final String requestMethod,
                                                                                           final HttpResponse.BodySubscriber<InputStream> upstream )
    {
        return responseInfo -> HttpResponse.BodySubscribers.mapping( upstream, is -> () -> {
            Long length = "HEAD".equals( requestMethod ) ? null : Utils.expectBodyOfLength( responseInfo );

            try (InputStream body = is)
            {
                if ( length == null || length.equals( 0L ) )
                {
                    return ByteSource.empty();
                }

                final boolean unlimitedBodyLength = length == -1;

                final InputStream bodyStream = unlimitedBodyLength ? body : ByteStreams.limit( body, length );

                if ( unlimitedBodyLength || length > MAX_IN_MEMORY_BODY_STREAM_BYTES )
                {
                    final Path tempFile = Files.createTempFile( "xphttp", ".tmp" );
                    Files.copy( bodyStream, tempFile, StandardCopyOption.REPLACE_EXISTING );
                    return new RefPathByteSource( tempFile );
                }
                else
                {
                    return ByteSource.wrap( bodyStream.readAllBytes() );
                }
            }
            catch ( IOException e )
            {
                throw new UncheckedIOException( e.getMessage(), e );
            }
        } );
    }

    @SuppressWarnings("unused")
    public void setContentType( final String contentType )
    {
        this.contentType = contentType;
    }

    @SuppressWarnings("unused")
    public void setBody( final Object value )
    {
        this.body = value;
    }

    @SuppressWarnings("unused")
    public void setHeaders( final Map<String, String> headers )
    {
        this.headers = headers;
    }

    @SuppressWarnings("unused")
    public void setUrl( final String value )
    {
        this.url = value;
    }

    @SuppressWarnings("unused")
    public void setParams( final Map<String, Object> params )
    {
        this.params = params;
    }

    @SuppressWarnings("unused")
    public void setQueryParams( final Map<String, Object> queryParams )
    {
        this.queryParams = queryParams;
    }

    @SuppressWarnings("unused")
    public void setMethod( final String value )
    {
        this.method = value;
    }

    @SuppressWarnings("unused")
    public void setConnectionTimeout( final Long value )
    {
        this.connectionTimeout = value;
    }

    @SuppressWarnings("unused")
    public void setReadTimeout( final Long value )
    {
        this.readTimeout = value;
    }

    @SuppressWarnings("unused")
    public void setMultipart( final List<Map<String, Object>> multipart )
    {
        this.multipart = multipart;
    }

    @SuppressWarnings("unused")
    public void setProxyHost( final String proxyHost )
    {
        this.proxyHost = proxyHost;
    }

    @SuppressWarnings("unused")
    public void setProxyPort( final Integer proxyPort )
    {
        this.proxyPort = proxyPort;
    }

    @SuppressWarnings("unused")
    public void setProxyUser( final String proxyUser )
    {
        this.proxyUser = proxyUser;
    }

    @SuppressWarnings("unused")
    public void setProxyPassword( final String proxyPassword )
    {
        this.proxyPassword = proxyPassword;
    }

    @SuppressWarnings("unused")
    public void setAuthUser( final String authUser )
    {
        this.authUser = authUser;
    }

    @SuppressWarnings("unused")
    public void setAuthPassword( final String authPassword )
    {
        this.authPassword = authPassword;
    }

    @SuppressWarnings("unused")
    public void setFollowRedirects( final Boolean followRedirects )
    {
        this.followRedirects = followRedirects;
    }

    @SuppressWarnings("unused")
    public void setCertificates( final ByteSource certificates )
    {
        this.certificates = certificates;
    }

    @SuppressWarnings("unused")
    public void setClientCertificate( final ByteSource clientCertificate )
    {
        this.clientCertificate = clientCertificate;
    }
}
