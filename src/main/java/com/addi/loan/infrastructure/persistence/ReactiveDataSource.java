package com.addi.loan.infrastructure.persistence;

import io.vertx.mutiny.pgclient.PgPool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Reactive DataSource - Non-blocking PostgreSQL access
 *
 * Wrapper for reactive PostgreSQL connection pool.
 * Uses Vert.x Reactive PG Client for non-blocking I/O per ADR145.
 */
@ApplicationScoped
public class ReactiveDataSource {

    private static final Logger LOG = Logger.getLogger(ReactiveDataSource.class);

    @Inject
    PgPool pgPool;

    public PgPool getPool() {
        return pgPool;
    }
}
