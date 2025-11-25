package com.addi.loan.features.checkcredit;

import com.addi.loan.features.checkcredit.dto.CreditCheckResult;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * CheckCreditWorkflowImpl - Smart child workflow implementation
 *
 * Contains business logic for calculating credit limits based on score.
 * This is a child workflow called by SubmitLoanWorkflow.
 */
public class CheckCreditWorkflowImpl implements CheckCreditWorkflow {

    private final CheckCreditActivities activities = Workflow.newActivityStub(
        CheckCreditActivities.class,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .setRetryOptions(RetryOptions.newBuilder()
                .setMaximumAttempts(3)
                .build())
            .build()
    );

    @Override
    public CreditCheckResult checkCredit(String clientId) {
        // Fetch credit score from external service (Activity = I/O)
        int creditScore = activities.fetchCreditScore(clientId);

        // Fetch credit history (Activity = I/O)
        CheckCreditActivities.CreditHistory history = activities.fetchCreditHistory(clientId);

        // Calculate max loan amount based on credit (pure logic)
        BigDecimal maxLoanAmount = calculateMaxLoanAmount(creditScore, history);

        return new CreditCheckResult(creditScore, maxLoanAmount, history.hasDelinquencies());
    }

    /**
     * Pure business logic - deterministic calculation
     *
     * Calculates maximum loan amount based on credit score.
     */
    private BigDecimal calculateMaxLoanAmount(int score, CheckCreditActivities.CreditHistory history) {
        // Reduce limit if delinquencies exist
        BigDecimal baseLimit;

        if (score >= 750) {
            baseLimit = BigDecimal.valueOf(100000);
        } else if (score >= 700) {
            baseLimit = BigDecimal.valueOf(75000);
        } else if (score >= 650) {
            baseLimit = BigDecimal.valueOf(50000);
        } else if (score >= 600) {
            baseLimit = BigDecimal.valueOf(25000);
        } else {
            baseLimit = BigDecimal.valueOf(10000);
        }

        // Reduce by 50% if has delinquencies
        if (history.hasDelinquencies()) {
            baseLimit = baseLimit.multiply(BigDecimal.valueOf(0.5));
        }

        return baseLimit;
    }
}
