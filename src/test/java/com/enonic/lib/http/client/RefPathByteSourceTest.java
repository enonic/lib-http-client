package com.enonic.lib.http.client;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RefPathByteSourceTest
{
    private Path tempFile;

    @BeforeEach
    public void setUp()
        throws Exception
    {
        tempFile = Files.createTempFile( "xphttp", ".tmp" );
    }

    @AfterEach
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
        assertTrue( Files.exists( tempFile ), "File should exist while it is referenced" );

        ref.set( null );
        System.gc();
        CompletableFuture.completedFuture( tempFile ).thenAcceptAsync( this::busyWaitFileNotExists ).get( 5, TimeUnit.SECONDS );

        assertFalse( Files.exists( tempFile ), "File should be removed when it is no longer referenced" );
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