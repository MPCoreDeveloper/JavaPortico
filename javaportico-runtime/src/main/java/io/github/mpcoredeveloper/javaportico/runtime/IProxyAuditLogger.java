package io.github.mpcoredeveloper.javaportico.runtime;

/**
 * Optional audit logger. The generated proxy reports every call; a default implementation writes
 * to {@link java.util.logging.Logger}. Swap for a SIEM event sink by implementing this interface.
 */
public interface IProxyAuditLogger {

    void log(AuditEntry entry);
}
