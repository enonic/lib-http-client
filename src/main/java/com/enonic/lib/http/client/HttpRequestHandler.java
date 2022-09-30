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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.github.mizosoft.methanol.MoreBodySubscribers;
import com.google.common.io.ByteSource;

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

    private boolean allowHttp2;

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
                                                                       .allowHttp2( allowHttp2 && Utils.isSecure( request.uri() ) )
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
                                                                       .build(), request.uri() );

        return client.send( request, mapToFullyReadByteSource(
            MoreBodySubscribers.withReadTimeout( HttpResponse.BodySubscribers.ofInputStream(),
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

    public static HttpResponse.BodyHandler<Supplier<ByteSource>> mapToFullyReadByteSource(
        final HttpResponse.BodySubscriber<InputStream> upstream )
    {
        return responseInfo -> HttpResponse.BodySubscribers.mapping( upstream, is -> () -> {
            try (InputStream body = is)
            {
                final BufferedBytesProcessor processor = BufferedBytesProcessor.read( body );

                if ( processor.totalRead == 0 )
                {
                    return ByteSource.empty();
                }
                else if ( processor.readFully )
                {
                    return ByteSource.concat( processor.chunks );
                }
                else
                {
                    final Path tempFile = Files.createTempFile( "xphttp", ".tmp" );
                    Files.copy( body, tempFile, StandardCopyOption.REPLACE_EXISTING );
                    return ByteSource.concat( ByteSource.concat( processor.chunks ), new RefPathByteSource( tempFile ) );
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

    public void setAllowHttp2( final boolean allowHttp2 )
    {
        this.allowHttp2 = allowHttp2;
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

    static class BufferedBytesProcessor
    {
        private static final int BUFFER_SIZE = 8192;

        final List<ByteSource> chunks = new ArrayList<>();

        int totalRead;

        boolean readFully;

        static BufferedBytesProcessor read( final InputStream body )
            throws IOException
        {
            final BufferedBytesProcessor processor = new BufferedBytesProcessor();
            processor.readBytes( body );
            return processor;
        }

        void readBytes( InputStream input )
            throws IOException
        {
            byte[] buf = new byte[BUFFER_SIZE];
            int read;
            do
            {
                read = input.read( buf );
                if ( read == -1 )
                {
                    readFully = true;
                    return;
                }
            }
            while ( processBytes( buf, read ) );
        }

        boolean processBytes( final byte[] buf, final int len )
        {
            chunks.add( ByteSource.wrap( Arrays.copyOfRange( buf, 0, len ) ) );
            totalRead += len;
            return totalRead < MAX_IN_MEMORY_BODY_STREAM_BYTES;
        }
    }
}
