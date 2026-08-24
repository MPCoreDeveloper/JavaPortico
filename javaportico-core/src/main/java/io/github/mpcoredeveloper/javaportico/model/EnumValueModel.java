package io.github.mpcoredeveloper.javaportico.model;

/**
 * One value of a generated enum.
 *
 * @param name    sanitized Pascal identifier (proto name derived as ALL_CAPS)
 * @param number  protobuf enum number
 * @param rawName raw OpenAPI value (used for case-insensitive JSON matching in the proxy)
 */
public record EnumValueModel(String name, int number, String rawName) {
}
