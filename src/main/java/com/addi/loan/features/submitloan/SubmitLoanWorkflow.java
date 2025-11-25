package com.addi.loan.features.submitloan;

import com.addi.loan.features.submitloan.dto.LoanCommand;
import com.addi.loan.features.submitloan.dto.LoanResult;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Workflow Interface - Clean Interface per ADR145
 *
 * This is the strongly-typed contract that both humans and GenAI coding agents code against.
 * Defines the main workflow method, queries, and signals.
 */
@WorkflowInterface
public interface SubmitLoanWorkflow {

    @WorkflowMethod
    LoanResult processLoanApplication(LoanCommand command);

    @QueryMethod
    String getStatus();

    @SignalMethod
    void approveManually(String approverComment);

    @SignalMethod
    void rejectManually(String rejectionReason);
}
