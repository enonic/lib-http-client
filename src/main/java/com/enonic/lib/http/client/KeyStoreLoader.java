package com.enonic.lib.http.client;

import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicBoolean;

public class KeyStoreLoader
{
    private final static Logger log = LoggerFactory.getLogger(KeyStoreLoader.class);

    private static HeldCertificate certAndKey;

    private final static AtomicBoolean keyStoreLoaded = new AtomicBoolean(false);

    private static void load() throws Exception
    {
        String keyStoreFile = System.getProperty("javax.net.ssl.keyStore");
        String keyStorePassword = System.getProperty("javax.net.ssl.keyStorePassword");

        if ( keyStoreFile == null || keyStorePassword == null )
        {
            return;
        }

        String keyStoreType = System.getProperty("javax.net.ssl.keyStoreType");
        if ( keyStoreType == null )
        {
            keyStoreType = KeyStore.getDefaultType();
        }

        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(new FileInputStream(keyStoreFile), keyStorePassword.toCharArray());
        Enumeration<String> aliases = keyStore.aliases();

        // We only support 1 keypair in the keystore so we
        // will only pick the first alias in the keystore
        if( aliases.hasMoreElements() )
        {
            String alias = aliases.nextElement();
            Certificate certificate = keyStore.getCertificate(alias);
            Key key = keyStore.getKey(alias, keyStorePassword.toCharArray());
            KeyPair keyPair = new KeyPair(certificate.getPublicKey(), (PrivateKey) key);
            certAndKey = new HeldCertificate(keyPair, (X509Certificate) certificate);
        }
    }

    public static void addKeyStore(HandshakeCertificates.Builder handshakeCertificatesBuilder) {
        if( keyStoreLoaded.compareAndSet(false, true) )
        {
            try
            {
                load();
            }
            catch (Exception e) {
                log.error("Failed loading javax.net.ssl.keyStore", e);
            }
        }

        if ( certAndKey != null )
        {
            handshakeCertificatesBuilder.heldCertificate(certAndKey);
        }
    }

    // For testing purposes
    static void clear()
    {
        if( keyStoreLoaded.compareAndSet(true, false) )
        {
            certAndKey = null;
        }
    }
}
