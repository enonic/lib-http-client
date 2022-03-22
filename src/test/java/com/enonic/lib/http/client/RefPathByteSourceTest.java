package com.enonic.lib.http.client;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.*;

public class RefPathByteSourceTest
{
    private Path tempFile;

    @Before
    public void setUp()
        throws Exception
    {
        tempFile = Files.createTempFile( "xphttp", ".tmp" );
    }

    @After
    public final void shutdown()
        throws Exception
    {
        Files.deleteIfExists( tempFile );
    }

    @Test
    public void testFileByteSource()
        throws Throwable
    {
        RefPathByteSource fileByteSource = new RefPathByteSource( tempFile );
        final byte[] bytes = {0, 1, 2};
        Files.write( tempFile, bytes );

        assertNotNull( fileByteSource.openStream() );
        assertEquals( 3, fileByteSource.size() );
        assertArrayEquals( bytes, fileByteSource.read() );

        assertTrue( Files.exists( tempFile ) );
    }

    @Test
    public void testAsCharSource()
        throws Throwable
    {
        RefPathByteSource fileByteSource = new RefPathByteSource( tempFile );
        Files.writeString( tempFile, "abc", StandardCharsets.UTF_16 );

        assertEquals( "abc", fileByteSource.asCharSource( StandardCharsets.UTF_16 ).read() );
    }

    @Test
    public void testSizeIsKnown()
        throws Throwable
    {
        RefPathByteSource fileByteSource = new RefPathByteSource( tempFile );
        Files.writeString( tempFile, "abc", StandardCharsets.UTF_16 );

        assertTrue( fileByteSource.sizeIfKnown().isPresent() );
        assertEquals( 8L, fileByteSource.sizeIfKnown().get().longValue() );
    }

    @Test
    public void testCleanup()
        throws Exception
    {
        final AtomicReference<RefPathByteSource> ref = new AtomicReference<>( new RefPathByteSource( tempFile ) );
        assertNotNull( ref.get() );

        System.gc();
        Thread.sleep( 5000 );
        assertTrue( "File should exist while it is referenced", Files.exists( tempFile ) );

        ref.set( null );
        System.gc();
        CompletableFuture.completedFuture( tempFile ).thenAcceptAsync( this::busyWaitFileNotExists ).get( 5, TimeUnit.SECONDS );

        assertFalse( "File should be removed when it is no longer referenced", Files.exists( tempFile ) );
    }

    private void busyWaitFileNotExists( Path tempFile )
    {
        while ( true )
        {
            if ( !Files.exists( tempFile ) )
            {
                return;
            }
            else
            {
                try
                {
                    Thread.sleep( 100 );
                }
                catch ( InterruptedException e )
                {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}