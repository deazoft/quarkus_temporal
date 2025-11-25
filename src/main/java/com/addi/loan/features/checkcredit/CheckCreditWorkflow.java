package com.addi.loan.features.checkcredit;

import com.addi.loan.features.checkcredit.dto.CreditCheckResult;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * CheckCreditWorkflow - Child workflow for credit checking
 *
 * This workflow encapsulates the credit check logic.
 * Can be called as a child workflow or standalone.
 */
@WorkflowInterface
public interface CheckCreditWorkflow {

    @WorkflowMethod
    CreditCheckResult checkCredit(String clientId);
}
