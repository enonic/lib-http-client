package com.enonic.lib.http.client;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Base64;

import javax.net.ssl.SSLHandshakeException;

import org.junit.After;
import org.junit.Test;

import com.google.common.io.ByteSource;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;

import com.enonic.xp.testing.ScriptTestSupport;

import static org.junit.Assert.*;

public class SecureHttpRequestHandlerTest
    extends ScriptTestSupport
{

    private MockWebServer server;

    private HeldCertificate localhostCertificate;

    @Override
    public void initialize()
        throws Exception
    {
        super.initialize();
        String localhost = InetAddress.getByName( "localhost" ).getCanonicalHostName();
        localhostCertificate = new HeldCertificate.Builder().addSubjectAlternativeName( localhost ).build();
        HandshakeCertificates serverCertificates = new HandshakeCertificates.Builder().heldCertificate( localhostCertificate ).build();
        server = new MockWebServer();
        server.useHttps( serverCertificates.sslSocketFactory(), false );
        server.setProtocolNegotiationEnabled( false );
        server.start();

    }

    @After
    public void tearDown()
        throws Exception
    {
        server.shutdown();
    }

    @Test
    public void withCertificatesGetRequest()
        throws Exception

    {
        final MockResponse response = new MockResponse();
        response.setBody( "GET request" );
        response.setHeader( "content-type", "text/plain" );
        server.enqueue( response );

        final X509Certificate certificate = localhostCertificate.certificate();
        final ByteSource certificates = ByteSource.wrap(
            ( "-----BEGIN CERTIFICATE-----\n" + Base64.getEncoder().encodeToString( certificate.getEncoded() ) +
                "\n-----END CERTIFICATE-----" ).getBytes( StandardCharsets.ISO_8859_1 ) );

        runFunction( "/lib/test/secure-request-test.js", "withCertificatesGetRequest", server.url( "/my/url" ), certificates );
    }

    @Test
    public void withCertificatesGetRequest_no_match()
        throws Exception

    {
        final MockResponse response = new MockResponse();
        response.setBody( "GET request" );
        response.setHeader( "content-type", "text/plain" );
        server.enqueue( response );

        String localhost = InetAddress.getByName( "localhost" ).getCanonicalHostName();

        final X509Certificate anotherCertificate =
            new HeldCertificate.Builder().addSubjectAlternativeName( localhost ).build().certificate();
        final ByteSource certificates = ByteSource.wrap(
            ( "-----BEGIN CERTIFICATE-----\n" + Base64.getEncoder().encodeToString( anotherCertificate.getEncoded() ) +
                "\n-----END CERTIFICATE-----" ).getBytes( StandardCharsets.ISO_8859_1 ) );

        try
        {
            runFunction( "/lib/test/secure-request-test.js", "withCertificatesGetRequest", server.url( "/my/url" ), certificates );
        }
        catch ( Exception e )
        {
            assertEquals( SSLHandshakeException.class, e.getCause().getClass() );
        }
    }
}
