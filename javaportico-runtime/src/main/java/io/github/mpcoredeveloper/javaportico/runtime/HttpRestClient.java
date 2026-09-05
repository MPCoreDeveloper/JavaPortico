package io.github.mpcoredeveloper.javaportico.runtime;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * {@link IRestClient} backed by {@link java.net.http.HttpClient}. Substitutes path parameters
 * (case-insensitively), appends query parameters, sets headers and dispatches the request.
 * Ported from SharpPortico's {@code HttpRestClient}.
 */
public final class HttpRestClient implements IRestClient, AutoCloseable {

    /** Default cap on a single response body (64 MiB) to avoid unbounded heap growth. */
    private static final int DEFAULT_MAX_RESPONSE_BYTES = 64 * 1024 * 1024;

    private final HttpClient http;
    private final boolean ownsHttpClient;
    private final String baseUrl;
    private final int maxResponseBytes;

    public HttpRestClient(HttpClient http, String baseUrl) {
        this(http, baseUrl, DEFAULT_MAX_RESPONSE_BYTES);
    }

    public HttpRestClient(HttpClient http, String baseUrl, int maxResponseBytes) {
        this.ownsHttpClient = http == null;
        this.http = http != null ? http : HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.baseUrl = stripTrailingSlashes(baseUrl == null ? "" : baseUrl);
        this.maxResponseBytes = Math.max(1, maxResponseBytes);
    }

    public HttpRestClient(String baseUrl) {
        this(null, baseUrl, DEFAULT_MAX_RESPONSE_BYTES);
    }

    /**
     * Releases the {@link HttpClient} connection/thread pools, but only when this instance
     * created the client internally (the two-argument constructor keeps caller-owned clients open).
     */
    @Override
    public void close() {
        if (ownsHttpClient) http.close();
    }

    @Override
    public RestResponse send(RestRequest request) {
        String uri = buildUri(request);
        HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(uri));
        if (request.body() != null && request.body().length > 0) {
            rb.method(request.method(), HttpRequest.BodyPublishers.ofByteArray(request.body()));
            rb.header("Content-Type", request.contentType());
        } else {
            rb.method(request.method(), HttpRequest.BodyPublishers.noBody());
        }
        for (Map.Entry<String, String> e : request.headers().entrySet()) {
            rb.header(e.getKey(), e.getValue());
        }
        try {
            HttpResponse<InputStream> resp = http.send(rb.build(), HttpResponse.BodyHandlers.ofInputStream());
            String ct = resp.headers().firstValue("Content-Type").orElse("application/json");
            try (InputStream in = resp.body()) {
                byte[] body = in.readNBytes(maxResponseBytes + 1);
                if (body.length > maxResponseBytes) {
                    throw new RestException("response body exceeds the " + maxResponseBytes + " byte limit");
                }
                return new RestResponse(resp.statusCode(), body, ct);
            }
        } catch (java.io.IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RestException(e);
        }
    }

    private String buildUri(RestRequest request) {
        String path = request.pathTemplate() == null ? "" : request.pathTemplate();
        for (Map.Entry<String, String> e : request.pathParameters().entrySet()) {
            String token = "{" + e.getKey() + "}";
            String escaped = percentEncode(e.getValue() == null ? "" : e.getValue());
            path = replaceCaseInsensitive(path, token, escaped);
        }

        StringBuilder sb = new StringBuilder(baseUrl).append(path);
        if (!request.queryParameters().isEmpty()) {
            char sep = '?';
            for (Map.Entry<String, String> e : request.queryParameters().entrySet()) {
                sb.append(sep)
                        .append(percentEncode(e.getKey()))
                        .append('=')
                        .append(percentEncode(e.getValue() == null ? "" : e.getValue()));
                sep = '&';
            }
        }
        return sb.toString();
    }

    private static String replaceCaseInsensitive(String source, String token, String replacement) {
        String lower = source.toLowerCase(java.util.Locale.ROOT);
        String lowerToken = token.toLowerCase(java.util.Locale.ROOT);
        int idx = lower.indexOf(lowerToken);
        if (idx < 0) return source;
        return source.substring(0, idx) + replacement + source.substring(idx + token.length());
    }

    private static String stripTrailingSlashes(String url) {
        int end = url.length();
        while (end > 0 && url.charAt(end - 1) == '/') {
            end--;
        }
        return end == url.length() ? url : url.substring(0, end);
    }

    private static String percentEncode(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (byte b : s.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xFF;
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '.' || c == '~') {
                sb.append((char) c);
            } else {
                sb.append('%').append(String.format("%02X", c));
            }
        }
        return sb.toString();
    }
}
