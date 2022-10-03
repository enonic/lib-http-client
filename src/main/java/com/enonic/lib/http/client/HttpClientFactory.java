package com.enonic.lib.http.client;

import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.CharBuffer;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;

import com.github.mizosoft.methanol.Methanol;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;

import nl.altindag.ssl.SSLFactory;

import static java.util.Objects.requireNonNullElse;

class HttpClientFactory
{
    private static final long IDLE_TIMEOUT_MS = Long.getLong( "com.enonic.lib.http.client.idle.timeout", 30000 );

    private static final long DEFAULT_CONNECT_TIMEOUT_MS = 10_000;

    private static final int DEFAULT_PROXY_PORT = 8080;

    private static final ConcurrentMap<String, HttpClientWrapper> CACHE = new ConcurrentHashMap<>();

    private static final Executor SHARED_WORKERS_EXECUTOR = Executors.newCachedThreadPool( new SharedWorkerThreadFactory() );

    private HttpClientFactory()
    {
    }

    private static class HttpClientWrapper
    {
        final HttpClient client;

        final Map<String, Long> lastAccess;

        public HttpClientWrapper( final HttpClient client, Map<String, Long> lastAccess )
        {
            this.client = client;
            this.lastAccess = lastAccess;
        }
    }

    static class ClientParams
    {
        final Duration connectTimeout;

        final PasswordAuthentication serverAuth;

        final PasswordAuthentication proxyAuth;

        final InetSocketAddress proxy;

        final HttpClient.Redirect followRedirects;

        final byte[] certificates;

        final byte[] clientCertificate;

        final boolean disableHttp2;

        private ClientParams( final Builder builder )
            throws IOException
        {
            this.disableHttp2 = builder.disableHttp2;

            this.connectTimeout = Duration.ofMillis( requireNonNullElse( builder.connectTimeout, DEFAULT_CONNECT_TIMEOUT_MS ) );

            if ( !requireNonNullElse( builder.authUser, "" ).isEmpty() && builder.authPassword != null )
            {
                this.serverAuth = new PasswordAuthentication( builder.authUser, builder.authPassword.toCharArray() );
            }
            else
            {
                this.serverAuth = null;
            }

            if ( !requireNonNullElse( builder.proxyUser, "" ).isEmpty() && builder.proxyPassword != null )
            {
                this.proxyAuth = new PasswordAuthentication( builder.proxyUser, builder.proxyPassword.toCharArray() );
            }
            else
            {
                this.proxyAuth = null;
            }

            if ( !requireNonNullElse( builder.proxyHost, "" ).isBlank() )
            {
                this.proxy = new InetSocketAddress( builder.proxyHost, requireNonNullElse( builder.proxyPort, DEFAULT_PROXY_PORT ) );
            }
            else
            {
                this.proxy = null;
            }

            this.followRedirects = builder.followRedirects == null
                ? HttpClient.Redirect.NORMAL
                : ( builder.followRedirects ? HttpClient.Redirect.ALWAYS : HttpClient.Redirect.NEVER );

            this.certificates = builder.certificates != null ? builder.certificates.read() : null;

            this.clientCertificate = builder.clientCertificate != null ? builder.clientCertificate.read() : null;
        }

        static class Builder
        {
            private Builder()
            {
            }

            private boolean disableHttp2;

            private Long connectTimeout;

            private String authUser;

            private String authPassword;

            private String proxyHost;

            private Integer proxyPort;

            private String proxyUser;

            private String proxyPassword;

            private Boolean followRedirects;

            private ByteSource clientCertificate;

            private ByteSource certificates;

            Builder disableHttp2( final boolean disableHttp2 )
            {
                this.disableHttp2 = disableHttp2;
                return this;
            }

            Builder connectTimeout( final Long connectTimeout )
            {
                this.connectTimeout = connectTimeout;
                return this;
            }

            Builder authUser( final String authUser )

            {
                this.authUser = authUser;
                return this;
            }

            Builder authPassword( final String authPassword )
            {
                this.authPassword = authPassword;
                return this;
            }

            Builder proxyHost( final String proxyHost )
            {
                this.proxyHost = proxyHost;
                return this;
            }

            Builder proxyPort( final Integer proxyPort )
            {
                this.proxyPort = proxyPort;
                return this;
            }

            Builder proxyUser( final String proxyUser )
            {
                this.proxyUser = proxyUser;
                return this;
            }

            Builder proxyPassword( final String proxyPassword )
            {
                this.proxyPassword = proxyPassword;
                return this;
            }

            Builder followRedirects( final Boolean followRedirects )
            {
                this.followRedirects = followRedirects;
                return this;
            }

            Builder clientCertificate( final ByteSource clientCertificate )
            {
                this.clientCertificate = clientCertificate;
                return this;
            }

            Builder certificates( final ByteSource certificates )
            {
                this.certificates = certificates;
                return this;
            }

            ClientParams build()
                throws IOException
            {
                return new ClientParams( this );
            }
        }
    }

    static ClientParams.Builder params()
    {
        return new ClientParams.Builder();
    }

    static void clearCache()
    {
        CACHE.clear();
    }

