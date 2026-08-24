package io.github.mpcoredeveloper.javaportico.model;

/** A single RPC derived from an OpenAPI path + HTTP verb. */
public record RpcModel(
        String name,
        String originalPath,
        String httpMethod,
        String requestType,
        String responseType,
        RpcKind kind,
        boolean hasPagination,
        String requestStreamingMessage,
        String responseStreamingMessage) {
}
