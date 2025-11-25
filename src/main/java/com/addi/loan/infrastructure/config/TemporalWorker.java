package com.addi.loan.infrastructure.config;

import com.addi.loan.features.checkcredit.CheckCreditActivitiesImpl;
import com.addi.loan.features.checkcredit.CheckCreditWorkflowImpl;
import com.addi.loan.features.submitloan.SubmitLoanActivitiesImpl;
import com.addi.loan.features.submitloan.SubmitLoanWorkflowImpl;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.temporal.client.WorkflowClient;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Temporal Worker - Registers workflows and activities
 *
 * This component starts a Temporal worker on application startup and
 * registers all workflows and activities for the loan-processing task queue.
 */
@ApplicationScoped
public class TemporalWorker {

    private static final Logger LOG = Logger.getLogger(TemporalWorker.class);
    private static final String TASK_QUEUE = "loan-processing";

    @Inject
    WorkflowClient workflowClient;

    @Inject
    WorkerFactory workerFactory;

    @Inject
    SubmitLoanActivitiesImpl submitLoanActivities;

    @Inject
    CheckCreditActivitiesImpl checkCreditActivities;

    void onStart(@Observes StartupEvent ev) {
        LOG.info("Starting Temporal worker for task queue: " + TASK_QUEUE);

        // Create worker for loan-processing task queue
        Worker worker = workerFactory.newWorker(TASK_QUEUE);

        // Register workflow implementations
        worker.registerWorkflowImplementationTypes(
            SubmitLoanWorkflowImpl.class,
            CheckCreditWorkflowImpl.class
        );

        // Register activity implementations
        worker.registerActivitiesImplementations(submitLoanActivities, checkCreditActivities);

        // Start the worker factory
        workerFactory.start();

        LOG.info("Temporal worker started successfully");
    }

    void onStop(@Observes ShutdownEvent ev) {
        LOG.info("Shutting down Temporal worker");
        workerFactory.shutdown();
    }
}
