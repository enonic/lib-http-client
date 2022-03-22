package com.enonic.lib.http.client;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.Supplier;

import com.google.common.io.ByteSource;

import com.enonic.xp.script.serializer.MapGenerator;
import com.enonic.xp.script.serializer.MapSerializable;

public final class ResponseMapper
    implements MapSerializable
{

    private final List<HttpCookie> cookies;

    private final long cookiesTime;

    private final int status;

    private final HttpHeaders headers;

    private final ByteSource bodySource;

    private final String bodyString;

    public ResponseMapper( final HttpResponse<Supplier<ByteSource>> response )
        throws IOException
    {
        this.status = response.statusCode();
        this.headers = response.headers();
        this.bodySource = response.body().get();

        final Charset charset = Utils.guessTextCharset( this.headers );
        this.bodyString = charset != null ? this.bodySource.asCharSource( charset ).read() : null;

        this.cookiesTime = System.currentTimeMillis();
        this.cookies = Utils.getCookies( response );
    }

    @Override
    public void serialize( final MapGenerator gen )
    {
        gen.value( "status", status );
        gen.value( "message", Utils.httpCodeToMessage( status ) );

        gen.value( "body", bodyString );
        gen.value( "bodyStream", bodySource );
        gen.value( "contentType", Utils.getContentType( headers ) );

        serializeHeaders( "headers", gen );
        serializeCookies( "cookies", gen );
    }

    private void serializeHeaders( final String name, final MapGenerator gen )
    {
        gen.map( name );
        this.headers.map().forEach( ( key, value ) -> gen.value( key, value.size() == 1 ? value.get( 0 ) : value ) );
        gen.end();
    }

    private void serializeCookies( final String name, final MapGenerator gen )
    {
        gen.array( name );
        for ( final HttpCookie cookie : this.cookies )
        {
            gen.map();
            gen.value( "name", cookie.getName() );
            gen.value( "value", cookie.getValue() );
            gen.value( "path", cookie.getPath() );
            gen.value( "domain", cookie.getDomain() );
            gen.value( "expires", Utils.calculateCookieExpires( cookiesTime, cookie.getMaxAge() ) );
            gen.value( "secure", cookie.getSecure() );
            gen.value( "httpOnly", cookie.isHttpOnly() );
            gen.end();
        }
        gen.end();
    }
}
