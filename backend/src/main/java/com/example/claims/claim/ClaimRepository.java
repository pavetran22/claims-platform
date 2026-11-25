package com.example.claims.claim;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ClaimRepository extends JpaRepository<Claim, UUID> {

    List<Claim> findByClaimantIdOrderByCreatedAtDesc(String claimantId);

    List<Claim> findByStatusOrderByCreatedAtAsc(ClaimStatus status);

    List<Claim> findByAssignedOfficerAndStatusInOrderByUpdatedAtDesc(String officerId, Collection<ClaimStatus> statuses);

    List<Claim> findByStatusIn(Collection<ClaimStatus> statuses);
}
