package com.addi.loan.features.submitloan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * LoanApplicationEntity - Database entity
 *
 * Represents a loan application in the database.
 * Used by the reactive repository for persistence.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationEntity {
    private String id;
    private String clientId;
    private BigDecimal amount;
    private Integer term;
    private String purpose;
    private String status;
    private String decisionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
