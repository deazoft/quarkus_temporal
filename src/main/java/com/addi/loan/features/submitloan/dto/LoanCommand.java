package com.addi.loan.features.submitloan.dto;

import java.math.BigDecimal;

/**
 * LoanCommand - Workflow input
 *
 * Command object passed to the Temporal workflow.
 * Must be serializable for Temporal.
 */
public record LoanCommand(
    String clientId,
    BigDecimal amount,
    int termMonths,
    String purpose
) {}
