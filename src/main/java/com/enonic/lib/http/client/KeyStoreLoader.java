package com.enonic.lib.http.client;

import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.tls.HeldCertificate;

public class KeyStoreLoader
{
    private KeyStoreLoader()
    {
    }

    private static final Logger LOG = LoggerFactory.getLogger( KeyStoreLoader.class );

    private static final ConcurrentMap<String, HeldCertificate> CACHE = new ConcurrentHashMap<>();

    private static final HeldCertificate NO_CERTIFICATE = new HeldCertificate.Builder().commonName( "stub" ).build();

    public static HeldCertificate get( final String alias )
    {
        final HeldCertificate heldCertificate = CACHE.computeIfAbsent( alias, KeyStoreLoader::compute );
        return heldCertificate == NO_CERTIFICATE ? null : heldCertificate;
    }

    static void clearCache()
    {
        CACHE.clear();
    }

    private static HeldCertificate compute( final String alias )
    {
        try
        {
            final String defaultKeyStore = System.getProperty( "com.enonic.lib.http.client.keyStore", "" );
            final String defaultKeyStoreType = System.getProperty( "com.enonic.lib.http.client.keyStoreType", KeyStore.getDefaultType() );
            final String defaultKeyStoreProvider = System.getProperty( "com.enonic.lib.http.client.keyStoreProvider", "" );
            final char[] defaultKeyStorePassword = System.getProperty( "com.enonic.lib.http.client.keyStorePassword", "" ).toCharArray();

            if ( !defaultKeyStoreType.isEmpty() && !defaultKeyStore.isEmpty() )
            {
                final KeyStore keyStore =
                    getKeyStore( defaultKeyStore, defaultKeyStoreType, defaultKeyStoreProvider, defaultKeyStorePassword );

                final String effectiveAlias;
                if ( alias.isEmpty() )
                {
                    // We only support 1 keypair in the keystore so we
                    // will only pick the first alias in the keystore
                    effectiveAlias = keyStore.aliases().nextElement();
                }
                else
                {
                    effectiveAlias = alias;
                }

                final Certificate certificate = Objects.requireNonNull( keyStore.getCertificate( effectiveAlias ), "No certificate found for alias " + alias );
                final Key key = Objects.requireNonNull( keyStore.getKey( effectiveAlias, defaultKeyStorePassword ), "No key found for alias " + alias );
                final KeyPair keyPair = new KeyPair( certificate.getPublicKey(), (PrivateKey) key );
                return new HeldCertificate( keyPair, (X509Certificate) certificate );
            }
        }
        catch ( Exception e )
        {
            LOG.error( "Failed loading com.enonic.lib.http.client.keyStore", e );
        }

        return NO_CERTIFICATE;
    }

    private static KeyStore getKeyStore( final String keyStore, final String keyStoreType, final String keyStoreProvider,
                                         final char[] keyStorePassword )
        throws Exception
    {
        final KeyStore ks =
            keyStoreProvider.isEmpty() ? KeyStore.getInstance( keyStoreType ) : KeyStore.getInstance( keyStoreType, keyStoreProvider );

        try (FileInputStream fs = "NONE".equals( keyStore ) ? null : new FileInputStream( keyStore ))
        {
            ks.load( fs, keyStorePassword.length != 0 ? keyStorePassword : null );
        }
        return ks;
    }
}
