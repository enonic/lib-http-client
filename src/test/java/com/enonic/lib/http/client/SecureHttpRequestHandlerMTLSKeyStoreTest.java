package com.enonic.lib.http.client;

import javax.net.ssl.SSLHandshakeException;

import org.junit.After;
import org.junit.Test;

import com.google.common.base.Throwables;

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

    @Test
    public void withNoClientCertificateGetRequest()
    {
        final MockResponse response = new MockResponse();
        response.setBody( "GET request" );
        response.setHeader( "content-type", "text/plain" );
        server.enqueue( response );

        Exception exception = assertThrows( RuntimeException.class,
                                            () -> runFunction( "/lib/test/secure-request-test.js", "withCertificatesGetRequest",
                                                               server.url( "/my/url" ), serverCertificateBytes ) );

        assertTrue(
            Throwables.getCausalChain( exception ).stream().map( Throwable::getClass ).anyMatch( c -> c == SSLHandshakeException.class ) );
    }
}
