package io.github.mpcoredeveloper.javaportico.runtime;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * Validates ULID-shaped client keys against an allowed list. The list is supplied by the host
 * (config, vault, DB) — never hardcoded. Accepts a custom validate hook so the host can integrate
 * any ULID implementation. Ported from SharpPortico's {@code UlidClientKeyValidator}.
 */
public final class UlidClientKeyValidator implements IClientKeyValidator {

    private final Set<String> allowedKeys;
    private final Function<String, Boolean> customValidate;

    public UlidClientKeyValidator(Collection<String> allowedKeys, Function<String, Boolean> customValidate) {
        this.allowedKeys = allowedKeys == null ? new HashSet<>() : new HashSet<>(allowedKeys);
        this.customValidate = customValidate;
    }

    public UlidClientKeyValidator(Collection<String> allowedKeys) {
        this(allowedKeys, null);
    }

    @Override
    public boolean isValid(String key) {
        if (key == null || key.isBlank()) return false;
        if (customValidate != null) return Boolean.TRUE.equals(customValidate.apply(key));
        if (!isUlidShape(key)) return false;
        return allowedKeys.contains(key);
    }

    @Override
    public String describe(String key) {
        // Try to derive the issuance timestamp from the ULID prefix (first 10 chars).
        if (key != null && key.length() >= 10) {
            Long millis = tryReadTimestampMillis(key.substring(0, 10));
            if (millis != null) {
                return "ulid:" + Instant.ofEpochMilli(millis).toString();
            }
        }
        return null;
    }

    /** Structural check only; the allowed-list enforces authority. */
    public static boolean isUlidShape(String value) {
        if (value == null || value.length() != 26) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean ok = (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
            if (!ok) return false;
            // Exclude I, L, O, U (Crockford base32 excludes them).
            if (c == 'I' || c == 'L' || c == 'O' || c == 'U' || c == 'i' || c == 'l' || c == 'o' || c == 'u') return false;
        }
        return true;
    }

    private static Long tryReadTimestampMillis(String prefix) {
        long value = 0;
        for (int i = 0; i < prefix.length(); i++) {
            int digit = decodeBase32(prefix.charAt(i));
            if (digit < 0) return null;
            value = (value << 5) | (long) digit;
        }
        long millis = value >> 8;
        return millis >= 0 ? millis : null;
    }

    private static int decodeBase32(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'A' && c <= 'Z') {
            return switch (c) {
                case 'I', 'L', 'O', 'U' -> -1;
                default -> c - 'A' + 10;
            };
        }
        if (c >= 'a' && c <= 'z') return decodeBase32(Character.toUpperCase(c));
        return -1;
    }
}
