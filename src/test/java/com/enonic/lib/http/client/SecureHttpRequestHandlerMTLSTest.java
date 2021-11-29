package com.enonic.lib.http.client;

import java.net.SocketException;

import org.junit.Test;

import okhttp3.mockwebserver.MockResponse;

import static org.junit.Assert.*;

public class SecureHttpRequestHandlerMTLSTest
    extends SecureHttpRequest
{
    @Override
    public void initialize()
        throws Exception
    {
        super.initialize();
        setupServer( true );
    }

    @Test
    public void withClientCertificateGetRequest()
    {
        final MockResponse response = new MockResponse();
        response.setBody( "GET request" );
        response.setHeader( "content-type", "text/plain" );
        server.enqueue( response );

        runFunction(
            "/lib/test/secure-request-test.js",
            "withClientCertificatesGetRequest",
            server.url( "/my/url" ),
            serverCertificateBytes,
            clientCertificateBytes
        );
    }

    @Test
    public void withNoClientCertificateGetRequest()
    {
        final MockResponse response = new MockResponse();
        response.setBody( "GET request" );
        response.setHeader( "content-type", "text/plain" );
        server.enqueue( response );

        Exception exception = assertThrows(RuntimeException.class, () -> runFunction(
            "/lib/test/secure-request-test.js",
            "withNoClientCertificatesGetRequest",
            server.url( "/my/url" ),
            serverCertificateBytes
        ));

        assertTrue( exception.getCause() instanceof SocketException );
    }
}
