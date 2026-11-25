package com.example.claims.claim;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * All claim actions go through here. Each public method is one transaction:
 * check who is asking, apply the lifecycle rule, write the audit row, raise the event.
 */
@Service
@Transactional
public class ClaimService {

    static final Set<ClaimStatus> OPEN = EnumSet.of(
            ClaimStatus.SUBMITTED, ClaimStatus.UNDER_REVIEW, ClaimStatus.INFO_REQUESTED, ClaimStatus.APPROVED);

    private final ClaimRepository claims;
    private final ClaimEventRepository history;
    private final ApplicationEventPublisher events;

    public ClaimService(ClaimRepository claims, ClaimEventRepository history, ApplicationEventPublisher events) {
        this.claims = claims;
        this.history = history;
        this.events = events;
    }

    // ---------- claimant ----------

    public Claim submit(Actor actor, SubmitClaimRequest req) {
        requireRole(actor, Role.CLAIMANT);
        Claim claim = Claim.submit(actor.id(), req.market(), req.productType(),
                req.incidentDate(), req.description().trim(), req.claimedAmount());
        claims.save(claim);
        record(claim, null, actor, "Claim submitted");
        return claim;
    }

    @Transactional(readOnly = true)
    public List<Claim> myClaims(Actor actor) {
        requireRole(actor, Role.CLAIMANT);
        return claims.findByClaimantIdOrderByCreatedAtDesc(actor.id());
    }

    public Claim provideInfo(Actor actor, UUID claimId, String info) {
        requireRole(actor, Role.CLAIMANT);
        Claim claim = find(claimId);
        requireOwner(actor, claim);
        return move(claim, ClaimStatus.UNDER_REVIEW, actor, info);
    }

    // ---------- anyone allowed to see the claim ----------

    @Transactional(readOnly = true)
    public Claim get(Actor actor, UUID claimId) {
        Claim claim = find(claimId);
        requireCanView(actor, claim);
        return claim;
    }

    @Transactional(readOnly = true)
    public List<ClaimEvent> history(Actor actor, UUID claimId) {
        get(actor, claimId);
        return history.findByClaimIdOrderByCreatedAtAsc(claimId);
    }

    // ---------- claims officer ----------

    @Transactional(readOnly = true)
    public List<Claim> unassignedQueue(Actor actor) {
        requireRole(actor, Role.OFFICER, Role.MANAGER);
        return claims.findByStatusOrderByCreatedAtAsc(ClaimStatus.SUBMITTED);
    }

    @Transactional(readOnly = true)
    public List<Claim> assignedToMe(Actor actor) {
        requireRole(actor, Role.OFFICER);
        return claims.findByAssignedOfficerAndStatusInOrderByUpdatedAtDesc(actor.id(), OPEN);
    }

    public Claim pickUp(Actor actor, UUID claimId) {
        requireRole(actor, Role.OFFICER);
        Claim claim = find(claimId);
        ClaimStatus from = claim.transitionTo(ClaimStatus.UNDER_REVIEW); // only a SUBMITTED claim can be picked up
        claim.assignTo(actor.id());
        record(claim, from, actor, "Picked up by " + actor.id());
        return claim;
    }

    public Claim requestInfo(Actor actor, UUID claimId, String question) {
        Claim claim = findAssigned(actor, claimId);
        return move(claim, ClaimStatus.INFO_REQUESTED, actor, question);
    }

    public Claim approve(Actor actor, UUID claimId, BigDecimal approvedAmount, String note) {
        Claim claim = findAssigned(actor, claimId);
        if (approvedAmount.compareTo(claim.getClaimedAmount()) > 0) {
            throw new BusinessRuleException("Approved amount cannot exceed the claimed amount of "
                    + claim.getClaimedAmount() + " " + claim.getCurrency());
        }
        ClaimStatus from = claim.transitionTo(ClaimStatus.APPROVED);
        claim.setReserve(approvedAmount);
        record(claim, from, actor, note == null || note.isBlank()
                ? "Approved for " + approvedAmount + " " + claim.getCurrency() : note);
        return claim;
    }

    public Claim reject(Actor actor, UUID claimId, String reason) {
        Claim claim = findAssigned(actor, claimId);
        return move(claim, ClaimStatus.REJECTED, actor, reason);
    }

    public Claim settle(Actor actor, UUID claimId, BigDecimal paidAmount) {
        Claim claim = findAssigned(actor, claimId);
        if (claim.getReserveAmount() != null && paidAmount.compareTo(claim.getReserveAmount()) > 0) {
            throw new BusinessRuleException("Settlement cannot exceed the approved amount of "
                    + claim.getReserveAmount() + " " + claim.getCurrency());
        }
        ClaimStatus from = claim.transitionTo(ClaimStatus.SETTLED);
        claim.recordSettlement(paidAmount);
        record(claim, from, actor, "Settled for " + paidAmount + " " + claim.getCurrency());
        return claim;
    }

    // ---------- helpers ----------

    private Claim move(Claim claim, ClaimStatus next, Actor actor, String note) {
        ClaimStatus from = claim.transitionTo(next);
        record(claim, from, actor, note);
        return claim;
    }

    private void record(Claim claim, ClaimStatus from, Actor actor, String note) {
        history.save(new ClaimEvent(claim.getId(), from, claim.getStatus(), actor.id(), actor.role(), note));
        events.publishEvent(new ClaimStatusChanged(UUID.randomUUID(), claim.getId(), claim.getReference(),
                claim.getClaimantId(), from, claim.getStatus(), actor.id(), actor.role(), note, Instant.now()));
    }

    private Claim find(UUID claimId) {
        return claims.findById(claimId).orElseThrow(() -> new NotFoundException("Claim " + claimId + " not found"));
    }

    private Claim findAssigned(Actor actor, UUID claimId) {
        requireRole(actor, Role.OFFICER);
        Claim claim = find(claimId);
        if (!actor.id().equals(claim.getAssignedOfficer())) {
            throw new ForbiddenException("Claim " + claim.getReference() + " is not assigned to you");
        }
        return claim;
    }

    private static void requireRole(Actor actor, Role... allowed) {
        for (Role r : allowed) {
            if (actor.role() == r) return;
        }
        throw new ForbiddenException("Role " + actor.role() + " cannot do this");
    }

    private static void requireOwner(Actor actor, Claim claim) {
        if (!claim.getClaimantId().equals(actor.id())) {
            // 404, not 403, so a claimant can't probe which claim IDs exist
            throw new NotFoundException("Claim " + claim.getId() + " not found");
        }
    }

    private static void requireCanView(Actor actor, Claim claim) {
        if (actor.role() == Role.CLAIMANT) requireOwner(actor, claim);
    }
}
