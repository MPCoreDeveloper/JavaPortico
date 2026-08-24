package io.github.mpcoredeveloper.javaportico.runtime;

import java.util.Map;

/**
 * Reads the API key from a configuration map. The configuration path is
 * {@code javaportico.proxy.apikeys.<headerName>} with a single fallback key
 * {@code javaportico.proxy.apikey}.
 */
public final class ConfigurationKeyProvider implements IKeyProvider {

    private final Map<String, String> config;

    public ConfigurationKeyProvider(Map<String, String> config) {
        this.config = config == null ? Map.of() : config;
    }

    @Override
    public String getApiKey(String headerName) {
        String key = config.get("javaportico.proxy.apikeys." + headerName);
        if (key == null || key.isBlank()) {
            key = config.get("javaportico.proxy.apikey");
        }
        return (key == null || key.isBlank()) ? null : key;
    }
}
