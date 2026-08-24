package io.github.mpcoredeveloper.javaportico.runtime;

import io.grpc.Context;
import io.grpc.Metadata;

/**
 * Parses the gRPC call context into proxy inputs (bypass flag, client identity).
 *
 * <p>grpc-java does not expose request headers to {@code ImplBase} handlers directly;
 * the host registers {@link ProxyMetadataInterceptor} which captures the headers into
 * {@link #REQUEST_METADATA} on the current gRPC {@link Context}.</p>
 */
public final class ProxyContext {

    /** Context key carrying the captured request {@link Metadata}. */
    public static final Context.Key<Metadata> REQUEST_METADATA = Context.key("javaportico.request-metadata");

    private ProxyContext() {
    }

    /** Returns true when the per-call cache bypass metadata is present and true. */
    public static boolean parseBypass(Metadata headers, String key) {
        if (headers == null || key == null || key.isBlank()) return false;
        Metadata.Key<String> mk = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
        Iterable<String> values = headers.getAll(mk);
        if (values != null) {
            for (String v : values) {
                if (Boolean.parseBoolean(v)) return true;
            }
        }
        return false;
    }

    /** Reads the client key id from the request headers. */
    public static ClientIdentity readIdentity(Metadata headers, String clientKeyHeader) {
        String key = null;
        if (headers != null && clientKeyHeader != null && !clientKeyHeader.isBlank()) {
            Metadata.Key<String> mk = Metadata.Key.of(clientKeyHeader, Metadata.ASCII_STRING_MARSHALLER);
            String v = headers.get(mk);
            if (v != null) key = v;
        }
        return new ClientIdentity(key, null);
    }
}
