package com.enonic.lib.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import com.google.common.io.ByteSource;

import okhttp3.OkHttpClient;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;

final class CertificateTools
{
    private final ByteSource certificates;

    private final ByteSource clientCertificate;

    public CertificateTools( final ByteSource certificates, final ByteSource clientCertificate )
    {
        this.certificates = certificates;
        this.clientCertificate = clientCertificate;
    }

    public void setupHandshakeCertificates( final OkHttpClient.Builder clientBuilder )
        throws IOException
    {
        if ( certificates != null || clientCertificate != null )
        {
            final HandshakeCertificates.Builder handshakeCertificatesBuilder = new HandshakeCertificates.Builder();

            if ( certificates != null )
            {
                try (InputStream certificatesStream = certificates.openStream())
                {
                    final CertificateFactory certificateFactory = CertificateFactory.getInstance( "X.509" );
                    certificateFactory.generateCertificates( certificatesStream )
                        .stream()
                        .map( certificate -> (X509Certificate) certificate )
                        .forEach( handshakeCertificatesBuilder::addTrustedCertificate );
                }
                catch ( GeneralSecurityException e )
                {
                    throw new RuntimeException( e );
                }
            }
            else
            {
                handshakeCertificatesBuilder.addPlatformTrustedCertificates();
            }
            if ( clientCertificate != null )
            {
                handshakeCertificatesBuilder.heldCertificate(
                    HeldCertificate.decode( new String( clientCertificate.read(), StandardCharsets.ISO_8859_1 ) ) );
            }
            final HandshakeCertificates handshakeCertificates = handshakeCertificatesBuilder.build();
            clientBuilder.sslSocketFactory( handshakeCertificates.sslSocketFactory(), handshakeCertificates.trustManager() );
        }
    }
}
