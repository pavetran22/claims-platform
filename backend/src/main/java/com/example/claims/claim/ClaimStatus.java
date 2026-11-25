package com.example.claims.claim;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Claim lifecycle. The allowed moves live in one place so the rules are easy to read and test.
 *
 * SUBMITTED -> UNDER_REVIEW -> APPROVED -> SETTLED
 *                  |  ^    \
 *                  v  |     -> REJECTED
 *             INFO_REQUESTED
 */
public enum ClaimStatus {
    SUBMITTED, UNDER_REVIEW, INFO_REQUESTED, APPROVED, REJECTED, SETTLED;

    private static final Map<ClaimStatus, Set<ClaimStatus>> ALLOWED = Map.of(
            SUBMITTED, EnumSet.of(UNDER_REVIEW),
            UNDER_REVIEW, EnumSet.of(INFO_REQUESTED, APPROVED, REJECTED),
            INFO_REQUESTED, EnumSet.of(UNDER_REVIEW),
            APPROVED, EnumSet.of(SETTLED),
            REJECTED, EnumSet.noneOf(ClaimStatus.class),
            SETTLED, EnumSet.noneOf(ClaimStatus.class));

    public boolean canMoveTo(ClaimStatus next) {
        return ALLOWED.get(this).contains(next);
    }

    /** Still open: counts towards workload and exposure. */
    public boolean isOpen() {
        return this != REJECTED && this != SETTLED;
    }
}
