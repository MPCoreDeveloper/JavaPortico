package io.github.mpcoredeveloper.javaportico.runtime;

import java.util.function.UnaryOperator;

/**
 * Decorator that resolves keys through a delegate — perfect for vault/secret-store integration.
 */
public final class DelegateKeyProvider implements IKeyProvider {

    private final UnaryOperator<String> get;

    public DelegateKeyProvider(UnaryOperator<String> get) {
        this.get = get;
    }

    @Override
    public String getApiKey(String headerName) {
        return get.apply(headerName);
    }
}
