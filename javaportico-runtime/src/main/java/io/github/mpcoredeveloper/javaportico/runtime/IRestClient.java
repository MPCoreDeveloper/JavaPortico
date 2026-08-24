package io.github.mpcoredeveloper.javaportico.runtime;

/**
 * Minimal HTTP transport abstraction. Implementations wrap {@link java.net.http.HttpClient}
 * and return fully materialized responses so the pipeline stays sync-friendly and cacheable.
 */
public interface IRestClient {

    RestResponse send(RestRequest request);
}
