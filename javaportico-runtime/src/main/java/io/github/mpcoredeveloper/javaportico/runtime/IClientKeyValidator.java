package io.github.mpcoredeveloper.javaportico.runtime;

/**
 * Validates client keys presented by gRPC callers. Mirrors SharpPortico's {@code IClientKeyValidator}.
 */
public interface IClientKeyValidator {

    /** Returns true when the presented key is valid. */
    boolean isValid(String key);

    /** Optional display name/issuer hint for the key (for audit logging). */
    String describe(String key);
}
