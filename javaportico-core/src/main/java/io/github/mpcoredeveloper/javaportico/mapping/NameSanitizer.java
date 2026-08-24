package io.github.mpcoredeveloper.javaportico.mapping;

import java.util.ArrayList;
import java.util.List;

/**
 * Identifier sanitizers ported from SharpPortico's SchemaMapper.
 */
public final class NameSanitizer {

    private NameSanitizer() {
    }

    /** PascalCase sanitizer (C# convention). */
    public static String sanitizePascal(String input) {
        if (input == null || input.isEmpty()) return "Field";
        StringBuilder chars = new StringBuilder(input.length());
        boolean upper = true;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                chars.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            } else {
                upper = true;
            }
        }
        if (chars.length() == 0) return "Field";
        if (Character.isDigit(chars.charAt(0))) chars.insert(0, 'N');
        return chars.toString();
    }

    /** snake_case protobuf field name converter. */
    public static String toProtoName(String name) {
        StringBuilder chars = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                if (chars.length() > 0 && Character.isUpperCase(c)) {
                    char prev = chars.charAt(chars.length() - 1);
                    if (Character.isLowerCase(prev)) {
                        chars.append('_');
                    }
                }
                chars.append(Character.toLowerCase(c));
            } else if (c == '_' || c == '-' || c == '.' || c == ' ' || c == ':') {
                chars.append('_');
            }
        }
        String result = chars.toString();
        if (result.isEmpty()) return "field";
        if (Character.isDigit(result.charAt(0))) result = "_" + result;
        return result;
    }

    /** Sanitizes a raw enum value into a Pascal identifier with a fallback. */
    public static String sanitizeEnumName(String raw, int fallback) {
        String pascal = sanitizePascal(raw);
        if ((pascal.equals("Field") && (raw == null || raw.isBlank())) || pascal.isEmpty()) {
            return "VALUE_" + fallback;
        }
        if (Character.isDigit(pascal.charAt(0))) pascal = "V_" + pascal;
        return pascal;
    }

    /** All-caps snake case for proto enum value names. */
    public static String toProtoEnumName(String name) {
        StringBuilder sb = new StringBuilder(name.length() + 4);
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c) && i > 0) sb.append('_');
            sb.append(Character.toUpperCase(c));
        }
        return sb.toString();
    }

    /** camelCase helper (lowercases the first character). */
    public static String camel(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    /** Extracts the identifier from a {@code $ref} string like {@code #/components/schemas/Pet}. */
    public static String refId(String ref) {
        if (ref == null) return "";
        int idx = ref.lastIndexOf('/');
        return idx >= 0 ? ref.substring(idx + 1) : ref;
    }

    /** Collects identifiers that differ in at most case. */
    public static List<String> unique(List<String> names) {
        List<String> result = new ArrayList<>();
        for (String n : names) {
            boolean found = false;
            for (String r : result) {
                if (r.equalsIgnoreCase(n)) { found = true; break; }
            }
            if (!found) result.add(n);
        }
        return result;
    }
}
