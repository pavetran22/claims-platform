package com.example.claims.claim;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** One row in a claim's audit trail. Never updated after it is written. */
@Entity
@Table(name = "claim_events")
public class ClaimEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "claim_id", nullable = false)
    private UUID claimId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private ClaimStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private ClaimStatus toStatus;

    @Column(name = "actor_id", nullable = false)
    private String actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role", nullable = false)
    private Role actorRole;

    @Column(length = 4000)
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ClaimEvent() {
        // for JPA
    }

    public ClaimEvent(UUID claimId, ClaimStatus fromStatus, ClaimStatus toStatus,
                      String actorId, Role actorRole, String note) {
        this.claimId = claimId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actorId = actorId;
        this.actorRole = actorRole;
        this.note = note;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public UUID getClaimId() { return claimId; }
    public ClaimStatus getFromStatus() { return fromStatus; }
    public ClaimStatus getToStatus() { return toStatus; }
    public String getActorId() { return actorId; }
    public Role getActorRole() { return actorRole; }
    public String getNote() { return note; }
    public Instant getCreatedAt() { return createdAt; }
}
