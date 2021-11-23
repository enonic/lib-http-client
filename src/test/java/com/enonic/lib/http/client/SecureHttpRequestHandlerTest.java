package com.enonic.lib.http.client;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.SSLHandshakeException;

import org.junit.Test;

import com.google.common.io.ByteSource;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.tls.HeldCertificate;

import static org.junit.Assert.*;

public class SecureHttpRequestHandlerTest
    extends SecureHttpRequest
{
    @Override
    public void initialize()
        throws Exception
    {
        super.initialize();
        setupServer( false );
    }

    @Test
    public void withCertificatesGetRequest()
    {
        final MockResponse response = new MockResponse();
        response.setBody( "GET request" );
        response.setHeader( "content-type", "text/plain" );
        server.enqueue( response );

        runFunction(
            "/lib/test/secure-request-test.js",
            "withCertificatesGetRequest",
            server.url( "/my/url" ),
            serverCertificateBytes
        );
    }

    @Test
    public void withCertificatesGetRequest_no_match()
        throws Exception
    {
        final MockResponse response = new MockResponse();
        response.setBody( "GET request" );
        response.setHeader( "content-type", "text/plain" );
        server.enqueue( response );

        // Create come invalid server certificate
        HeldCertificate invalidServerCertificate = new HeldCertificate.Builder()
                .commonName("server")
                .addSubjectAlternativeName(InetAddress.getByName( "localhost" ).getCanonicalHostName())
                .build();
        ByteSource invalidServerCertificateBytes = ByteSource.wrap(invalidServerCertificate
                .certificatePem().getBytes( StandardCharsets.ISO_8859_1 ));

        Exception exception = assertThrows(RuntimeException.class, () -> runFunction(
            "/lib/test/secure-request-test.js",
            "withCertificatesGetRequest",
            server.url( "/my/url" ),
            invalidServerCertificateBytes
        ));

        assertEquals(exception.getCause().getClass(), SSLHandshakeException.class);
    }
}
