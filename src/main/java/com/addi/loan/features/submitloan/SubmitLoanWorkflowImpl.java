package com.addi.loan.features.submitloan;

import com.addi.loan.features.checkcredit.CheckCreditWorkflow;
import com.addi.loan.features.checkcredit.dto.CreditCheckResult;
import com.addi.loan.features.submitloan.dto.LoanCommand;
import com.addi.loan.features.submitloan.dto.LoanDecision;
import com.addi.loan.features.submitloan.dto.LoanResult;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Workflow;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Smart Workflow Implementation - per ADR145
 *
 * ALL business logic lives here. This code is:
 * - Deterministic (no I/O, no randomness, no system time)
 * - Imperative (clear, step-by-step logic)
 * - Testable (using Temporal TestWorkflowEnvironment)
 *
 * This is the PRIMARY area GenAI agents will focus on building.
 */
public class SubmitLoanWorkflowImpl implements SubmitLoanWorkflow {

    // Workflow state
    private String status = "INITIATED";
    private String loanId;
    private LoanDecision decision;

    // Activity stub - Activities are "dumb I/O ports"
    private final SubmitLoanActivities activities = Workflow.newActivityStub(
        SubmitLoanActivities.class,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .setRetryOptions(RetryOptions.newBuilder()
                .setMaximumAttempts(3)
                .setInitialInterval(Duration.ofSeconds(1))
                .setBackoffCoefficient(2.0)
                .build())
            .build()
    );

    @Override
    public LoanResult processLoanApplication(LoanCommand command) {
        // Step 1: Save initial application (Activity = I/O)
        status = "SAVING";
        loanId = activities.saveLoanApplication(command);

        // Step 2: Execute credit check as Child Workflow
        status = "CHECKING_CREDIT";
        CheckCreditWorkflow creditWorkflow = Workflow.newChildWorkflowStub(
            CheckCreditWorkflow.class,
            ChildWorkflowOptions.newBuilder()
                .setWorkflowId("credit-check-" + loanId)
                .build()
        );
        CreditCheckResult creditResult = creditWorkflow.checkCredit(command.clientId());

        // Step 3: BUSINESS LOGIC - Determine loan decision (pure, deterministic)
        status = "EVALUATING";
        decision = evaluateLoanApplication(command, creditResult);

        // Step 4: Handle decision
        if (decision.requiresManualReview()) {
            status = "PENDING_REVIEW";
            // Wait for manual approval/rejection signal
            Workflow.await(() ->
                "APPROVED".equals(status) || "REJECTED".equals(status));
        } else {
            status = decision.status();
        }

        // Step 5: Update application with decision (Activity = I/O)
        activities.updateLoanDecision(loanId, status, decision.reason());

        // Step 6: Publish event for Data team (Activity = I/O, Kafka)
        activities.publishLoanDecisionEvent(loanId, command.clientId(), decision);

        return new LoanResult(loanId, status, decision.reason());
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public void approveManually(String approverComment) {
        if ("PENDING_REVIEW".equals(status)) {
            status = "APPROVED";
            decision = new LoanDecision("APPROVED", "Manual approval: " + approverComment, false);
        }
    }

    @Override
    public void rejectManually(String rejectionReason) {
        if ("PENDING_REVIEW".equals(status)) {
            status = "REJECTED";
            decision = new LoanDecision("REJECTED", "Manual rejection: " + rejectionReason, false);
        }
    }

    /**
     * Pure business logic - deterministic, testable, no I/O
     *
     * This method contains the core decision-making logic that GenAI agents
     * will focus on building and modifying.
     */
    private LoanDecision evaluateLoanApplication(LoanCommand command, CreditCheckResult credit) {
        // Rule 1: Reject if credit score too low
        if (credit.score() < 500) {
            return new LoanDecision("REJECTED",
                "Credit score " + credit.score() + " below minimum threshold of 500", false);
        }

        // Rule 2: Reject if amount exceeds credit limit
        if (command.amount().compareTo(credit.maxLoanAmount()) > 0) {
            return new LoanDecision("REJECTED",
                String.format("Requested amount %s exceeds credit limit %s",
                    command.amount(), credit.maxLoanAmount()), false);
        }

        // Rule 3: Auto-approve for excellent credit with reasonable amount
        if (credit.score() >= 750 &&
            command.amount().compareTo(BigDecimal.valueOf(50000)) <= 0) {
            return new LoanDecision("APPROVED",
                "Auto-approved: Excellent credit score " + credit.score(), false);
        }

        // Rule 4: Auto-approve for good credit with small amount
        if (credit.score() >= 650 &&
            command.amount().compareTo(BigDecimal.valueOf(10000)) <= 0) {
            return new LoanDecision("APPROVED",
                "Auto-approved: Good credit with small loan amount", false);
        }

        // Rule 5: Manual review for edge cases
        return new LoanDecision("PENDING_REVIEW",
            String.format("Manual review required: credit=%d, amount=%s",
                credit.score(), command.amount()), true);
    }
}
