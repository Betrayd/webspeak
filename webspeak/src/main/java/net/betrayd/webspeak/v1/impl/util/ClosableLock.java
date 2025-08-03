package net.betrayd.webspeak.v1.impl.util;

import java.util.concurrent.locks.Lock;

/**
 * A simple closable wrapper around a lock that unlocks on <code>close</code>
 */
public class ClosableLock implements AutoCloseable {
    public static ClosableLock lock(Lock baseLock) {
        baseLock.lock();
        return new ClosableLock(baseLock);
    }

    private final Lock baseLock;

    private ClosableLock(Lock baseLock) {
        this.baseLock = baseLock;
    }

    @Override
    public void close() {
        baseLock.unlock();
    }
    
}
