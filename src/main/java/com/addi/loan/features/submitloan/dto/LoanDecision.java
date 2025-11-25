package com.addi.loan.features.submitloan.dto;

/**
 * LoanDecision - Internal workflow decision
 *
 * Represents the decision made by the workflow's business logic.
 */
public record LoanDecision(
    String status,
    String reason,
    boolean requiresManualReview
) {}
