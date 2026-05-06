package com.enonic.lib.http.client;

import com.enonic.xp.testing.ScriptTestSupport;
import com.google.common.io.ByteSource;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

public class SecureHttpRequest extends ScriptTestSupport
{
    @TempDir
    public Path tempFolder;

    protected MockWebServer server;

    private HeldCertificate serverRootCertificate;

    private HeldCertificate clientRootCertificate;

    protected HeldCertificate clientCertificate;

    protected HeldCertificate serverCertificate;

    protected ByteSource serverCertificateBytes;

    protected ByteSource clientCertificateBytes;

    protected void setupServer( boolean mTLS )
        throws Exception
    {
        // Create the root certificate for the server
        serverRootCertificate = new HeldCertificate.Builder()
                .commonName("serverCA")
                .certificateAuthority(0)
                .build();

        // Create the root certificate for the client
        clientRootCertificate = new HeldCertificate.Builder()
                .commonName("clientCA")
                .certificateAuthority(0)
                .build();

        // Create a server certificate
        serverCertificate = new HeldCertificate.Builder()
                .commonName("server")
                .addSubjectAlternativeName(InetAddress.getByName( "localhost" ).getCanonicalHostName())
                .signedBy(serverRootCertificate)
                .build();
        HandshakeCertificates serverCertificates = new HandshakeCertificates.Builder()
                .addTrustedCertificate(clientRootCertificate.certificate())
                .heldCertificate(serverCertificate)
                .build();
        serverCertificateBytes = ByteSource.wrap(serverCertificate
                .certificatePem()
                .getBytes( StandardCharsets.ISO_8859_1 ));

        // Create a client certificate
        clientCertificate = new HeldCertificate.Builder()
                .commonName("client")
                .signedBy(clientRootCertificate)
                .build();
        clientCertificateBytes = ByteSource.wrap( new StringBuilder().
                append(clientCertificate.certificatePem()).
                append('\n').
                append(clientCertificate.privateKeyPkcs8Pem()).
                toString().getBytes( StandardCharsets.ISO_8859_1 ) );

        // Setup server
        server = new MockWebServer();
        server.useHttps( serverCertificates.sslSocketFactory(), false );
        server.setProtocolNegotiationEnabled( false );
        if ( mTLS )
        {
            server.requireClientAuth();
        }
        server.start();
    }

    @AfterEach
    public void tearDown()
        throws Exception
    {
        HttpClientFactory.clearCache();
        server.shutdown();
    }

    protected String setupTrustStore( String password )
        throws Exception
    {
        File trustStoreFile = tempFolder.resolve( "tmpTrustStore" ).toFile();

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("server", serverRootCertificate.certificate() );
        trustStore.store(new FileOutputStream(trustStoreFile), password.toCharArray());

        return trustStoreFile.getAbsolutePath();
    }

    protected String setupKeyStore( String password )
        throws Exception
    {
        File keystoreFile = tempFolder.resolve( "tmpKeyStore" ).toFile();

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setKeyEntry(
            "client",
            clientCertificate.keyPair().getPrivate(),
            password.toCharArray(),
            new X509Certificate[]{clientCertificate.certificate()}
        );
        keyStore.store(new FileOutputStream(keystoreFile), password.toCharArray());

        return keystoreFile.getAbsolutePath();
    }
}
