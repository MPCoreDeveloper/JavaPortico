package io.github.mpcoredeveloper.javaportico.runtime;

/**
 * Supplies the outbound API key used to authenticate against the legacy OpenAPI service.
 * Implementations must never hardcode secrets — config, a secret store or a vault is expected.
 * Mirrors SharpPortico's {@code IKeyProvider}.
 */
public interface IKeyProvider {

    /** Returns the API key for the given header name, or null when unavailable. */
    String getApiKey(String headerName);
}
