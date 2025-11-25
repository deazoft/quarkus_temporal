package com.addi.loan.features.checkcredit;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.Random;

/**
 * CheckCreditActivitiesImpl - Mock implementation for demo
 *
 * In production, this would call real credit bureau APIs.
 * For demo purposes, returns mock data.
 */
@ApplicationScoped
public class CheckCreditActivitiesImpl implements CheckCreditActivities {

    private static final Logger LOG = Logger.getLogger(CheckCreditActivitiesImpl.class);
    private final Random random = new Random();

    @Override
    @WithSpan("activity.fetchCreditScore")
    public int fetchCreditScore(String clientId) {
        Span.current().setAttribute("client.id", clientId);

        LOG.infof("Fetching credit score for client: %s", clientId);

        // Mock credit score (in production, call credit bureau API)
        // Generate deterministic score based on clientId hash
        int hash = Math.abs(clientId.hashCode());
        int score = 500 + (hash % 350); // Range: 500-850

        LOG.infof("Credit score for client %s: %d", clientId, score);
        return score;
    }

    @Override
    @WithSpan("activity.fetchCreditHistory")
    public CreditHistory fetchCreditHistory(String clientId) {
        Span.current().setAttribute("client.id", clientId);

        LOG.infof("Fetching credit history for client: %s", clientId);

        // Mock credit history (in production, call credit bureau API)
        int hash = Math.abs(clientId.hashCode());
        boolean hasDelinquencies = (hash % 10) < 2; // 20% have delinquencies

        LOG.infof("Credit history for client %s: hasDelinquencies=%s", clientId, hasDelinquencies);
        return new CreditHistory(hasDelinquencies);
    }
}
