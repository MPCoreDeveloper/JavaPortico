package io.github.mpcoredeveloper.javaportico.runtime;

import io.grpc.Status;

/**
 * Maps HTTP status codes from the legacy REST service to gRPC {@link Status} objects.
 */
public final class GrpcStatusMapper {

    private GrpcStatusMapper() {
    }

    public static Status toStatus(int httpStatusCode, String message) {
        if (httpStatusCode >= 500) return Status.INTERNAL.withDescription(message);
        if (httpStatusCode == 401 || httpStatusCode == 403) return Status.UNAUTHENTICATED.withDescription(message);
        if (httpStatusCode == 404) return Status.NOT_FOUND.withDescription(message);
        if (httpStatusCode == 400) return Status.INVALID_ARGUMENT.withDescription(message);
        return Status.INVALID_ARGUMENT.withDescription(message);
    }
}
