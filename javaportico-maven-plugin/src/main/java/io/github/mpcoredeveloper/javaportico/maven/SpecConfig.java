package io.github.mpcoredeveloper.javaportico.maven;

import io.github.mpcoredeveloper.javaportico.annotations.ClientKeyMode;
import io.github.mpcoredeveloper.javaportico.annotations.JavaPorticoOptions;

/**
 * One OpenAPI spec declared in the POM {@code <specs><spec>...} configuration.
 */
public class SpecConfig {

    private String file;
    private String serviceName;
    private String namespace;
    private boolean emitProtoFile = true;
    private boolean emitClient = true;
    private boolean emitServer = true;
    private boolean emitDependencyInjection = true;
    private boolean respectStreamingHints = true;
    private int largePayloadStreamingThresholdBytes = 1_000_000;
    private boolean generateAuthMetadataHelpers = true;
    private boolean generateAuthInterceptors = true;
    private boolean detectPagination = true;
    private String paginationPageParameter = "page";
    private String paginationLimitParameter = "limit";
    private String paginationCursorParameter = "cursor";
    private String paginationNextPageTokenParameter = "next_page_token";
    private boolean emitGoogleRpcStatusWrapper = true;
    private String serviceNameSuffix = "Service";
    private boolean enableProxyGeneration;
    private String proxyBaseUrl;
    private String proxyApiKeyHeaderName = "X-Api-Key";
    private int proxyCacheTtlSeconds = 60;
    private String proxyBypassCacheMetadataKey = "x-portico-bypass-cache";
    private String proxyClientKeyHeaderName = "x-portico-key";
    private String proxyClientKeyMode = "NONE";
    private boolean proxyAuditEnabled;

