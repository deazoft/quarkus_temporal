package com.addi.loan.infrastructure.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Observability Configuration - OpenTelemetry setup
 *
 * Provides centralized access to OpenTelemetry primitives.
 * Per ADR145/ADR093: Tracing-First with business ID propagation.
 */
@ApplicationScoped
public class ObservabilityConfig {

    private static final Logger LOG = Logger.getLogger(ObservabilityConfig.class);

    @Inject
    OpenTelemetry openTelemetry;

    @Produces
    @ApplicationScoped
    public Tracer tracer() {
        LOG.info("Creating OpenTelemetry tracer for loan-application-service");
        return openTelemetry.getTracer("loan-application-service", "1.0.0");
    }
}