    static HttpClient getHttpClient( final ClientParams params, final URI uri )
    {
        return CACHE.compute( cacheKey( params ), ( key, old ) -> {
            if ( IDLE_TIMEOUT_MS <= 0 )
            {
                return old != null ? old : new HttpClientWrapper( createClient( params ), null );
            }
            final String keyForRequest = keyForRequest( uri, params.proxy );
            final long currentTimeMillis = System.currentTimeMillis();

            final Long lastAccess = old != null ? old.lastAccess.get( keyForRequest ) : null;

            if ( old != null && ( lastAccess == null || lastAccess > currentTimeMillis - IDLE_TIMEOUT_MS ) )
            {
                old.lastAccess.put( keyForRequest, currentTimeMillis );
                return old;
            }
            else
            {
                return new HttpClientWrapper( createClient( params ), new HashMap<>( Map.of( keyForRequest, currentTimeMillis ) ) );
            }
        } ).client;
    }

    private static String keyForRequest( final URI uri, final InetSocketAddress proxy )
    {
        final boolean isSecure = Utils.isSecure( uri );
        final String host = uri.getHost();
        final int port = uri.getPort() == -1 ? ( isSecure ? 443 : 80 ) : uri.getPort();

        final StringBuilder keyBuilder = new StringBuilder( isSecure ? "S:" : "C:" );

        if ( proxy == null )
        {
            keyBuilder.append( "H:" ).append( host ).append( ":" ).append( port );
        }
        else
        {
            if ( isSecure )
            {
                keyBuilder.append( "T:H:" ).append( host ).append( ":" ).append( port ).append( ";" );
            }
            final String proxyHost = proxy.getHostString();
            final int proxyPort = proxy.getPort();

            keyBuilder.append( "P:" ).append( proxyHost ).append( ":" ).append( proxyPort );
        }
        return keyBuilder.toString();
    }

    private static String cacheKey( final ClientParams params )
    {
        final Hasher hasher = Hashing.sha512().newHasher();

        hasher.putBoolean( params.disableHttp2 );

        hasher.putLong( params.connectTimeout.toMillis() );

        if ( params.serverAuth != null )
        {
            hasher.putUnencodedChars( params.serverAuth.getUserName() )
                .putUnencodedChars( CharBuffer.wrap( params.serverAuth.getPassword() ) );
        }

        hasher.putInt( 0 );

        if ( params.proxyAuth != null )
        {
            hasher.putUnencodedChars( params.proxyAuth.getUserName() )
                .putUnencodedChars( CharBuffer.wrap( params.proxyAuth.getPassword() ) );
        }

        hasher.putInt( 0 );

        if ( params.proxy != null )
        {
            hasher.putUnencodedChars( params.proxy.toString() );
        }

        hasher.putInt( 0 );

        hasher.putInt( params.followRedirects.ordinal() );

        if ( params.certificates != null )
        {
            hasher.putBytes( params.certificates );
        }

        hasher.putInt( 0 );

        if ( params.clientCertificate != null )
        {
            hasher.putBytes( params.clientCertificate );
        }

        hasher.putInt( 0 );

        return hasher.hash().toString();
    }

    private static HttpClient createClient( final ClientParams params )
    {
        final var clientBuilder = Methanol.newBuilder();
        clientBuilder.headersTimeout( params.connectTimeout );
        if ( params.disableHttp2 )
        {
            clientBuilder.version( HttpClient.Version.HTTP_1_1 );
        }
        clientBuilder.followRedirects( params.followRedirects );
        clientBuilder.executor( SHARED_WORKERS_EXECUTOR );
        setupProxy( params, clientBuilder );
        setupAuthenticator( params, clientBuilder );
        setupHandshakeCertificates( params, clientBuilder );

        return clientBuilder.build();
    }

    private static void setupProxy( final ClientParams params, final HttpClient.Builder clientBuilder )
    {
        if ( params.proxy != null )
        {
            clientBuilder.proxy( ProxySelector.of( params.proxy ) );
        }
    }

    private static void setupAuthenticator( final ClientParams params, final HttpClient.Builder clientBuilder )
    {
        final PasswordAuthentication serverAuth = params.serverAuth;
        final PasswordAuthentication proxyAuth = params.proxyAuth;

        if ( serverAuth != null || proxyAuth != null )
        {
            clientBuilder.authenticator( new Authenticator()
            {
                @Override
                protected PasswordAuthentication getPasswordAuthentication()
                {
                    switch ( this.getRequestorType() )
                    {
                        case PROXY:
                            return proxyAuth;
                        case SERVER:
                            return serverAuth;
                        default:
                            return null;
                    }
                }
            } );
        }
    }

    private static void setupHandshakeCertificates( final ClientParams params, final HttpClient.Builder clientBuilder )
    {
        if ( params.certificates != null || params.clientCertificate != null )
        {
            final SSLFactory.Builder sslFactoryBuilder = SSLFactory.builder();

            if ( params.certificates != null )
            {
                // Add custom CA certificates
                sslFactoryBuilder.withTrustMaterial( CertificateTools.loadCertificates( params.certificates ) );
            }
            else
            {
                // Add default trusted CA certificates
                sslFactoryBuilder.withDefaultTrustMaterial();
            }

            if ( params.clientCertificate != null )
            {
                // Add custom client key and certificate
                final CertWithKey clientCert = CertificateTools.loadClientCertificate( params.clientCertificate );
                sslFactoryBuilder.withIdentityMaterial( clientCert.getKey(), null, clientCert.getCertificate() );
            }

            // Set certificates
            final SSLFactory sslFactory = sslFactoryBuilder.build();

            final SSLContext sslContext = sslFactory.getSslContext();

            clientBuilder.sslContext( sslContext );
            clientBuilder.sslParameters( sslFactory.getSslParameters() );
        }
    }
}
