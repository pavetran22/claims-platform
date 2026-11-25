package com.example.claims.notify;

import com.example.claims.claim.ClaimStatusChanged;
import com.example.claims.claim.Role;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Kafka consumer: turns claim events into messages for the claimant.
 * Idempotent: Kafka can deliver an event more than once, so we skip event IDs we've already handled.
 */
@Component
public class ClaimNotificationListener {

    private final NotificationRepository notifications;
    private final ObjectMapper json;

    public ClaimNotificationListener(NotificationRepository notifications, ObjectMapper json) {
        this.notifications = notifications;
        this.json = json;
    }

    @KafkaListener(topics = ClaimEventPublisher.TOPIC, groupId = "notifications")
    @Transactional
    public void onClaimEvent(String payload) {
        ClaimStatusChanged event = json.readValue(payload, ClaimStatusChanged.class);
        if (event.actorRole() == Role.CLAIMANT) {
            return; // the claimant did this themselves; nothing to tell them
        }
        if (notifications.existsByEventId(event.eventId())) {
            return;
        }
        notifications.save(new Notification(event.eventId(), event.claimantId(), event.claimId(), message(event)));
    }

    static String message(ClaimStatusChanged e) {
        String ref = e.reference();
        return switch (e.to()) {
            case UNDER_REVIEW -> "Your claim " + ref + " is now being reviewed.";
            case INFO_REQUESTED -> "We need more information for claim " + ref + ": " + e.note();
            case APPROVED -> "Your claim " + ref + " has been approved. " + e.note();
            case REJECTED -> "Your claim " + ref + " has been rejected. Reason: " + e.note();
            case SETTLED -> "Your claim " + ref + " has been settled. " + e.note();
            case SUBMITTED -> "Your claim " + ref + " was received.";
        };
    }
}
