package com.addi.loan.features.submitloan.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * SubmitLoanRequest DTO - REST API input
 *
 * Validated input for loan submission endpoint.
 * Part of the "thin trigger" layer per ADR145.
 */
public record SubmitLoanRequest(
    @NotBlank(message = "Client ID is required")
    String clientId,

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1000", message = "Minimum loan amount is 1000")
    @DecimalMax(value = "500000", message = "Maximum loan amount is 500000")
    BigDecimal amount,

    @Min(value = 6, message = "Minimum term is 6 months")
    @Max(value = 60, message = "Maximum term is 60 months")
    int termMonths,

    @NotBlank(message = "Purpose is required")
    String purpose
) {
    public LoanCommand toCommand() {
        return new LoanCommand(clientId, amount, termMonths, purpose);
    }
}
