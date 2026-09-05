package com.example.springproxy;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Mock legacy REST petstore served by Spring MVC on port 8099.
 * Requires {@code X-Api-Key} and counts every accepted call.
 */
@RestController
@RequestMapping("/pets")
public class LegacyPetstoreController {

    private static final Logger log = LoggerFactory.getLogger(LegacyPetstoreController.class);

    /** Inbound API-key header name used by the mock legacy service. */
    private static final String API_KEY_HEADER = "X-Api-Key";

    public static final String LEGACY_KEY = "legacy-secret-key";

    private final List<Map<String, Object>> pets = new CopyOnWriteArrayList<>();
    private final LegacyCallCounter counter;

    public LegacyPetstoreController(LegacyCallCounter counter) {
        this.counter = counter;
    }

    @PostConstruct
    public void seed() {
        pets.add(Map.of("id", 1L, "name", "Rex", "status", "available"));
        pets.add(Map.of("id", 2L, "name", "Milo", "status", "pending"));
    }

    @GetMapping
    public ResponseEntity<Object> list(HttpServletRequest request) {
        if (!LEGACY_KEY.equals(request.getHeader(API_KEY_HEADER))) {
            log.warn("Rejected {} {} without a valid X-Api-Key", request.getMethod(), request.getRequestURI());
            return ResponseEntity.status(401).body(null);
        }
        counter.increment();
        return ResponseEntity.ok(pets);
    }

    @GetMapping("/{petId}")
    public ResponseEntity<Object> get(@PathVariable long petId, HttpServletRequest request) {
        if (!LEGACY_KEY.equals(request.getHeader(API_KEY_HEADER))) {
            log.warn("Rejected {} {} without a valid X-Api-Key", request.getMethod(), request.getRequestURI());
            return ResponseEntity.status(401).body(null);
        }
        counter.increment();
        for (Map<String, Object> p : pets) {
            if (((Number) p.get("id")).longValue() == petId) {
                return ResponseEntity.ok(p);
            }
        }
        return ResponseEntity.status(404).body(null);
    }

    @PostMapping
    public ResponseEntity<Object> create(@RequestBody Map<String, Object> pet, HttpServletRequest request) {
        if (!LEGACY_KEY.equals(request.getHeader(API_KEY_HEADER))) {
            log.warn("Rejected {} {} without a valid X-Api-Key", request.getMethod(), request.getRequestURI());
            return ResponseEntity.status(401).body(null);
        }
        counter.increment();
        pets.add(pet);
        return ResponseEntity.status(201).body(pet);
    }
}
