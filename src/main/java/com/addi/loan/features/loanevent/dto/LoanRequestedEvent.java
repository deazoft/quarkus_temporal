package com.addi.loan.features.loanevent.dto;

import com.addi.loan.features.submitloan.dto.LoanCommand;

import java.math.BigDecimal;

/**
 * LoanRequestedEvent - Kafka event consumed from upstream systems
 *
 * This event triggers loan processing via Kafka instead of REST.
 */
public record LoanRequestedEvent(
    String eventId,
    String clientId,
    BigDecimal amount,
    int termMonths,
    String purpose
) {
    public LoanCommand toCommand() {
        return new LoanCommand(clientId, amount, termMonths, purpose);
    }
}
