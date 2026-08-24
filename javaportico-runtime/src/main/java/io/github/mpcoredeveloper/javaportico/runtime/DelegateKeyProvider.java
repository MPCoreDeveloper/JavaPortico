package io.github.mpcoredeveloper.javaportico.runtime;

import java.util.function.Function;

/**
 * Decorator that resolves keys through a delegate — perfect for vault/secret-store integration.
 */
public final class DelegateKeyProvider implements IKeyProvider {

    private final Function<String, String> get;

    public DelegateKeyProvider(Function<String, String> get) {
        this.get = get;
    }

    @Override
    public String getApiKey(String headerName) {
        return get.apply(headerName);
    }
}
