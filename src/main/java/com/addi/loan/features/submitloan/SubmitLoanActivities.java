package com.addi.loan.features.submitloan;

import com.addi.loan.features.submitloan.dto.LoanCommand;
import com.addi.loan.features.submitloan.dto.LoanDecision;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Activities Interface - Clean Interface per ADR145
 *
 * Activities are "dumb I/O ports" - thin wrappers around single I/O calls.
 * NO business logic here!
 */
@ActivityInterface
public interface SubmitLoanActivities {

    @ActivityMethod
    String saveLoanApplication(LoanCommand command);

    @ActivityMethod
    void updateLoanDecision(String loanId, String status, String reason);

    @ActivityMethod
    void publishLoanDecisionEvent(String loanId, String clientId, LoanDecision decision);

    @ActivityMethod
    LoanApplicationEntity findLoanById(String loanId);
}
