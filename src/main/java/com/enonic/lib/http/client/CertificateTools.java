package com.enonic.lib.http.client;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;

final class CertificateTools
{
    private static final String BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----";

    private static final String END_PRIVATE_KEY = "-----END PRIVATE KEY-----";

    private static final String BEGIN_CERTIFICATE = "-----BEGIN CERTIFICATE-----";

    private static final String END_CERTIFICATE = "-----END CERTIFICATE-----";

    private CertificateTools()
    {
    }

    public static List<Certificate> loadCertificates( final byte[] certificates )
    {
        try
        {
            final CertificateFactory certificateFactory = CertificateFactory.getInstance( "X.509" );
            return List.copyOf( certificateFactory.generateCertificates( new ByteArrayInputStream( certificates ) ) );
        }
        catch ( GeneralSecurityException e )
        {
            throw new RuntimeException( e );
        }
    }

    public static CertWithKey loadClientCertificate( final byte[] clientCertificate )
    {
        try
        {
            final String s = new String( clientCertificate, StandardCharsets.ISO_8859_1 );
            String certPem = s.substring( s.indexOf( BEGIN_CERTIFICATE ) + BEGIN_CERTIFICATE.length(), s.indexOf( END_CERTIFICATE ) )
                .replaceAll( "\n", "" );
            String pkcs8Base64 = s.substring( s.indexOf( BEGIN_PRIVATE_KEY ) + BEGIN_PRIVATE_KEY.length(), s.indexOf( END_PRIVATE_KEY ) )
                .replaceAll( "\n", "" );
            final Certificate cert = CertificateFactory.getInstance( "X.509" )
                .generateCertificate(
                    new ByteArrayInputStream( Base64.getDecoder().decode( certPem.getBytes( StandardCharsets.ISO_8859_1 ) ) ) );

            final Key key = KeyFactory.getInstance( cert.getPublicKey().getAlgorithm() )
                .generatePrivate(
                    new PKCS8EncodedKeySpec( Base64.getDecoder().decode( pkcs8Base64.getBytes( StandardCharsets.ISO_8859_1 ) ) ) );
            return new CertWithKey( cert, key );
        }
        catch ( GeneralSecurityException e )
        {
            throw new RuntimeException( e );
        }
    }
}
