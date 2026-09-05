package io.github.mpcoredeveloper.javaportico.runtime;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.Objects;

/**
 * In-memory {@link IProxyCache} backed by Caffeine. Default for single-instance deployments.
 */
public final class MemoryProxyCache implements IProxyCache {

    private static final class Entry {
        final byte[] value;
        final long expiresAtNanos;

        Entry(byte[] value, long expiresAtNanos) {
            this.value = value;
            this.expiresAtNanos = expiresAtNanos;
        }
    }

    private final Cache<String, Entry> cache;

    public MemoryProxyCache() {
        this(10_000);
    }

    public MemoryProxyCache(long maximumSize) {
        this.cache = Caffeine.newBuilder().maximumSize(maximumSize).build();
    }

    @Override
    @SuppressWarnings("java:S1168") // null is the documented cache-miss contract for IProxyCache (generated proxies rely on it).
    public byte[] get(String key) {
        Entry e = cache.getIfPresent(key);
        if (e == null) return null;
        if (System.nanoTime() > e.expiresAtNanos) {
            cache.asMap().remove(key, e);
            return null;
        }
        return e.value;
    }

    @Override
    public void set(String key, byte[] value, Duration ttl) {
        Objects.requireNonNull(value, "value");
        long ttlNanos = ttl == null ? Duration.ofSeconds(60).toNanos() : Math.max(1, ttl.toNanos());
        cache.put(key, new Entry(value, System.nanoTime() + ttlNanos));
    }
}
