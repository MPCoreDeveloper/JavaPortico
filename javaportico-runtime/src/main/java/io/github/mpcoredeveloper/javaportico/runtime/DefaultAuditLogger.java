package io.github.mpcoredeveloper.javaportico.runtime;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default audit logger writing to {@link java.util.logging.Logger}.
 */
public final class DefaultAuditLogger implements IProxyAuditLogger {

    private static final Logger LOG = Logger.getLogger(DefaultAuditLogger.class.getName());

    @Override
    public void log(AuditEntry entry) {
        // Supplier-based logging defers string construction until the INFO level is enabled.
        LOG.log(Level.INFO, () -> "[portico-audit] service=" + nz(entry.serviceName())
                + " rpc=" + nz(entry.rpcName())
                + " client=" + nz(entry.clientKeyId())
                + " peer=" + nz(entry.clientAddress())
                + " cacheHit=" + entry.cacheHit()
                + " http=" + (entry.httpStatusCode() == null ? "-" : entry.httpStatusCode())
                + " at=" + (entry.timestampUtc() == null ? "-" : entry.timestampUtc()));
    }

    private static String nz(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }
}
