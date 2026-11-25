package com.example.claims.notify;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "recipient_id", nullable = false)
    private String recipientId;

    @Column(name = "claim_id", nullable = false)
    private UUID claimId;

    @Column(nullable = false, length = 4000)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Notification() {
        // for JPA
    }

    public Notification(UUID eventId, String recipientId, UUID claimId, String message) {
        this.eventId = eventId;
        this.recipientId = recipientId;
        this.claimId = claimId;
        this.message = message;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getRecipientId() { return recipientId; }
    public UUID getClaimId() { return claimId; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
}
