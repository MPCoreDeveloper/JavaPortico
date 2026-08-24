package io.github.mpcoredeveloper.javaportico.runtime;

import java.util.List;

/**
 * Aggregates several providers and returns the first non-blank key.
 */
public final class CompositeKeyProvider implements IKeyProvider {

    private final List<IKeyProvider> providers;

    public CompositeKeyProvider(IKeyProvider... providers) {
        this.providers = List.of(providers);
    }

    public CompositeKeyProvider(List<IKeyProvider> providers) {
        this.providers = providers == null ? List.of() : List.copyOf(providers);
    }

    @Override
    public String getApiKey(String headerName) {
        for (IKeyProvider p : providers) {
            String key = p.getApiKey(headerName);
            if (key != null && !key.isBlank()) return key;
        }
        return null;
    }
}
