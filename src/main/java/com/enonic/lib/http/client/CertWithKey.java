package com.enonic.lib.http.client;

import java.io.Serializable;
import java.security.Key;
import java.security.cert.Certificate;

public final class CertWithKey implements Serializable
{
    private static final long serialVersionUID = 0;

    private final Key key;
    private final Certificate certificate;

    public CertWithKey( final Certificate certificate, final Key key )
    {
        this.key = key;
        this.certificate = certificate;
    }

    public Key getKey()
    {
        return key;
    }

    public Certificate getCertificate()
    {
        return certificate;
    }
}
