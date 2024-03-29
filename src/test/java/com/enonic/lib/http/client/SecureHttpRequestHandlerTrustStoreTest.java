package com.enonic.lib.http.client;

import javax.net.ssl.SSLHandshakeException;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Throwables;

import okhttp3.mockwebserver.MockResponse;

import static org.junit.Assert.*;

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
        System.clearProperty( "javax.net.ssl.trustStore" );
        System.clearProperty( "javax.net.ssl.trustStorePassword" );
    }

    @Test
    @Ignore("javax.net.ssl once read can't be reset. Test works in isolated JVM")
    public void withTrustStoreGetRequest()
    {
        System.setProperty( "javax.net.ssl.trustStore", trustStorePath );
        System.setProperty( "javax.net.ssl.trustStorePassword", trustStorePass );

        final MockResponse response = new MockResponse();
        response.setBody( "GET request" );
        response.setHeader( "content-type", "text/plain" );
        server.enqueue( response );

        runFunction( "/lib/test/secure-request-test.js", "withNoCertificatesGetRequest", server.url( "/my/url" ) );
    }

    @Test
    public void withTrustStoreGetRequest_no_match()
    {
        final MockResponse response = new MockResponse();
        response.setBody( "GET request" );
        response.setHeader( "content-type", "text/plain" );
        server.enqueue( response );

        Exception exception = assertThrows( RuntimeException.class,
                                            () -> runFunction( "/lib/test/secure-request-test.js", "withNoCertificatesGetRequest",
                                                               server.url( "/my/url" ) ) );

        assertTrue(
            Throwables.getCausalChain( exception ).stream().map( Throwable::getClass ).anyMatch( c -> c == SSLHandshakeException.class ) );
    }
}
