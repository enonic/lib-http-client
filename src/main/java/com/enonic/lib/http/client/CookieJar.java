package com.enonic.lib.http.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

public class CookieJar
    implements okhttp3.CookieJar
{
    private final List<Cookie> cookies;

    CookieJar()
    {
        this.cookies = new ArrayList<>();
    }

    @Override
    public void saveFromResponse( final HttpUrl url, final List<Cookie> cookies )
    {
        this.cookies.addAll( cookies );
    }

    @Override
    public List<Cookie> loadForRequest( final HttpUrl url )
    {
        return Collections.emptyList();
    }

    List<Cookie> getCookies()
    {
        return this.cookies;
    }
}
