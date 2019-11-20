package com.enonic.lib.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Collection;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

final class CertificateTools
{
    private CertificateTools()
    {
    }

    public static X509TrustManager buildTrustManagerWithCertificates( final InputStream inStream )
        throws GeneralSecurityException, IOException
    {
        final CertificateFactory certificateFactory = CertificateFactory.getInstance( "X.509" );
        final Collection<? extends Certificate> certificates = certificateFactory.generateCertificates( inStream );

        final KeyStore keyStore = KeyStore.getInstance( KeyStore.getDefaultType() );
        keyStore.load( null, null );

        int index = 0;
        for ( Certificate certificate : certificates )
        {
            keyStore.setCertificateEntry( String.valueOf( index ), certificate );
            index++;
        }
        if ( index == 0 )
        {
            throw new IllegalArgumentException( "No certificates provided" );
        }

        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance( TrustManagerFactory.getDefaultAlgorithm() );
        trustManagerFactory.init( keyStore );

        final TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        return (X509TrustManager) trustManagers[0];
    }

    public static void configureSocketFactory( final OkHttpClient.Builder clientBuilder, final X509TrustManager trustManager )
        throws GeneralSecurityException
    {
        final SSLContext sslContext;
        sslContext = SSLContext.getInstance( "TLS" );
        sslContext.init( null, new TrustManager[]{trustManager}, null );

        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        clientBuilder.sslSocketFactory( sslSocketFactory, trustManager );
    }
}
