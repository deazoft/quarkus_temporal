package com.addi.loan.features.submitloan.dto;

import java.time.Instant;

/**
 * LoanDecisionEvent - Kafka event for Data team
 *
 * Published to Kafka for data contracts per ADR145 constraints.
 * Consumed by data pipelines and analytics systems.
 */
public record LoanDecisionEvent(
    String loanId,
    String clientId,
    String status,
    String reason,
    Instant timestamp
) {}
