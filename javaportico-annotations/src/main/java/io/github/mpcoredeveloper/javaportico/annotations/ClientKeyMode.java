package io.github.mpcoredeveloper.javaportico.annotations;

/**
 * Inbound authentication mode for the generated gRPC-to-REST proxy.
 */
public enum ClientKeyMode {
    /** No inbound client authentication. */
    NONE,

    /** Forward the client key 1:1 as the outbound API key. */
    FORWARD,

    /** Validate a Portico (ULID) client key and use the configured outbound key. */
    OWN
}
