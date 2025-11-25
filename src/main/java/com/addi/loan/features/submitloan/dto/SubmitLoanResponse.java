package com.addi.loan.features.submitloan.dto;

/**
 * SubmitLoanResponse DTO - REST API output
 *
 * Response from loan submission endpoint.
 */
public record SubmitLoanResponse(
    String loanId,
    String status,
    String message
) {}
