package io.github.mpcoredeveloper.javaportico.runtime;

import java.time.Duration;

/**
 * Cache used by the generated proxy. Keys are stable strings (service+rpc+request-hash);
 * values are the serialized response payloads. Mirrors SharpPortico's {@code IProxyCache}.
 */
public interface IProxyCache {

    byte[] get(String key);

    void set(String key, byte[] value, Duration ttl);
}
