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
            // A valid ULID's first character encodes only 3 bits (values 0-7).
            if (i == 0 && digit > 7) return null;
            value = (value << 5) | (long) digit;
        }
        // ULID: the first 10 characters encode the 48-bit millisecond timestamp.
        // Ten Crockford base32 characters hold 50 bits, and the top 2 bits of a
        // valid ULID timestamp are always 0, so the timestamp is the low 48 bits
        // of the accumulated value. (A previous `>> 8` dropped 8 real timestamp
        // bits, making describe() report a time ~256x too small.)
        return value & 0xFFFF_FFFF_FFFFL;
    }

    private static int decodeBase32(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'z') c = Character.toUpperCase(c);
        if (c < 'A' || c > 'Z') return -1;
        return switch (c) {
            case 'I', 'L', 'O', 'U' -> -1;
            default -> {
                int v = c - 'A' + 10;
                // Crockford's base32 omits I, L, O and U from its alphabet, so every
                // letter after an omitted one maps to a value one less than its
                // contiguous A-Z position (e.g. J=18, M=20, R=24, Z=31).
                if (c > 'H') v -= 1; // skip I
                if (c > 'L') v -= 1; // skip L
                if (c > 'O') v -= 1; // skip O
                if (c > 'U') v -= 1; // skip U
                yield v;
            }
        };
    }
}
