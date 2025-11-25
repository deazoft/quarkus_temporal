package com.addi.loan.features.checkcredit.dto;

import java.math.BigDecimal;

/**
 * CreditCheckResult - Output from credit check workflow
 *
 * Contains credit score and calculated loan limits.
 */
public record CreditCheckResult(
    int score,
    BigDecimal maxLoanAmount,
    boolean hasDelinquencies
) {}
