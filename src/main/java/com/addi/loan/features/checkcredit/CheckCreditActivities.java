package com.addi.loan.features.checkcredit;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * CheckCreditActivities - I/O operations for credit checking
 *
 * Activities for fetching credit data from external services.
 */
@ActivityInterface
public interface CheckCreditActivities {

    @ActivityMethod
    int fetchCreditScore(String clientId);

    @ActivityMethod
    CreditHistory fetchCreditHistory(String clientId);

    /**
     * CreditHistory - Simple record for credit history data
     */
    record CreditHistory(boolean hasDelinquencies) {}
}
