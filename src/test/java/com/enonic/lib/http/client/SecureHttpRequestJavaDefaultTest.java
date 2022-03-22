package com.enonic.lib.http.client;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import okhttp3.mockwebserver.MockResponse;

public class SecureHttpRequestJavaDefaultTest
    extends SecureHttpRequest
{
    private String keyStorePath;

    private final String keyStorePass = "key_store_pass";

    private String trustStorePath;

    private final String trustStorePass = "trust_store_pass";

    @Override
    public void initialize()
        throws Exception
    {
        setupServer( true );
        super.initialize();
    }

    @After
    public void clear()
    {
        System.clearProperty( "javax.net.ssl.trustStore" );
        System.clearProperty( "javax.net.ssl.trustStorePassword" );
        System.clearProperty( "javax.net.ssl.keyStore" );
        System.clearProperty( "javax.net.ssl.keyStorePassword" );
    }

    @Test
    @Ignore("javax.net.ssl once read can't be reset. Test works in isolated JVM")
    public void withJavaDefault()
        throws Exception
    {
        keyStorePath = setupKeyStore( keyStorePass );
        trustStorePath = setupTrustStore( trustStorePass );

        System.setProperty( "javax.net.ssl.trustStore", trustStorePath );
        System.setProperty( "javax.net.ssl.trustStorePassword", trustStorePass );

        System.setProperty( "javax.net.ssl.keyStore", keyStorePath );
        System.setProperty( "javax.net.ssl.keyStorePassword", keyStorePass );

        final MockResponse response = new MockResponse();
        response.setBody( "GET request" );
        response.setHeader( "content-type", "text/plain" );
        server.enqueue( response );

        runFunction( "/lib/test/secure-request-test.js", "withNoCertificatesGetRequest", server.url( "/my/url" ) );
    }

    @Test
    @Ignore("javax.net.ssl once read can't be reset. Test works in isolated JVM")
    public void withJavaDefaultTrustStore()
        throws Exception
    {
        keyStorePath = setupKeyStore( keyStorePass );
        trustStorePath = setupTrustStore( trustStorePass );

        System.setProperty( "javax.net.ssl.trustStore", trustStorePath );
        System.setProperty( "javax.net.ssl.trustStorePassword", trustStorePass );

        final MockResponse response = new MockResponse();
        response.setBody( "GET request" );
        response.setHeader( "content-type", "text/plain" );
        server.enqueue( response );

        runFunction( "/lib/test/secure-request-test.js", "withCertificatesGetRequest", server.url( "/my/url" ), null,
                     clientCertificateBytes );
    }
}
