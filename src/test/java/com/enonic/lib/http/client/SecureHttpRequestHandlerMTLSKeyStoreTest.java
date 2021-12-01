package com.enonic.lib.http.client;

import java.net.SocketException;

import org.junit.After;
import org.junit.Test;

import okhttp3.mockwebserver.MockResponse;

import static org.junit.Assert.*;

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
        KeyStoreLoader.clearCache();
        System.clearProperty( "com.enonic.lib.http.client.keyStore" );
        System.clearProperty( "com.enonic.lib.http.client.keyStorePassword" );
    }

    @Test
    public void withClientCertificateGetRequest()
    {
        KeyStoreLoader.clearCache();
        System.setProperty( "com.enonic.lib.http.client.keyStore", keyStorePath );
        System.setProperty( "com.enonic.lib.http.client.keyStorePassword", keyStorePass );

        final MockResponse response = new MockResponse();
        response.setBody( "GET request" );
        response.setHeader( "content-type", "text/plain" );
        server.enqueue( response );

        runFunction( "/lib/test/secure-request-test.js", "withNoClientCertificatesGetRequest", server.url( "/my/url" ),
                     serverCertificateBytes );
    }

    @Test
    public void withClientCertificateAliasGetRequest()
    {
        KeyStoreLoader.clearCache();
        System.setProperty( "com.enonic.lib.http.client.keyStore", keyStorePath );
        System.setProperty( "com.enonic.lib.http.client.keyStorePassword", keyStorePass );

        final MockResponse response = new MockResponse();
        response.setBody( "GET request" );
        response.setHeader( "content-type", "text/plain" );
        server.enqueue( response );

        runFunction( "/lib/test/secure-request-test.js", "withClientCertificateAliasGetRequest", server.url( "/my/url" ),
                     serverCertificateBytes );
    }

    @Test
    public void withNoClientCertificateGetRequest()
    {
        final MockResponse response = new MockResponse();
        response.setBody( "GET request" );
        response.setHeader( "content-type", "text/plain" );
        server.enqueue( response );

        Exception exception = assertThrows( RuntimeException.class,
                                            () -> runFunction( "/lib/test/secure-request-test.js", "withNoClientCertificatesGetRequest",
                                                               server.url( "/my/url" ), serverCertificateBytes ) );

        assertTrue( exception.getCause() instanceof SocketException );
    }
}
