package com.example.springproxy;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<?> list(HttpServletRequest request) {
        if (!LEGACY_KEY.equals(request.getHeader("X-Api-Key"))) {
            return ResponseEntity.status(401).build();
        }
        counter.increment();
        return ResponseEntity.ok(pets);
    }

    @GetMapping("/{petId}")
    public ResponseEntity<?> get(@PathVariable long petId, HttpServletRequest request) {
        if (!LEGACY_KEY.equals(request.getHeader("X-Api-Key"))) {
            return ResponseEntity.status(401).build();
        }
        counter.increment();
        return pets.stream()
                .filter(p -> ((Number) p.get("id")).longValue() == petId)
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> pet, HttpServletRequest request) {
        if (!LEGACY_KEY.equals(request.getHeader("X-Api-Key"))) {
            return ResponseEntity.status(401).build();
        }
        counter.increment();
        pets.add(pet);
        return ResponseEntity.status(201).body(pet);
    }
}
