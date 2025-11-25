package com.addi.loan.features.submitloan;

import com.addi.loan.features.submitloan.dto.LoanCommand;
import com.addi.loan.features.submitloan.dto.LoanDecision;
import com.addi.loan.features.submitloan.dto.LoanDecisionEvent;
import com.addi.loan.infrastructure.kafka.EventPublisher;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;

/**
 * Dumb Activities Implementation - per ADR145
 *
 * These are simple, non-deterministic I/O wrappers.
 * They delegate all I/O to the platform-infra components.
 *
 * NO business logic here! Just I/O.
 */
@ApplicationScoped
public class SubmitLoanActivitiesImpl implements SubmitLoanActivities {

    private static final Logger LOG = Logger.getLogger(SubmitLoanActivitiesImpl.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @Inject
    LoanApplicationRepository repository;

    @Inject
    EventPublisher eventPublisher;

    @Override
    @WithSpan("activity.saveLoanApplication")
    public String saveLoanApplication(LoanCommand command) {
        // Add business IDs to span
        Span.current().setAttribute("client.id", command.clientId());

        LOG.infof("Saving loan application for client: %s", command.clientId());

        LoanApplicationEntity entity = LoanApplicationEntity.builder()
            .clientId(command.clientId())
            .amount(command.amount())
            .term(command.termMonths())
            .purpose(command.purpose())
            .status("INITIATED")
            .build();

        return repository.save(entity)
            .map(LoanApplicationEntity::getId)
            .await().atMost(TIMEOUT);
    }

    @Override
    @WithSpan("activity.updateLoanDecision")
    public void updateLoanDecision(String loanId, String status, String reason) {
        Span.current().setAttribute("loan.id", loanId);
        Span.current().setAttribute("decision.status", status);

        LOG.infof("Updating loan decision: loanId=%s, status=%s", loanId, status);

        repository.updateDecision(loanId, status, reason)
            .await().atMost(TIMEOUT);
    }

    @Override
    @WithSpan("activity.publishLoanDecisionEvent")
    public void publishLoanDecisionEvent(String loanId, String clientId, LoanDecision decision) {
        Span.current().setAttribute("loan.id", loanId);
        Span.current().setAttribute("client.id", clientId);

        LOG.infof("Publishing loan decision event: loanId=%s", loanId);

        // Create event for Data team (Kafka contract per ADR145 constraint)
        LoanDecisionEvent event = new LoanDecisionEvent(
            loanId,
            clientId,
            decision.status(),
            decision.reason(),
            Instant.now()
        );

        eventPublisher.publish("loan-decisions", loanId, event)
            .await().atMost(TIMEOUT);
    }

    @Override
    @WithSpan("activity.findLoanById")
    public LoanApplicationEntity findLoanById(String loanId) {
        Span.current().setAttribute("loan.id", loanId);

        return repository.findById(loanId)
            .await().atMost(TIMEOUT);
    }
}
