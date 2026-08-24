package io.github.mpcoredeveloper.javaportico.model;

/** Authentication scheme kinds mapped from the OpenAPI securitySchemes section. */
public enum AuthKind {
    BEARER,
    API_KEY,
    OAUTH2,
    UNKNOWN
}
