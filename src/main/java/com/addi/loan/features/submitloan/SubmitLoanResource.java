package com.addi.loan.features.submitloan;

import com.addi.loan.features.submitloan.dto.SubmitLoanRequest;
import com.addi.loan.features.submitloan.dto.SubmitLoanResponse;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * REST Thin Trigger - per ADR145 "Thin Triggers, Smart Workflows, Dumb Activities"
 *
 * This endpoint ONLY:
 * 1. Validates input
 * 2. Adds business IDs to trace context
 * 3. Starts the Temporal workflow
 *
 * NO business logic here!
 */
@Path("/api/v1/loans")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SubmitLoanResource {

    private static final Logger LOG = Logger.getLogger(SubmitLoanResource.class);
    private static final String TASK_QUEUE = "loan-processing";

    @Inject
    WorkflowClient workflowClient;

    @POST
    @WithSpan("submitLoan")
    public Uni<Response> submitLoan(@Valid SubmitLoanRequest request) {
        String workflowId = "loan-" + UUID.randomUUID();

        // Tracing-First: Add business IDs to trace context (per ADR145/ADR093)
        Span.current().setAttribute("client.id", request.clientId());
        Span.current().setAttribute("loan.id", workflowId);

        LOG.infof("Submitting loan application: clientId=%s, workflowId=%s",
            request.clientId(), workflowId);

        return Uni.createFrom().item(() -> {
            WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(TASK_QUEUE)
                .build();

            SubmitLoanWorkflow workflow = workflowClient.newWorkflowStub(
                SubmitLoanWorkflow.class, options);

            // THIN TRIGGER: Start workflow asynchronously
            WorkflowClient.start(workflow::processLoanApplication, request.toCommand());

            return Response.accepted(
                new SubmitLoanResponse(workflowId, "PROCESSING", "Loan application submitted")
            ).build();
        });
    }

    @GET
    @Path("/{loanId}")
    @WithSpan("getLoanStatus")
    public Uni<Response> getLoanStatus(@PathParam("loanId") String loanId) {
        Span.current().setAttribute("loan.id", loanId);

        return Uni.createFrom().item(() -> {
            SubmitLoanWorkflow workflow = workflowClient.newWorkflowStub(
                SubmitLoanWorkflow.class, loanId);

            // Query workflow state
            String status = workflow.getStatus();

            return Response.ok(new SubmitLoanResponse(loanId, status, null)).build();
        });
    }
}
