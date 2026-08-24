package io.github.mpcoredeveloper.javaportico.annotations;

/**
 * Fine-grained generation options for the JavaPortico generator.
 * Mirrors SharpPortico's {@code SharpPorticoOptions}.
 */
public class JavaPorticoOptions {
    private boolean emitProtoFile = true;
    private boolean emitClient = true;
    private boolean emitServer = true;
    private boolean emitDependencyInjection = true;
    private boolean respectStreamingHints = true;
    private int largePayloadStreamingThresholdBytes = 1_000_000;
    private boolean mapEnumsToProtobufEnums = true;
    private boolean emitDiagnosticsForUnmappableConstructs = true;
    private boolean generateAuthMetadataHelpers = true;
    private boolean generateAuthInterceptors = true;
    private boolean detectPagination = true;
    private String paginationPageParameter = "page";
    private String paginationLimitParameter = "limit";
    private String paginationCursorParameter = "cursor";
    private String paginationNextPageTokenParameter = "next_page_token";
    private boolean emitGoogleRpcStatusWrapper = true;
    private GrpcStatusCodeMapping default4xxStatus = GrpcStatusCodeMapping.INVALID_ARGUMENT;
    private GrpcStatusCodeMapping default5xxStatus = GrpcStatusCodeMapping.INTERNAL;
    private NamingConvention messageNaming = NamingConvention.PASCAL_CASE;
    private NamingConvention methodNaming = NamingConvention.PASCAL_CASE;
    private String serviceNameSuffix = "Service";

    // Proxy options
    private boolean enableProxyGeneration;
    private String proxyBaseUrl;
    private String proxyApiKeyHeaderName = "X-Api-Key";
    private int proxyCacheTtlSeconds = 60;
    private String proxyBypassCacheMetadataKey = "x-portico-bypass-cache";
    private String proxyClientKeyHeaderName = "x-portico-key";
    private ClientKeyMode proxyClientKeyMode = ClientKeyMode.NONE;
    private boolean proxyAuditEnabled;

    public static JavaPorticoOptions defaults() {
        return new JavaPorticoOptions();
    }

    public boolean isEmitProtoFile() { return emitProtoFile; }
    public JavaPorticoOptions setEmitProtoFile(boolean emitProtoFile) { this.emitProtoFile = emitProtoFile; return this; }

    public boolean isEmitClient() { return emitClient; }
    public JavaPorticoOptions setEmitClient(boolean emitClient) { this.emitClient = emitClient; return this; }

    public boolean isEmitServer() { return emitServer; }
    public JavaPorticoOptions setEmitServer(boolean emitServer) { this.emitServer = emitServer; return this; }

    public boolean isEmitDependencyInjection() { return emitDependencyInjection; }
    public JavaPorticoOptions setEmitDependencyInjection(boolean emitDependencyInjection) { this.emitDependencyInjection = emitDependencyInjection; return this; }

    public boolean isRespectStreamingHints() { return respectStreamingHints; }
    public JavaPorticoOptions setRespectStreamingHints(boolean respectStreamingHints) { this.respectStreamingHints = respectStreamingHints; return this; }

    public int getLargePayloadStreamingThresholdBytes() { return largePayloadStreamingThresholdBytes; }
    public JavaPorticoOptions setLargePayloadStreamingThresholdBytes(int largePayloadStreamingThresholdBytes) { this.largePayloadStreamingThresholdBytes = largePayloadStreamingThresholdBytes; return this; }

    public boolean isMapEnumsToProtobufEnums() { return mapEnumsToProtobufEnums; }
    public JavaPorticoOptions setMapEnumsToProtobufEnums(boolean mapEnumsToProtobufEnums) { this.mapEnumsToProtobufEnums = mapEnumsToProtobufEnums; return this; }

    public boolean isEmitDiagnosticsForUnmappableConstructs() { return emitDiagnosticsForUnmappableConstructs; }
    public JavaPorticoOptions setEmitDiagnosticsForUnmappableConstructs(boolean emitDiagnosticsForUnmappableConstructs) { this.emitDiagnosticsForUnmappableConstructs = emitDiagnosticsForUnmappableConstructs; return this; }

    public boolean isGenerateAuthMetadataHelpers() { return generateAuthMetadataHelpers; }
    public JavaPorticoOptions setGenerateAuthMetadataHelpers(boolean generateAuthMetadataHelpers) { this.generateAuthMetadataHelpers = generateAuthMetadataHelpers; return this; }

    public boolean isGenerateAuthInterceptors() { return generateAuthInterceptors; }
    public JavaPorticoOptions setGenerateAuthInterceptors(boolean generateAuthInterceptors) { this.generateAuthInterceptors = generateAuthInterceptors; return this; }

