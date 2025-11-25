package com.addi.loan.infrastructure.config;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Optional;

/**
 * Temporal CDI Producers - Infrastructure Configuration
 *
 * This provides centralized Temporal client and worker configuration.
 * Note: With quarkus-temporal extension, this may be partially auto-configured.
 */
@ApplicationScoped
public class TemporalProducers {

    private static final Logger LOG = Logger.getLogger(TemporalProducers.class);

    @ConfigProperty(name = "quarkus.temporal.connection.target", defaultValue = "localhost:7233")
    String temporalAddress;

    @ConfigProperty(name = "quarkus.temporal.namespace", defaultValue = "default")
    String namespace;

    @Produces
    @ApplicationScoped
    public WorkflowServiceStubs workflowServiceStubs() {
        LOG.infof("Creating Temporal service stubs for address: %s", temporalAddress);

        return WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder()
                .setTarget(temporalAddress)
                .build()
        );
    }

    @Produces
    @ApplicationScoped
    public WorkflowClient workflowClient(WorkflowServiceStubs serviceStubs) {
        LOG.infof("Creating Temporal workflow client for namespace: %s", namespace);

        return WorkflowClient.newInstance(
            serviceStubs,
            WorkflowClientOptions.newBuilder()
                .setNamespace(namespace)
                .build()
        );
    }

    @Produces
    @ApplicationScoped
    public WorkerFactory workerFactory(WorkflowClient workflowClient) {
        LOG.info("Creating Temporal worker factory");
        return WorkerFactory.newInstance(workflowClient);
    }
}
