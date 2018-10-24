package com.enonic.lib.http.client;

import org.junit.Test;

import okhttp3.Cookie;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import com.enonic.xp.testing.JsonAssert;

public class ResponseMapperTest
{
    @Test
    public void serialize()
        throws Exception
    {
        final Request.Builder request = new Request.Builder();
        request.url( "http://host/some/path" );
        request.get();

        final Response.Builder response = new Response.Builder();
        final ResponseBody body = ResponseBody.create( MediaType.parse( "application/json" ), "{\"ok\": true}" );
        response.body( body );
        response.code( 200 );
        response.message( "Ok" );
        response.protocol( Protocol.HTTP_1_1 );
        response.request( request.build() );
        response.header( "Content-Type", "application/json" );
        ResponseMapper mapper = new ResponseMapper( response.build(), new CookieJar() );

        JsonAssert.assertJson( getClass(), "response", mapper );
    }

    @Test
    public void serializeNoContentType()
        throws Exception
    {
        final Request.Builder request = new Request.Builder();
        request.url( "http://host/some/path" );
        request.get();

        final Response.Builder response = new Response.Builder();
        final ResponseBody body = ResponseBody.create( MediaType.parse( "application/json" ), "{\"ok\": true}" );
        response.body( body );
        response.code( 200 );
        response.message( "Ok" );
        response.protocol( Protocol.HTTP_1_1 );
        response.request( request.build() );
        ResponseMapper mapper = new ResponseMapper( response.build(), new CookieJar() );

        JsonAssert.assertJson( getClass(), "response-no-type", mapper );
    }

    @Test
    public void serializeSoapContentType()
        throws Exception
    {
        final Request.Builder request = new Request.Builder();
        request.url( "http://host/some/path" );
        request.get();

        final Response.Builder response = new Response.Builder();
        final ResponseBody body = ResponseBody.create( MediaType.parse( "application/soap+xml; charset=utf-8" ),
                                                       "<?xml version=\"1.0\" encoding=\"utf-8\"?><body/>" );
        response.body( body );
        response.code( 200 );
        response.message( "Ok" );
        response.protocol( Protocol.HTTP_1_1 );
        response.request( request.build() );
        response.header( "Content-Type", "application/soap+xml; charset=utf-8" );
        ResponseMapper mapper = new ResponseMapper( response.build(), new CookieJar() );

        JsonAssert.assertJson( getClass(), "response-soap", mapper );
    }

    @Test
    public void serializeXmlContentType()
        throws Exception
    {
        final Request.Builder request = new Request.Builder();
        request.url( "http://host/some/path" );
        request.get();

        final Response.Builder response = new Response.Builder();
        final ResponseBody body = ResponseBody.create( MediaType.parse( "application/mathml+xml; charset=utf-8" ),
                                                       "<?xml version=\"1.0\" encoding=\"utf-8\"?><body/>" );
        response.body( body );
        response.code( 200 );
        response.message( "Ok" );
        response.protocol( Protocol.HTTP_1_1 );
        response.request( request.build() );
        response.header( "Content-Type", "application/mathml+xml; charset=utf-8" );
        ResponseMapper mapper = new ResponseMapper( response.build(), new CookieJar() );

        JsonAssert.assertJson( getClass(), "response-xml", mapper );
    }

    @Test
    public void serializeCookies()
        throws Exception
    {
        final Request.Builder request = new Request.Builder();
        request.url( "http://host/some/path" );
        request.get();

        final Response.Builder response = new Response.Builder();
        final ResponseBody body = ResponseBody.create( MediaType.parse( "application/json" ), "{\"ok\": true}" );
        response.body( body );
        response.code( 200 );
        response.message( "Ok" );
        response.protocol( Protocol.HTTP_1_1 );
        response.request( request.build() );
        response.header( "Content-Type", "application/json" );
        final CookieJar cookieJar = new CookieJar();
        final Cookie.Builder c1 = new Cookie.Builder();
        c1.name( "name" );
        c1.value( "value" );
        c1.domain( "mydomain" );
        c1.path( "/some/path" );
        c1.httpOnly();
        c1.secure();
        c1.expiresAt( 1540378491L );
        cookieJar.getCookies().add( c1.build() );
        final Cookie.Builder c2 = new Cookie.Builder();
        c2.name( "name2" );
        c2.value( "value2" );
        c2.domain( "mydomain" );
        c2.path( "/some/path2" );
        c2.expiresAt( 1506841200L );
        cookieJar.getCookies().add( c2.build() );

        ResponseMapper mapper = new ResponseMapper( response.build(), cookieJar );

        JsonAssert.assertJson( getClass(), "response-cookies", mapper );
    }
}