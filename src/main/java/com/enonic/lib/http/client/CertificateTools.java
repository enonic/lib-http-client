package com.enonic.lib.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.stream.Collectors;

import com.google.common.io.ByteSource;

import okhttp3.OkHttpClient;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;

final class CertificateTools
{
    private final Collection<X509Certificate> certificates;

    private final HeldCertificate clientCertificate;

    private final String clientCertificateAlias;

    public CertificateTools( final ByteSource certificates, final ByteSource clientCertificate, final String clientCertificateAlias )
    {
        if ( certificates != null )
        {
            try (InputStream certificatesStream = certificates.openStream())
            {
                final CertificateFactory certificateFactory = CertificateFactory.getInstance( "X.509" );
                this.certificates = certificateFactory.generateCertificates( certificatesStream )
                    .stream()
                    .map( certificate -> (X509Certificate) certificate )
                    .collect( Collectors.toList() );
            }
            catch ( GeneralSecurityException e )
            {
                throw new RuntimeException( e );
            }
            catch ( IOException e )
            {
                throw new UncheckedIOException( e );
            }
        }
        else
        {
            this.certificates = null;
        }

        try
        {
            this.clientCertificate = clientCertificate == null
                ? null
                : HeldCertificate.decode( new String( clientCertificate.read(), StandardCharsets.ISO_8859_1 ) );
        }
        catch ( IOException e )
        {
            throw new UncheckedIOException( e );
        }
        this.clientCertificateAlias = clientCertificateAlias;
    }

    public void setupHandshakeCertificates( final OkHttpClient.Builder clientBuilder )
    {
        final HandshakeCertificates.Builder handshakeCertificatesBuilder = new HandshakeCertificates.Builder();

        if ( certificates != null )
        {
            // Add custom CA certificates
            certificates.forEach( handshakeCertificatesBuilder::addTrustedCertificate );
        }
        else
        {
            // Add default trusted CA certificates
            handshakeCertificatesBuilder.addPlatformTrustedCertificates();
        }

        if ( clientCertificate != null )
        {
            // Add custom client key and certificate
            handshakeCertificatesBuilder.heldCertificate( clientCertificate );
        }
        else if ( clientCertificateAlias != null )
        {
            // Use client key and certificate defined in keystore if available
            final HeldCertificate certificate = KeyStoreLoader.get( clientCertificateAlias );
            if ( certificate != null )
            {
                handshakeCertificatesBuilder.heldCertificate( certificate );
            }
        }

        // Set certificates
        final HandshakeCertificates handshakeCertificates = handshakeCertificatesBuilder.build();
        clientBuilder.sslSocketFactory( handshakeCertificates.sslSocketFactory(), handshakeCertificates.trustManager() );
    }
}
