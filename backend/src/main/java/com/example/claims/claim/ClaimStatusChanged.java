package com.example.claims.claim;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event raised on every status change (including submission, where from is null).
 * Published to Kafka only after the database transaction commits.
 */
public record ClaimStatusChanged(
        UUID eventId,
        UUID claimId,
        String reference,
        String claimantId,
        ClaimStatus from,
        ClaimStatus to,
        String actorId,
        Role actorRole,
        String note,
        Instant occurredAt) {
}
