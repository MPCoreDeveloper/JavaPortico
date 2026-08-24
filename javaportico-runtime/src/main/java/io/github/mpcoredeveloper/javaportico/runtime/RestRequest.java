package io.github.mpcoredeveloper.javaportico.runtime;

import java.util.Map;

/**
 * Describes a REST request the generated proxy dispatches to the legacy OpenAPI service.
 * Mirrors SharpPortico's {@code RestRequest}.
 */
public record RestRequest(
        String method,
        String pathTemplate,
        Map<String, String> pathParameters,
        Map<String, String> queryParameters,
        Map<String, String> headers,
        byte[] body,
        String contentType) {

    public RestRequest {
        pathParameters = pathParameters == null ? Map.of() : Map.copyOf(pathParameters);
        queryParameters = queryParameters == null ? Map.of() : Map.copyOf(queryParameters);
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        contentType = contentType == null || contentType.isBlank() ? "application/json" : contentType;
    }
}