    /** Converts this config into {@link JavaPorticoOptions}. */
    public JavaPorticoOptions toOptions() {
        ClientKeyMode mode;
        try {
            mode = ClientKeyMode.valueOf(proxyClientKeyMode == null ? "NONE" : proxyClientKeyMode.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            mode = ClientKeyMode.NONE;
        }
        return JavaPorticoOptions.defaults()
                .setEmitProtoFile(emitProtoFile)
                .setEmitClient(emitClient)
                .setEmitServer(emitServer)
                .setEmitDependencyInjection(emitDependencyInjection)
                .setRespectStreamingHints(respectStreamingHints)
                .setLargePayloadStreamingThresholdBytes(largePayloadStreamingThresholdBytes)
                .setGenerateAuthMetadataHelpers(generateAuthMetadataHelpers)
                .setGenerateAuthInterceptors(generateAuthInterceptors)
                .setDetectPagination(detectPagination)
                .setPaginationPageParameter(paginationPageParameter)
                .setPaginationLimitParameter(paginationLimitParameter)
                .setPaginationCursorParameter(paginationCursorParameter)
                .setPaginationNextPageTokenParameter(paginationNextPageTokenParameter)
                .setEmitGoogleRpcStatusWrapper(emitGoogleRpcStatusWrapper)
                .setServiceNameSuffix(serviceNameSuffix)
                .setEnableProxyGeneration(enableProxyGeneration)
                .setProxyBaseUrl(proxyBaseUrl)
                .setProxyApiKeyHeaderName(proxyApiKeyHeaderName)
                .setProxyCacheTtlSeconds(proxyCacheTtlSeconds)
                .setProxyBypassCacheMetadataKey(proxyBypassCacheMetadataKey)
                .setProxyClientKeyHeaderName(proxyClientKeyHeaderName)
                .setProxyClientKeyMode(mode)
                .setProxyAuditEnabled(proxyAuditEnabled);
    }

    public String getFile() { return file; }
    public SpecConfig setFile(String file) { this.file = file; return this; }

    public String getServiceName() { return serviceName; }
    public SpecConfig setServiceName(String serviceName) { this.serviceName = serviceName; return this; }

    public String getNamespace() { return namespace; }
    public SpecConfig setNamespace(String namespace) { this.namespace = namespace; return this; }

    public boolean isEmitProtoFile() { return emitProtoFile; }
    public SpecConfig setEmitProtoFile(boolean emitProtoFile) { this.emitProtoFile = emitProtoFile; return this; }

    public boolean isEmitClient() { return emitClient; }
    public SpecConfig setEmitClient(boolean emitClient) { this.emitClient = emitClient; return this; }

    public boolean isEmitServer() { return emitServer; }
    public SpecConfig setEmitServer(boolean emitServer) { this.emitServer = emitServer; return this; }

    public boolean isEmitDependencyInjection() { return emitDependencyInjection; }
    public SpecConfig setEmitDependencyInjection(boolean emitDependencyInjection) { this.emitDependencyInjection = emitDependencyInjection; return this; }

    public boolean isRespectStreamingHints() { return respectStreamingHints; }
    public SpecConfig setRespectStreamingHints(boolean respectStreamingHints) { this.respectStreamingHints = respectStreamingHints; return this; }

    public int getLargePayloadStreamingThresholdBytes() { return largePayloadStreamingThresholdBytes; }
    public SpecConfig setLargePayloadStreamingThresholdBytes(int largePayloadStreamingThresholdBytes) { this.largePayloadStreamingThresholdBytes = largePayloadStreamingThresholdBytes; return this; }

    public boolean isGenerateAuthMetadataHelpers() { return generateAuthMetadataHelpers; }
    public SpecConfig setGenerateAuthMetadataHelpers(boolean generateAuthMetadataHelpers) { this.generateAuthMetadataHelpers = generateAuthMetadataHelpers; return this; }

    public boolean isGenerateAuthInterceptors() { return generateAuthInterceptors; }
    public SpecConfig setGenerateAuthInterceptors(boolean generateAuthInterceptors) { this.generateAuthInterceptors = generateAuthInterceptors; return this; }

    public boolean isDetectPagination() { return detectPagination; }
    public SpecConfig setDetectPagination(boolean detectPagination) { this.detectPagination = detectPagination; return this; }

    public String getPaginationPageParameter() { return paginationPageParameter; }
    public SpecConfig setPaginationPageParameter(String paginationPageParameter) { this.paginationPageParameter = paginationPageParameter; return this; }

    public String getPaginationLimitParameter() { return paginationLimitParameter; }
    public SpecConfig setPaginationLimitParameter(String paginationLimitParameter) { this.paginationLimitParameter = paginationLimitParameter; return this; }

    public String getPaginationCursorParameter() { return paginationCursorParameter; }
    public SpecConfig setPaginationCursorParameter(String paginationCursorParameter) { this.paginationCursorParameter = paginationCursorParameter; return this; }

    public String getPaginationNextPageTokenParameter() { return paginationNextPageTokenParameter; }
    public SpecConfig setPaginationNextPageTokenParameter(String paginationNextPageTokenParameter) { this.paginationNextPageTokenParameter = paginationNextPageTokenParameter; return this; }

    public boolean isEmitGoogleRpcStatusWrapper() { return emitGoogleRpcStatusWrapper; }
    public SpecConfig setEmitGoogleRpcStatusWrapper(boolean emitGoogleRpcStatusWrapper) { this.emitGoogleRpcStatusWrapper = emitGoogleRpcStatusWrapper; return this; }

    public String getServiceNameSuffix() { return serviceNameSuffix; }
    public SpecConfig setServiceNameSuffix(String serviceNameSuffix) { this.serviceNameSuffix = serviceNameSuffix; return this; }

    public boolean isEnableProxyGeneration() { return enableProxyGeneration; }
    public SpecConfig setEnableProxyGeneration(boolean enableProxyGeneration) { this.enableProxyGeneration = enableProxyGeneration; return this; }

    public String getProxyBaseUrl() { return proxyBaseUrl; }
    public SpecConfig setProxyBaseUrl(String proxyBaseUrl) { this.proxyBaseUrl = proxyBaseUrl; return this; }

    public String getProxyApiKeyHeaderName() { return proxyApiKeyHeaderName; }
    public SpecConfig setProxyApiKeyHeaderName(String proxyApiKeyHeaderName) { this.proxyApiKeyHeaderName = proxyApiKeyHeaderName; return this; }

    public int getProxyCacheTtlSeconds() { return proxyCacheTtlSeconds; }
    public SpecConfig setProxyCacheTtlSeconds(int proxyCacheTtlSeconds) { this.proxyCacheTtlSeconds = proxyCacheTtlSeconds; return this; }

    public String getProxyBypassCacheMetadataKey() { return proxyBypassCacheMetadataKey; }
    public SpecConfig setProxyBypassCacheMetadataKey(String proxyBypassCacheMetadataKey) { this.proxyBypassCacheMetadataKey = proxyBypassCacheMetadataKey; return this; }

    public String getProxyClientKeyHeaderName() { return proxyClientKeyHeaderName; }
    public SpecConfig setProxyClientKeyHeaderName(String proxyClientKeyHeaderName) { this.proxyClientKeyHeaderName = proxyClientKeyHeaderName; return this; }

    public String getProxyClientKeyMode() { return proxyClientKeyMode; }
    public SpecConfig setProxyClientKeyMode(String proxyClientKeyMode) { this.proxyClientKeyMode = proxyClientKeyMode; return this; }

    public boolean isProxyAuditEnabled() { return proxyAuditEnabled; }
    public SpecConfig setProxyAuditEnabled(boolean proxyAuditEnabled) { this.proxyAuditEnabled = proxyAuditEnabled; return this; }
}
