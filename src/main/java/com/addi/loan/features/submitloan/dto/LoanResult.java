package com.addi.loan.features.submitloan.dto;

/**
 * LoanResult - Workflow output
 *
 * Result returned from the Temporal workflow.
 */
public record LoanResult(
    String loanId,
    String status,
    String reason
) {}
