package io.github.mpcoredeveloper.javaportico.model;

/**
 * Proxy (gRPC-to-REST) generation configuration, populated when proxy mode is enabled.
 *
 * @param clientKeyMode 0 = NONE, 1 = FORWARD, 2 = OWN (mirrors the C# ClientKeyMode numbers).
 */
public record ProxyConfigModel(
        boolean enabled,
        String baseUrl,
        String apiKeyHeaderName,
        int cacheTtlSeconds,
        String bypassCacheMetadataKey,
        String clientKeyHeaderName,
        int clientKeyMode,
        boolean auditEnabled) {
}
