package io.github.mpcoredeveloper.javaportico.model;

/** The kind of an RPC (mirrors gRPC method types). */
public enum RpcKind {
    UNARY,
    SERVER_STREAMING,
    CLIENT_STREAMING,
    BIDI_STREAMING
}
