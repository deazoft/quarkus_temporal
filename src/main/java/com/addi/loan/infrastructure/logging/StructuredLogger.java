package com.addi.loan.infrastructure.logging;

import io.opentelemetry.api.trace.Span;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * Structured Logger - OTEL-enriched logging
 *
 * Provides structured logging with automatic OpenTelemetry context enrichment.
 * Per ADR145/ADR093: All logs include traceId and spanId.
 */
@ApplicationScoped
public class StructuredLogger {

    private static final Logger LOG = Logger.getLogger(StructuredLogger.class);

    public void info(String message, Map<String, Object> context) {
        enrichWithOtel(context);
        LOG.infof("%s | context=%s", message, context);
    }

    public void error(String message, Map<String, Object> context, Throwable t) {
        enrichWithOtel(context);
        LOG.errorf(t, "%s | context=%s", message, context);
    }

    public void debug(String message, Map<String, Object> context) {
        enrichWithOtel(context);
        LOG.debugf("%s | context=%s", message, context);
    }

    private void enrichWithOtel(Map<String, Object> context) {
        Span currentSpan = Span.current();
        if (currentSpan != null && currentSpan.getSpanContext().isValid()) {
            context.put("traceId", currentSpan.getSpanContext().getTraceId());
            context.put("spanId", currentSpan.getSpanContext().getSpanId());
        }
    }
}
