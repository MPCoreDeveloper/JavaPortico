package io.github.mpcoredeveloper.javaportico.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares an OpenAPI 3.0/3.1 specification that JavaPortico should turn into
 * gRPC + protobuf code. Mirrors SharpPortico's {@code [assembly: OpenApiToGrpc]}
 * attribute.
 *
 * <p>The annotation is processed by the {@code javaportico-maven-plugin} when a
 * specification is not declared in the POM configuration: package-level annotations
 * can be scanned to drive generation in Maven projects.</p>
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.PACKAGE)
@Repeatable(OpenApiToGrpcs.class)
public @interface OpenApiToGrpc {

    /** Path to the OpenAPI specification, relative to the project directory. */
    String file();

    /** Optional generated service name (e.g. {@code "PetService"}). */
    String serviceName() default "";

    /** Optional Java package for generated code (e.g. {@code "com.example.generated"}). */
    String namespace() default "";

    /** When {@code true} (default), a {@code *.proto} file is emitted. */
    boolean emitProtoFile() default true;

    /** When {@code true} (default), client stubs are generated (via grpc-java). */
    boolean emitClient() default true;

    /** When {@code true} (default), server base stubs are generated (via grpc-java). */
    boolean emitServer() default true;

    /** When {@code true} (default), DI wiring helpers are emitted. */
    boolean emitDependencyInjection() default true;

    /** When {@code false}, unary RPCs are always generated even when a large payload or an x-grpc-streaming hint is present. */
    boolean respectStreamingHints() default true;

    /** Payload size in bytes above which a request body is treated as large and eligible for client-streaming. */
    int largePayloadStreamingThresholdBytes() default 1_000_000;

    /** When {@code false}, OpenAPI enums are emitted as plain Java enums instead of protobuf enums. */
    boolean mapEnumsToProtobufEnums() default true;

    /** When {@code true}, constructs that cannot be mapped faithfully are reported as warnings. */
    boolean emitDiagnosticsForUnmappableConstructs() default true;

    /** When {@code true}, metadata helpers for Bearer / API-Key / OAuth2 are generated. */
    boolean generateAuthMetadataHelpers() default true;

    /** When {@code true}, gRPC call interceptors for authentication are generated. */
    boolean generateAuthInterceptors() default true;

    /** When {@code true}, page/limit/cursor/next_page_token parameters are detected. */
    boolean detectPagination() default true;

    /** Name of the parameter treated as the page number. */
    String paginationPageParameter() default "page";

    /** Name of the parameter treated as the page size. */
    String paginationLimitParameter() default "limit";

    /** Name of the parameter treated as the pagination cursor. */
    String paginationCursorParameter() default "cursor";

    /** Name of the parameter treated as the next-page token. */
    String paginationNextPageTokenParameter() default "next_page_token";

    /** When {@code true}, error responses are wrapped in a google.rpc.Status-shaped message. */
    boolean emitGoogleRpcStatusWrapper() default true;

    /** Suffix appended to derived service names. */
    String serviceNameSuffix() default "Service";

    /** When {@code true}, a REST proxy ({@code {Service}Proxy}) is generated that forwards gRPC calls to the legacy REST service. */
    boolean enableProxyGeneration() default false;

    /** Base URL of the legacy REST service. When omitted, the first value of the OpenAPI {@code servers[].url} is used. */
    String proxyBaseUrl() default "";

    /** Outbound API-key header name (default {@code X-Api-Key}). */
    String proxyApiKeyHeaderName() default "X-Api-Key";

    /** Default response cache TTL in seconds (default 60). */
    int proxyCacheTtlSeconds() default 60;

    /** gRPC metadata key used for the per-call cache bypass. */
    String proxyBypassCacheMetadataKey() default "x-portico-bypass-cache";

    /** gRPC metadata key carrying the client key. */
    String proxyClientKeyHeaderName() default "x-portico-key";

    /** Inbound client-key mode: {@link ClientKeyMode}. Default NONE. */
    ClientKeyMode proxyClientKeyMode() default ClientKeyMode.NONE;

    /** When {@code true}, the generated proxy logs an audit entry per call. */
    boolean proxyAuditEnabled() default false;
}
