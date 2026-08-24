package io.github.mpcoredeveloper.javaportico.runtime;

import java.time.Instant;

/**
 * Audit entry describing one proxied gRPC-to-REST call.
 */
public record AuditEntry(
        String serviceName,
        String rpcName,
        String clientKeyId,
        String clientAddress,
        boolean cacheHit,
        Instant timestampUtc,
        Integer httpStatusCode) {
}
