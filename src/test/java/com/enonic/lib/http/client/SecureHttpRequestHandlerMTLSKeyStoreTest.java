package com.enonic.lib.http.client;

import okhttp3.mockwebserver.MockResponse;
import org.junit.After;
import org.junit.Test;

import java.net.ConnectException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class SecureHttpRequestHandlerMTLSKeyStoreTest
    extends SecureHttpRequest
{
    private String keyStorePath;

    private final String keyStorePass = "key_store_pass";

    @Override
    public void initialize()
        throws Exception
    {
        super.initialize();
        setupServer( true );
        keyStorePath = setupKeyStore( keyStorePass );
    }

    @After
    public void clearTrustStore()
    {
        KeyStoreLoader.clear();
        System.clearProperty("javax.net.ssl.keyStore");
        System.clearProperty("javax.net.ssl.keyStorePassword");
    }

    @Test
    public void withClientCertificateGetRequest()
    {
        KeyStoreLoader.clear();
        System.setProperty("javax.net.ssl.keyStore", keyStorePath);
        System.setProperty("javax.net.ssl.keyStorePassword", keyStorePass);

        final MockResponse response = new MockResponse();
        response.setBody( "GET request" );
        response.setHeader( "content-type", "text/plain" );
        server.enqueue( response );

        runFunction(
            "/lib/test/secure-request-test.js",
            "withNoClientCertificatesGetRequest",
            server.url( "/my/url" ),
            serverCertificateBytes
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

        assertEquals(exception.getCause().getClass(), ConnectException.class);
    }
}
