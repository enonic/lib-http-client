package com.enonic.lib.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.ref.Cleaner;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Optional;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.common.io.MoreFiles;

/**
 * Wrapper for PathByteSource that deletes the file when there are no references to it (i.e. it's garbage collected and the finalize method is called).
 */
public final class RefPathByteSource
    extends ByteSource
{
    private static final Cleaner CLEANER = Cleaner.create();

    private final ByteSource delegate;

    private static final Set<Path> FILES_TO_DELETE_ON_EXIT = Collections.newSetFromMap( new ConcurrentHashMap<>() );

    static
    {
        Runtime.getRuntime().addShutdownHook( new Thread( RefPathByteSource::deleteAllFiles ) );
    }

    private static void deleteAllFiles()
    {
        for ( Path file : FILES_TO_DELETE_ON_EXIT )
        {
            try
            {
                Files.delete( file );
            }
            catch ( Exception ignore )
            {
            }
        }
    }

    private static class CleanupAction
        implements Runnable
    {
        Path file;

        CleanupAction( final Path file )
        {
            this.file = file;
        }

        public void run()
        {
            try
            {
                FILES_TO_DELETE_ON_EXIT.remove( file );
                Files.delete( file );
            }
            catch ( IOException e )
            {
                throw new UncheckedIOException( e );
            }
        }
    }

    public RefPathByteSource( final Path file )
    {

        this.delegate = MoreFiles.asByteSource( file );
        FILES_TO_DELETE_ON_EXIT.add( file );
        CLEANER.register( this, new CleanupAction( file ) );
    }

    @Override
    public InputStream openStream()
        throws IOException
    {
        return delegate.openStream();
    }

    @Override
    public byte[] read()
        throws IOException
    {
        return delegate.read();
    }

    @Override
    public long size()
        throws IOException
    {
        return delegate.size();
    }

    @Override
    public Optional<Long> sizeIfKnown()
    {
        return delegate.sizeIfKnown();
    }

    @Override
    public CharSource asCharSource( final Charset charset )
    {
        return delegate.asCharSource( charset );
    }
}
