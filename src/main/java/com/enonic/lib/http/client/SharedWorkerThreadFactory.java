package com.enonic.lib.http.client;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

final class SharedWorkerThreadFactory
    implements ThreadFactory
{
    private final String namePrefix;

    private final AtomicInteger nextId = new AtomicInteger();

    SharedWorkerThreadFactory()
    {
        namePrefix = "lib-httpclient-Shared-Worker-";
    }

    @Override
    public Thread newThread( Runnable r )
    {
        final String name = namePrefix + nextId.getAndIncrement();
        final Thread t = new Thread( null, r, name, 0, false );
        t.setDaemon( true );
        return t;
    }
}
