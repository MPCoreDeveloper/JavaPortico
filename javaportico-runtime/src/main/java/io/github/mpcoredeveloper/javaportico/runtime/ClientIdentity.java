package io.github.mpcoredeveloper.javaportico.runtime;

/** Identifies the calling client (from gRPC metadata). */
public record ClientIdentity(String keyId, String remoteAddress) {

    public String getKeyId() {
        return keyId;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }
}
