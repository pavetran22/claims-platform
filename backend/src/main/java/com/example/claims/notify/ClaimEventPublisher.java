package com.example.claims.notify;

import com.example.claims.claim.ClaimStatusChanged;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

/**
 * Sends claim events to Kafka AFTER the database commit, so consumers never hear about
 * a change that was rolled back. Keyed by claim ID so one claim's events stay in order.
 */
@Configuration
public class ClaimEventPublisher {

    /** Overridable so tests use their own topic and never mix with the running app. */
    public static final String TOPIC = "${claims.topic:claim-events}";
    private static final Logger log = LoggerFactory.getLogger(ClaimEventPublisher.class);

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;
    private final String topic;

    public ClaimEventPublisher(KafkaTemplate<String, String> kafka, ObjectMapper json, @Value(TOPIC) String topic) {
        this.kafka = kafka;
        this.json = json;
        this.topic = topic;
    }

    @Bean
    NewTopic claimEventsTopic() {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(ClaimStatusChanged event) {
        kafka.send(topic, event.claimId().toString(), json.writeValueAsString(event))
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Failed to publish {} for claim {}", event.to(), event.reference(), error);
                    }
                });
    }
}
