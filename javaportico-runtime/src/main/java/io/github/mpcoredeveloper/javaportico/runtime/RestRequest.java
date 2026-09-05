package io.github.mpcoredeveloper.javaportico.runtime;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RestRequest that)) return false;
        return Objects.equals(method, that.method)
                && Objects.equals(pathTemplate, that.pathTemplate)
                && Objects.equals(pathParameters, that.pathParameters)
                && Objects.equals(queryParameters, that.queryParameters)
                && Objects.equals(headers, that.headers)
                && Arrays.equals(body, that.body)
                && Objects.equals(contentType, that.contentType);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(method, pathTemplate, pathParameters, queryParameters, headers, contentType);
        return 31 * result + Arrays.hashCode(body);
    }

    @Override
    public String toString() {
        return "RestRequest[method=" + method + ", pathTemplate=" + pathTemplate
                + ", pathParameters=" + pathParameters + ", queryParameters=" + queryParameters
                + ", headers=" + headers + ", body=" + Arrays.toString(body)
                + ", contentType=" + contentType + "]";
    }
}
