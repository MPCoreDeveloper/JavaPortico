package com.example.springproxy;

import com.example.generated.PetServiceProxy;
import io.github.mpcoredeveloper.javaportico.annotations.ClientKeyMode;
import io.github.mpcoredeveloper.javaportico.runtime.DefaultAuditLogger;
import io.github.mpcoredeveloper.javaportico.runtime.DelegateKeyProvider;
import io.github.mpcoredeveloper.javaportico.runtime.HttpRestClient;
import io.github.mpcoredeveloper.javaportico.runtime.IClientKeyValidator;
import io.github.mpcoredeveloper.javaportico.runtime.IKeyProvider;
import io.github.mpcoredeveloper.javaportico.runtime.IProxyAuditLogger;
import io.github.mpcoredeveloper.javaportico.runtime.IProxyCache;
import io.github.mpcoredeveloper.javaportico.runtime.IRestClient;
import io.github.mpcoredeveloper.javaportico.runtime.MemoryProxyCache;
import io.github.mpcoredeveloper.javaportico.runtime.ProxyOptions;
import io.github.mpcoredeveloper.javaportico.runtime.UlidClientKeyValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * Spring DI wiring of the generated {@code PetServiceProxy} pipeline.
 * Demonstrates how keys/cache/validator/audit are provided as beans (config, vault, etc.).
 */
@Configuration
public class ProxyConfiguration {

    public static final String CLIENT_KEY = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

    @Bean
    public ProxyOptions proxyOptions() {
        return new ProxyOptions()
                .setBaseUrl("http://localhost:8099")
                .setApiKeyHeaderName("X-Api-Key")
                .setClientKeyMode(ClientKeyMode.OWN)
                .setCacheTtl(Duration.ofSeconds(60));
    }

    @Bean
    public IRestClient restClient() {
        return new HttpRestClient("http://localhost:8099");
    }

    @Bean
    public IProxyCache proxyCache() {
        return new MemoryProxyCache();
    }

    @Bean
    public IKeyProvider keyProvider() {
        // From config / vault in a real deployment — never hardcoded.
        return new DelegateKeyProvider(headerName -> LegacyPetstoreController.LEGACY_KEY);
    }

    @Bean
    public IClientKeyValidator clientKeyValidator() {
        return new UlidClientKeyValidator(List.of(CLIENT_KEY));
    }

    @Bean
    public IProxyAuditLogger auditLogger() {
        return new DefaultAuditLogger();
    }

    @Bean
    public PetServiceProxy petServiceProxy(ProxyOptions options,
                                           IRestClient rest,
                                           IProxyCache cache,
                                           IKeyProvider keys,
                                           IClientKeyValidator validator,
                                           IProxyAuditLogger audit) {
        return new PetServiceProxy(options, rest, cache, keys, validator, audit);
    }
}
