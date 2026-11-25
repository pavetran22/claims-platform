package com.example.claims.claim;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.example.claims.claim.ClaimStatus.*;
import static org.junit.jupiter.api.Assertions.*;

class ClaimStatusTest {

    @Test
    void happyPathIsAllowed() {
        assertTrue(SUBMITTED.canMoveTo(UNDER_REVIEW));
        assertTrue(UNDER_REVIEW.canMoveTo(APPROVED));
        assertTrue(APPROVED.canMoveTo(SETTLED));
    }

    @Test
    void infoRequestLoopsBackToReview() {
        assertTrue(UNDER_REVIEW.canMoveTo(INFO_REQUESTED));
        assertTrue(INFO_REQUESTED.canMoveTo(UNDER_REVIEW));
        assertFalse(INFO_REQUESTED.canMoveTo(APPROVED));
    }

    @Test
    void cannotSkipReview() {
        assertFalse(SUBMITTED.canMoveTo(APPROVED));
        assertFalse(SUBMITTED.canMoveTo(SETTLED));
        assertFalse(UNDER_REVIEW.canMoveTo(SETTLED));
    }

    @Test
    void settledAndRejectedAreFinal() {
        for (ClaimStatus s : ClaimStatus.values()) {
            assertFalse(SETTLED.canMoveTo(s));
            assertFalse(REJECTED.canMoveTo(s));
        }
        assertFalse(SETTLED.isOpen());
        assertFalse(REJECTED.isOpen());
    }

    @Test
    void claimRefusesIllegalMoveAndKeepsItsStatus() {
        Claim claim = Claim.submit("alice", Market.MY, ProductType.MOTOR,
                LocalDate.now(), "Rear-ended at traffic light", new BigDecimal("4500.00"));

        assertEquals(SUBMITTED, claim.getStatus());
        assertEquals("MYR", claim.getCurrency());
        assertThrows(InvalidTransitionException.class, () -> claim.transitionTo(SETTLED));
        assertEquals(SUBMITTED, claim.getStatus());
    }
}
