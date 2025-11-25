package com.addi.loan.features.submitloan;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

/**
 * Reactive Repository - Non-blocking PostgreSQL access
 *
 * Uses Vert.x Reactive PG Client for non-blocking I/O per ADR145.
 * This is a "dumb I/O port" used by Activities.
 */
@ApplicationScoped
public class LoanApplicationRepository {

    @Inject
    PgPool client;

    public Uni<LoanApplicationEntity> save(LoanApplicationEntity entity) {
        String id = UUID.randomUUID().toString();
        entity.setId(id);

        return client.preparedQuery("""
            INSERT INTO loan_applications
                (id, client_id, amount, term_months, purpose, status, created_at, updated_at)
            VALUES ($1, $2, $3, $4, $5, $6, NOW(), NOW())
            RETURNING *
            """)
            .execute(Tuple.of(
                entity.getId(),
                entity.getClientId(),
                entity.getAmount(),
                entity.getTerm(),
                entity.getPurpose(),
                entity.getStatus()
            ))
            .map(rows -> mapToEntity(rows.iterator().next()));
    }

    public Uni<Void> updateDecision(String loanId, String status, String reason) {
        return client.preparedQuery("""
            UPDATE loan_applications
            SET status = $1, decision_reason = $2, updated_at = NOW()
            WHERE id = $3
            """)
            .execute(Tuple.of(status, reason, loanId))
            .replaceWithVoid();
    }

    public Uni<LoanApplicationEntity> findById(String loanId) {
        return client.preparedQuery("""
            SELECT * FROM loan_applications WHERE id = $1
            """)
            .execute(Tuple.of(loanId))
            .map(rows -> {
                var iterator = rows.iterator();
                return iterator.hasNext() ? mapToEntity(iterator.next()) : null;
            });
    }

    public Uni<LoanApplicationEntity> findByClientId(String clientId) {
        return client.preparedQuery("""
            SELECT * FROM loan_applications
            WHERE client_id = $1
            ORDER BY created_at DESC
            LIMIT 1
            """)
            .execute(Tuple.of(clientId))
            .map(rows -> {
                var iterator = rows.iterator();
                return iterator.hasNext() ? mapToEntity(iterator.next()) : null;
            });
    }

    private LoanApplicationEntity mapToEntity(Row row) {
        return LoanApplicationEntity.builder()
            .id(row.getString("id"))
            .clientId(row.getString("client_id"))
            .amount(row.getBigDecimal("amount"))
            .term(row.getInteger("term_months"))
            .purpose(row.getString("purpose"))
            .status(row.getString("status"))
            .decisionReason(row.getString("decision_reason"))
            .createdAt(row.getLocalDateTime("created_at"))
            .updatedAt(row.getLocalDateTime("updated_at"))
            .build();
    }
}
