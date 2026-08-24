package io.github.mpcoredeveloper.javaportico.runtime;

import io.github.mpcoredeveloper.javaportico.annotations.ClientKeyMode;

import java.time.Duration;

/**
 * Configuration for the generated proxy call pipeline. Mirrors SharpPortico's {@code ProxyOptions}.
 */
public class ProxyOptions {

    private String baseUrl = "";
    private String apiKeyHeaderName = "X-Api-Key";
    private String clientKeyHeaderName = "x-portico-key";
    private String bypassCacheMetadataKey = "x-portico-bypass-cache";
    private ClientKeyMode clientKeyMode = ClientKeyMode.NONE;
    private Duration cacheTtl = Duration.ofSeconds(60);
    private boolean cacheReadsOnly = true;

    public String getBaseUrl() {
        return baseUrl;
    }

    public ProxyOptions setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl == null ? "" : baseUrl;
        return this;
    }

    public String getApiKeyHeaderName() {
        return apiKeyHeaderName;
    }

    public ProxyOptions setApiKeyHeaderName(String apiKeyHeaderName) {
        this.apiKeyHeaderName = apiKeyHeaderName == null ? "X-Api-Key" : apiKeyHeaderName;
        return this;
    }

    public String getClientKeyHeaderName() {
        return clientKeyHeaderName;
    }

    public ProxyOptions setClientKeyHeaderName(String clientKeyHeaderName) {
        this.clientKeyHeaderName = clientKeyHeaderName == null ? "x-portico-key" : clientKeyHeaderName;
        return this;
    }

    public String getBypassCacheMetadataKey() {
        return bypassCacheMetadataKey;
    }

    public ProxyOptions setBypassCacheMetadataKey(String bypassCacheMetadataKey) {
        this.bypassCacheMetadataKey = bypassCacheMetadataKey == null ? "x-portico-bypass-cache" : bypassCacheMetadataKey;
        return this;
    }

    public ClientKeyMode getClientKeyMode() {
        return clientKeyMode;
    }

    public ProxyOptions setClientKeyMode(ClientKeyMode clientKeyMode) {
        this.clientKeyMode = clientKeyMode == null ? ClientKeyMode.NONE : clientKeyMode;
        return this;
    }

    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public ProxyOptions setCacheTtl(Duration cacheTtl) {
        this.cacheTtl = cacheTtl == null ? Duration.ofSeconds(60) : cacheTtl;
        return this;
    }

    public boolean isCacheReadsOnly() {
        return cacheReadsOnly;
    }

    public ProxyOptions setCacheReadsOnly(boolean cacheReadsOnly) {
        this.cacheReadsOnly = cacheReadsOnly;
        return this;
    }
}
