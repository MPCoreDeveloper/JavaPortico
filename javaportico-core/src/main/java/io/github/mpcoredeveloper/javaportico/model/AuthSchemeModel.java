package io.github.mpcoredeveloper.javaportico.model;

/** An authentication scheme mapped from the OpenAPI securitySchemes section. */
public record AuthSchemeModel(
        String name,
        AuthKind kind,
        String headerName,
        String queryParameterName) {
}
