package com.enonic.lib.http.client;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.base.Charsets;
import com.google.common.io.ByteSource;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import okio.Buffer;

import com.enonic.xp.testing.ScriptTestSupport;
import com.enonic.xp.trace.Trace;
import com.enonic.xp.trace.TraceManager;

import static org.junit.Assert.*;

public class HttpRequestHandlerTest
    extends ScriptTestSupport
{
    protected MockWebServer server;

    @Override
    public void initialize()
        throws Exception
    {
        super.initialize();
        this.server = new MockWebServer();
        try
        {
            this.server.start();
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
        final TraceManager manager = Mockito.mock( TraceManager.class );
        final Trace trace = Mockito.mock( Trace.class );
        Mockito.when( manager.newTrace( Mockito.any(), Mockito.any() ) ).thenReturn( trace );
        com.enonic.xp.trace.Tracer.setManager( manager );
    }

    @After
    public final void shutdown()
        throws Exception
    {
        HttpClientFactory.clearCache();
        this.server.shutdown();
    }

    private MockResponse addResponse( final byte[] body, String contentType )
        throws Exception
    {
        final MockResponse response = new MockResponse();
        response.setBody( new Buffer().write( body ) );
        response.setHeader( "content-type", contentType );
        this.server.enqueue( response );
        return response;
    }

    private MockResponse addResponse( final String body )
        throws Exception
    {
        final MockResponse response = new MockResponse();
        response.setBody( body );
        response.setHeader( "content-type", "text/plain" );
        this.server.enqueue( response );
        return response;
    }

    private MockResponse addResponseWithDelay( final String body, final long millis )
        throws Exception
    {
        final MockResponse response = new MockResponse();
        response.setBody( body );
        response.setHeader( "content-type", "text/plain" );
        response.throttleBody( 0, millis, TimeUnit.MILLISECONDS );
        this.server.enqueue( response );
        return response;
    }

    private RecordedRequest takeRequest()
        throws Exception
    {
        return this.server.takeRequest();
    }

    @Test
    public void testSimpleGetRequest()
        throws Exception
    {
        addResponse( "GET request" );

        runFunction( "/lib/test/request-test.js", "simpleGetRequest", getServerHost() );

        final RecordedRequest request = takeRequest();
        assertEquals( "GET", request.getMethod() );
        assertEquals( "/my/url", request.getPath() );
        assertEquals( "", request.getBody().readString( Charsets.UTF_8 ) );
    }

    @Test
    public void testSimplePostRequest()
        throws Exception
    {
        server.enqueue( addResponse( "POST request" ) );

        runFunction( "/lib/test/request-test.js", "simplePostRequest", getServerHost() );

        final RecordedRequest request = takeRequest();
        assertEquals( "POST", request.getMethod() );
        assertEquals( "/my/url", request.getPath() );
        assertEquals( "POST body", request.getBody().readString( Charsets.UTF_8 ) );
    }

    @Test
    public void testSimplePatchRequest()
        throws Exception
    {
        server.enqueue( addResponse( "PATCH request" ) );

        runFunction( "/lib/test/request-test.js", "simplePatchRequest", getServerHost() );

        final RecordedRequest request = takeRequest();
        assertEquals( "PATCH", request.getMethod() );
        assertEquals( "/my/url", request.getPath() );
        assertEquals( "PATCH body", request.getBody().readString( Charsets.UTF_8 ) );
    }

    @Test
    public void testSimpleHeadRequest()
        throws Exception
    {
        server.enqueue( addResponse( "GET request" ) );

        runFunction( "/lib/test/request-test.js", "simpleHeadRequest", getServerHost() );

        final RecordedRequest request = takeRequest();
        assertEquals( "HEAD", request.getMethod() );
        assertEquals( "/my/url", request.getPath() );
        assertEquals( "", request.getBody().readString( Charsets.UTF_8 ) );
    }

    @Test
    public void testGetRequestWithParams()
        throws Exception
    {
        addResponse( "GET request" );

        runFunction( "/lib/test/request-test.js", "getRequestWithParams", getServerHost() );

        final RecordedRequest request = takeRequest();
        assertEquals( "GET", request.getMethod() );
        assertEquals( "/my/url?a=123&b=456", request.getPath() );
        assertEquals( "", request.getBody().readString( Charsets.UTF_8 ) );
    }

    @Test
    public void testPostRequestWithParams()
        throws Exception
    {
        addResponse( "POST request" );

        runFunction( "/lib/test/request-test.js", "postRequestWithParams", getServerHost() );

        final RecordedRequest request = takeRequest();
        assertEquals( "POST", request.getMethod() );
        assertEquals( "/my/url?p1=123&p2=something", request.getPath() );
        assertEquals( "a=123&b=456", request.getBody().readString( Charsets.UTF_8 ) );
    }

    @Test
    public void testPostJsonRequest()
        throws Exception
    {
        addResponse( "POST request" );

        runFunction( "/lib/test/request-test.js", "postJsonRequest", getServerHost() );

        final RecordedRequest request = takeRequest();
        assertEquals( "POST", request.getMethod() );
        assertEquals( "/my/url", request.getPath() );
        assertEquals( "application/json; charset=utf-8", request.getHeader( "content-type" ) );
        assertEquals( "{\"a\":123,\"b\":456}", request.getBody().readString( Charsets.UTF_8 ) );
    }

    @Test
    public void testPostImageRequest()
        throws Exception
    {
        final byte[] bytes = new byte[(int) HttpRequestHandler.MAX_IN_MEMORY_BODY_STREAM_BYTES + 1]; // bypass in-memory limit
        addResponse( bytes, "image/png" );

        ThreadLocalRandom.current().nextBytes( bytes );
        runFunction( "/lib/test/request-test.js", "postImageRequest", getServerHost(), ByteSource.wrap( bytes ) );

        final RecordedRequest request = takeRequest();
        assertEquals( "POST", request.getMethod() );
        assertEquals( "/my/url", request.getPath() );
        assertEquals( "image/png", request.getHeader( "content-type" ) );
        assertArrayEquals( bytes, request.getBody().readByteArray() );
    }

    @Test
    public void testGetWithHeadersRequest()
        throws Exception
    {
        addResponse( "GET request" );

        runFunction( "/lib/test/request-test.js", "getWithHeadersRequest", getServerHost() );

        final RecordedRequest request = takeRequest();
        assertEquals( "GET", request.getMethod() );
        assertEquals( "some-value", request.getHeader( "X-Custom-Header" ) );
    }

    @Test(timeout = 20000)
    public void testReadTimeout()
        throws Exception
    {
        addResponseWithDelay( "GET request", 5000 );

        runFunction( "/lib/test/request-test.js", "getWithResponseTimeout", getServerHost() );

        final RecordedRequest request = takeRequest();
        assertEquals( "GET", request.getMethod() );
    }

    @Test(timeout = 20000)
    @Ignore
    public void testConnectTimeout()
        throws Exception
    {
        MockResponse response = new MockResponse();
        response = response.setSocketPolicy( SocketPolicy.NO_RESPONSE );
        server.enqueue( response );

        runFunction( "/lib/test/request-test.js", "getWithConnectTimeout", getServerHost() );

        final RecordedRequest request = takeRequest();
        assertEquals( "GET", request.getMethod() );
    }

    @Test
    public void testRequestWithProxy()
        throws Exception
    {
        final MockWebServer proxy = new MockWebServer();
        try
        {
            proxy.start();

            final MockResponse response = new MockResponse();
            response.setBody( "POST request" );
            response.setHeader( "content-type", "text/plain" );
            proxy.enqueue( response );

            runFunction( "/lib/test/request-test.js", "requestWithProxy", getServerHost(), proxy.getHostName(), proxy.getPort() );

            final RecordedRequest proxyRequest = proxy.takeRequest();
            assertEquals( "POST", proxyRequest.getMethod() );
            assertTrue( proxyRequest.getRequestLine().contains( "http://" + getServerHost() + "/my/url" ) );
        }
        finally
        {
            proxy.shutdown();
        }
    }

    @Test
    public void testRequestWithProxyWithAuth()
        throws Exception
    {
        final MockWebServer proxy = new MockWebServer();
        try
        {
            proxy.start();

            final MockResponse proxyAuthResponse = new MockResponse();
            proxyAuthResponse.setResponseCode( 407 );
            proxyAuthResponse.setHeader( "Proxy-Authenticate", "Basic" );
            proxy.enqueue( proxyAuthResponse );

            final MockResponse response = new MockResponse();
            response.setBody( "POST request authenticated" );
            response.setHeader( "content-type", "text/plain" );
            proxy.enqueue( response );

            runFunction( "/lib/test/request-test.js", "requestWithProxyAuth", getServerHost(), proxy.getHostName(), proxy.getPort() );

            final RecordedRequest proxyRequest = proxy.takeRequest();
            assertEquals( "POST", proxyRequest.getMethod() );
            assertTrue( proxyRequest.getRequestLine().contains( "http://" + getServerHost() + "/my/url" ) );
        }
        finally
        {
            proxy.shutdown();
        }
    }

    public String getServerHost()
    {
        return server.getHostName() + ":" + server.getPort();
    }

    public ByteSource getImageStream()
    {
        return ByteSource.wrap( "image_data".getBytes() );
    }

    @Test
    public void testExample()
        throws Exception
    {
        this.server.enqueue( addResponse( "POST request" ) );
        runScript( "/lib/examples/http-client/request.js" );
    }

    @Test
    public void testExampleMultipart()
        throws Exception
    {
        this.server.enqueue( addResponse( "POST request" ) );
        runScript( "/lib/examples/http-client/multipart.js" );
        final RecordedRequest request = takeRequest();
        assertTrue( request.getHeader( "content-type" ).startsWith( "multipart/mixed;" ) );
    }

    @Test
    public void testBasicAuthentication()
        throws Exception
    {
        final MockResponse authResponse = new MockResponse();
        authResponse.setResponseCode( 401 );
        authResponse.setHeader( "WWW-Authenticate", "Basic realm=\"foo\", charset=\"UTF-8\"" );
        this.server.enqueue( authResponse );

        final MockResponse response = new MockResponse();
        response.setBody( "GET request authenticated" );
        response.setHeader( "content-type", "text/plain" );
        this.server.enqueue( response );

        runScript( "/lib/examples/http-client/basicauth.js" );

        final RecordedRequest request = takeRequest();
        assertEquals( "GET", request.getMethod() );

        final RecordedRequest request2 = takeRequest();
        assertEquals( "Basic dXNlcm5hbWU6c2VjcmV0", request2.getHeader( "Authorization" ) );
    }

    @Test
    public void testSimpleGetRequestWithoutTracing()
        throws Exception
    {
        com.enonic.xp.trace.Tracer.setManager( null );

        addResponse( "GET request" );

        runFunction( "/lib/test/request-test.js", "simpleGetRequest", getServerHost() );

        final RecordedRequest request = takeRequest();
        assertEquals( "GET", request.getMethod() );
        assertEquals( "/my/url", request.getPath() );
        assertEquals( "", request.getBody().readString( Charsets.UTF_8 ) );
    }

    @Test
    public void testCookies()
    {
        final MockResponse response = new MockResponse();
        response.setBody( "GET request" );
        response.setHeader( "content-type", "text/plain" );
        response.addHeader( "Set-Cookie", "a=b; Domain=example.com; Path=/docs; Secure; HttpOnly" );
        this.server.enqueue( response );

        runFunction( "/lib/test/request-test.js", "cookies", getServerHost() );
    }

    @Test
    public void testRequestWithSoapResponse()
    {
        final MockResponse response = new MockResponse();
        response.setHeader( "content-type", "application/soap+xml; charset=utf-8" );
        response.setBody( "<?xml version=\"1.0\" encoding=\"utf-8\"?><body/>" );
        this.server.enqueue( response );
        runFunction( "/lib/test/request-test.js", "requestWithSoapResponse", getServerHost() );
    }
}
