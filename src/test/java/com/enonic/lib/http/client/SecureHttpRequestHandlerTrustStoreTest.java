package com.enonic.lib.http.client;

import okhttp3.mockwebserver.MockResponse;
import org.junit.After;
import org.junit.Test;

import javax.net.ssl.SSLHandshakeException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class SecureHttpRequestHandlerTrustStoreTest
    extends SecureHttpRequest
{
    private String trustStorePath;

    private final String trustStorePass = "trust_store_pass";

    @Override
    public void initialize()
        throws Exception
    {
        super.initialize();
        setupServer( false );
        trustStorePath = setupTrustStore( trustStorePass );
    }

    @After
    public void clearTrustStore()
    {
        System.clearProperty("javax.net.ssl.trustStore");
        System.clearProperty("javax.net.ssl.trustStorePassword");
    }

    @Test
    public void withTrustStoreGetRequest()
    {
        System.setProperty("javax.net.ssl.trustStore", trustStorePath);
        System.setProperty("javax.net.ssl.trustStorePassword", trustStorePass);

        final MockResponse response = new MockResponse();
        response.setBody( "GET request" );
        response.setHeader( "content-type", "text/plain" );
        server.enqueue( response );

        runFunction(
            "/lib/test/secure-request-test.js",
            "withNoCertificatesGetRequest",
            server.url( "/my/url" )
        );
    }

    @Test
    public void withTrustStoreGetRequest_no_match()
    {
        final MockResponse response = new MockResponse();
        response.setBody( "GET request" );
        response.setHeader( "content-type", "text/plain" );
        server.enqueue( response );

        Exception exception = assertThrows(RuntimeException.class, () -> runFunction(
                "/lib/test/secure-request-test.js",
                "withNoCertificatesGetRequest",
                server.url( "/my/url" )
        ));

        assertEquals(exception.getCause().getClass(), SSLHandshakeException.class);
    }
}
