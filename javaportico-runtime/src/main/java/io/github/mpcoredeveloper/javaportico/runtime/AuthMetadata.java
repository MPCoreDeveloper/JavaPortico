package io.github.mpcoredeveloper.javaportico.runtime;

import io.grpc.Metadata;

/**
 * Metadata helpers for Bearer / API-Key / OAuth2 authentication. The functional equivalent of
 * SharpPortico's generated auth metadata helpers.
 */
public final class AuthMetadata {

    private static final Metadata.Key<String> AUTHORIZATION =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    private AuthMetadata() {
    }

    public static Metadata bearer(String token) {
        Metadata m = new Metadata();
        m.put(AUTHORIZATION, "Bearer " + token);
        return m;
    }

    public static Metadata apiKey(String headerName, String key) {
        Metadata m = new Metadata();
        m.put(Metadata.Key.of(headerName, Metadata.ASCII_STRING_MARSHALLER), key);
        return m;
    }

    public static Metadata oauth2(String token) {
        return bearer(token);
    }
}
