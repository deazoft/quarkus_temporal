package com.addi.loan.infrastructure.kafka;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

/**
 * Generic Kafka Event Publisher - part of platform-infra
 *
 * Publishes events to Kafka for Data team contracts per ADR145 constraints.
 * This is a "dumb I/O port" used by Activities.
 */
@ApplicationScoped
public class EventPublisher {

    private static final Logger LOG = Logger.getLogger(EventPublisher.class);

    @Inject
    @Channel("loan-decisions")
    MutinyEmitter<Object> loanDecisionsEmitter;

    @WithSpan("kafka.publish")
    public Uni<Void> publish(String topic, String key, Object event) {
        Span.current().setAttribute("kafka.topic", topic);
        Span.current().setAttribute("kafka.key", key);

        LOG.debugf("Publishing event to topic=%s, key=%s", topic, key);

        OutgoingKafkaRecordMetadata<?> metadata = OutgoingKafkaRecordMetadata.builder()
            .withKey(key)
            .build();

        Message<Object> message = Message.of(event)
            .addMetadata(metadata);

        return loanDecisionsEmitter.sendMessage(message);
    }
}
