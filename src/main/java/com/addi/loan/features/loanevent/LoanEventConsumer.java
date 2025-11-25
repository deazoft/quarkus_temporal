package com.addi.loan.features.loanevent;

import com.addi.loan.features.loanevent.dto.LoanRequestedEvent;
import com.addi.loan.features.submitloan.SubmitLoanWorkflow;
import com.addi.loan.features.submitloan.dto.LoanCommand;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Kafka Thin Trigger - per ADR145
 *
 * This consumer ONLY:
 * 1. Receives Kafka events
 * 2. Adds business IDs to trace context
 * 3. Starts Temporal workflow (idempotent by workflowId)
 *
 * NO business logic here!
 */
@ApplicationScoped
public class LoanEventConsumer {

    private static final Logger LOG = Logger.getLogger(LoanEventConsumer.class);
    private static final String TASK_QUEUE = "loan-processing";

    @Inject
    WorkflowClient workflowClient;

    @Incoming("loan-requests")
    @Blocking
    @WithSpan("kafka.consume.loanRequest")
    public CompletionStage<Void> consume(LoanRequestedEvent event) {
        // Tracing-First: Add business IDs to trace context
        Span.current().setAttribute("client.id", event.clientId());
        Span.current().setAttribute("event.id", event.eventId());

        LOG.infof("Received loan request event: eventId=%s, clientId=%s",
            event.eventId(), event.clientId());

        String workflowId = "loan-" + event.eventId();

        WorkflowOptions options = WorkflowOptions.newBuilder()
            .setWorkflowId(workflowId)
            .setTaskQueue(TASK_QUEUE)
            .build();

        SubmitLoanWorkflow workflow = workflowClient.newWorkflowStub(
            SubmitLoanWorkflow.class, options);

        try {
            // THIN TRIGGER: Start workflow (idempotent by workflowId)
            LoanCommand command = event.toCommand();
            WorkflowClient.start(workflow::processLoanApplication, command);
            LOG.infof("Started workflow: %s", workflowId);
        } catch (WorkflowExecutionAlreadyStarted e) {
            // Idempotent - workflow already started, this is OK
            LOG.infof("Workflow already started (idempotent): %s", workflowId);
        }

        return CompletableFuture.completedFuture(null);
    }
}