    public boolean isDetectPagination() { return detectPagination; }
    public JavaPorticoOptions setDetectPagination(boolean detectPagination) { this.detectPagination = detectPagination; return this; }

    public String getPaginationPageParameter() { return paginationPageParameter; }
    public JavaPorticoOptions setPaginationPageParameter(String paginationPageParameter) { this.paginationPageParameter = paginationPageParameter; return this; }

    public String getPaginationLimitParameter() { return paginationLimitParameter; }
    public JavaPorticoOptions setPaginationLimitParameter(String paginationLimitParameter) { this.paginationLimitParameter = paginationLimitParameter; return this; }

    public String getPaginationCursorParameter() { return paginationCursorParameter; }
    public JavaPorticoOptions setPaginationCursorParameter(String paginationCursorParameter) { this.paginationCursorParameter = paginationCursorParameter; return this; }

    public String getPaginationNextPageTokenParameter() { return paginationNextPageTokenParameter; }
    public JavaPorticoOptions setPaginationNextPageTokenParameter(String paginationNextPageTokenParameter) { this.paginationNextPageTokenParameter = paginationNextPageTokenParameter; return this; }

    public boolean isEmitGoogleRpcStatusWrapper() { return emitGoogleRpcStatusWrapper; }
    public JavaPorticoOptions setEmitGoogleRpcStatusWrapper(boolean emitGoogleRpcStatusWrapper) { this.emitGoogleRpcStatusWrapper = emitGoogleRpcStatusWrapper; return this; }

    public GrpcStatusCodeMapping getDefault4xxStatus() { return default4xxStatus; }
    public JavaPorticoOptions setDefault4xxStatus(GrpcStatusCodeMapping default4xxStatus) { this.default4xxStatus = default4xxStatus; return this; }

    public GrpcStatusCodeMapping getDefault5xxStatus() { return default5xxStatus; }
    public JavaPorticoOptions setDefault5xxStatus(GrpcStatusCodeMapping default5xxStatus) { this.default5xxStatus = default5xxStatus; return this; }

    public NamingConvention getMessageNaming() { return messageNaming; }
    public JavaPorticoOptions setMessageNaming(NamingConvention messageNaming) { this.messageNaming = messageNaming; return this; }

    public NamingConvention getMethodNaming() { return methodNaming; }
    public JavaPorticoOptions setMethodNaming(NamingConvention methodNaming) { this.methodNaming = methodNaming; return this; }

    public String getServiceNameSuffix() { return serviceNameSuffix; }
    public JavaPorticoOptions setServiceNameSuffix(String serviceNameSuffix) { this.serviceNameSuffix = serviceNameSuffix; return this; }

    public boolean isEnableProxyGeneration() { return enableProxyGeneration; }
    public JavaPorticoOptions setEnableProxyGeneration(boolean enableProxyGeneration) { this.enableProxyGeneration = enableProxyGeneration; return this; }

    public String getProxyBaseUrl() { return proxyBaseUrl; }
    public JavaPorticoOptions setProxyBaseUrl(String proxyBaseUrl) { this.proxyBaseUrl = proxyBaseUrl; return this; }

    public String getProxyApiKeyHeaderName() { return proxyApiKeyHeaderName; }
    public JavaPorticoOptions setProxyApiKeyHeaderName(String proxyApiKeyHeaderName) { this.proxyApiKeyHeaderName = proxyApiKeyHeaderName; return this; }

    public int getProxyCacheTtlSeconds() { return proxyCacheTtlSeconds; }
    public JavaPorticoOptions setProxyCacheTtlSeconds(int proxyCacheTtlSeconds) { this.proxyCacheTtlSeconds = proxyCacheTtlSeconds; return this; }

    public String getProxyBypassCacheMetadataKey() { return proxyBypassCacheMetadataKey; }
    public JavaPorticoOptions setProxyBypassCacheMetadataKey(String proxyBypassCacheMetadataKey) { this.proxyBypassCacheMetadataKey = proxyBypassCacheMetadataKey; return this; }

    public String getProxyClientKeyHeaderName() { return proxyClientKeyHeaderName; }
    public JavaPorticoOptions setProxyClientKeyHeaderName(String proxyClientKeyHeaderName) { this.proxyClientKeyHeaderName = proxyClientKeyHeaderName; return this; }

    public ClientKeyMode getProxyClientKeyMode() { return proxyClientKeyMode; }
    public JavaPorticoOptions setProxyClientKeyMode(ClientKeyMode proxyClientKeyMode) { this.proxyClientKeyMode = proxyClientKeyMode; return this; }

    public boolean isProxyAuditEnabled() { return proxyAuditEnabled; }
    public JavaPorticoOptions setProxyAuditEnabled(boolean proxyAuditEnabled) { this.proxyAuditEnabled = proxyAuditEnabled; return this; }
}
