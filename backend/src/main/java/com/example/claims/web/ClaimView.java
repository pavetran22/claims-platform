package com.example.claims.web;

import com.example.claims.claim.Claim;
import com.example.claims.claim.ClaimStatus;
import com.example.claims.claim.Market;
import com.example.claims.claim.ProductType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** What the API returns for a claim. Keeps the JPA entity out of the JSON contract. */
public record ClaimView(UUID id, String reference, String claimantId, Market market, ProductType productType,
                        ClaimStatus status, LocalDate incidentDate, String description,
                        BigDecimal claimedAmount, BigDecimal approvedAmount, BigDecimal settledAmount,
                        String currency, String assignedOfficer, Instant createdAt, Instant updatedAt) {

    static ClaimView of(Claim c) {
        return new ClaimView(c.getId(), c.getReference(), c.getClaimantId(), c.getMarket(), c.getProductType(),
                c.getStatus(), c.getIncidentDate(), c.getDescription(),
                c.getClaimedAmount(), c.getReserveAmount(), c.getSettledAmount(),
                c.getCurrency(), c.getAssignedOfficer(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
