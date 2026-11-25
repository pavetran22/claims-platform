package com.example.claims;

import com.example.claims.claim.Actor;
import com.example.claims.claim.BusinessRuleException;
import com.example.claims.claim.Claim;
import com.example.claims.claim.ClaimService;
import com.example.claims.claim.ClaimStatus;
import com.example.claims.claim.ForbiddenException;
import com.example.claims.claim.InvalidTransitionException;
import com.example.claims.claim.Market;
import com.example.claims.claim.NotFoundException;
import com.example.claims.claim.ProductType;
import com.example.claims.claim.Role;
import com.example.claims.claim.SubmitClaimRequest;
import com.example.claims.dashboard.DashboardService;
import com.example.claims.notify.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Runs against the real Postgres and Kafka from docker-compose, in a separate schema and topic (test profile). */
@SpringBootTest
@ActiveProfiles("test")
class ClaimLifecycleTest {

    @Autowired ClaimService claims;
    @Autowired DashboardService dashboard;
    @Autowired NotificationRepository notifications;

    // unique IDs per run so the tests don't depend on what's already in the database
    final String run = UUID.randomUUID().toString().substring(0, 6);
    final Actor alice = new Actor("alice-" + run, Role.CLAIMANT);
    final Actor bob = new Actor("bob-" + run, Role.CLAIMANT);
    final Actor officer = new Actor("officer-" + run, Role.OFFICER);
    final Actor otherOfficer = new Actor("officer2-" + run, Role.OFFICER);
    final Actor manager = new Actor("manager-" + run, Role.MANAGER);

    private Claim newClaim(Actor who) {
        return claims.submit(who, new SubmitClaimRequest(Market.SG, ProductType.MOTOR,
                LocalDate.now().minusDays(2), "Side mirror damaged in car park", new BigDecimal("1200.00")));
    }

    @Test
    void fullLifecycleWithInfoRequest() throws InterruptedException {
        UUID id = newClaim(alice).getId();

        claims.pickUp(officer, id);
        claims.requestInfo(officer, id, "Please upload a photo of the damage");
        claims.provideInfo(alice, id, "Photo attached");
        claims.approve(officer, id, new BigDecimal("1000.00"), null);
        Claim settled = claims.settle(officer, id, new BigDecimal("1000.00"));

        assertEquals(ClaimStatus.SETTLED, settled.getStatus());
        assertEquals(6, claims.history(alice, id).size()); // submitted + 5 changes

        // Kafka consumer should turn officer actions into notifications for alice
        long deadline = System.currentTimeMillis() + 15_000;
        while (notifications.findByRecipientIdOrderByCreatedAtDesc(alice.id()).size() < 4
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(250);
        }
        assertEquals(4, notifications.findByRecipientIdOrderByCreatedAtDesc(alice.id()).size(),
                "pick-up, info request, approval and settlement each notify the claimant");
    }

    @Test
    void claimantCannotSeeSomeoneElsesClaim() {
        UUID id = newClaim(alice).getId();
        assertThrows(NotFoundException.class, () -> claims.get(bob, id));
    }

    @Test
    void onlyTheAssignedOfficerCanDecide() {
        UUID id = newClaim(alice).getId();
        claims.pickUp(officer, id);
        assertThrows(ForbiddenException.class,
                () -> claims.approve(otherOfficer, id, new BigDecimal("100.00"), null));
    }

    @Test
    void cannotApproveMoreThanClaimedOrSettleBeforeApproval() {
        UUID id = newClaim(alice).getId();
        claims.pickUp(officer, id);
        assertThrows(InvalidTransitionException.class, () -> claims.settle(officer, id, new BigDecimal("10.00")));
        assertThrows(BusinessRuleException.class,
                () -> claims.approve(officer, id, new BigDecimal("5000.00"), null));
    }

    @Test
    void claimantsCannotSeeDashboards() {
        newClaim(alice);
        assertThrows(ForbiddenException.class, () -> dashboard.workload(alice));
        assertThrows(ForbiddenException.class, () -> dashboard.exposure(officer));
        assertFalse(dashboard.exposure(manager).isEmpty());
    }
}
