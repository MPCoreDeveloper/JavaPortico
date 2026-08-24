package io.github.mpcoredeveloper.javaportico.runtime;

import java.time.Duration;

/**
 * No-op cache — used when caching is disabled.
 */
public final class NullProxyCache implements IProxyCache {

    public static final NullProxyCache INSTANCE = new NullProxyCache();

    private NullProxyCache() {
    }

    @Override
    public byte[] get(String key) {
        return null;
    }

    @Override
    public void set(String key, byte[] value, Duration ttl) {
        // no-op
    }
}
