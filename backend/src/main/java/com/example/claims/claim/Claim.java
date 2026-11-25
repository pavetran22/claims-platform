package com.example.claims.claim;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "claims")
public class Claim {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(name = "claimant_id", nullable = false)
    private String claimantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Market market;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false)
    private ProductType productType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus status;

    @Column(name = "incident_date", nullable = false)
    private LocalDate incidentDate;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(name = "claimed_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal claimedAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "reserve_amount", precision = 14, scale = 2)
    private BigDecimal reserveAmount;

    @Column(name = "settled_amount", precision = 14, scale = 2)
    private BigDecimal settledAmount;

    @Column(name = "assigned_officer")
    private String assignedOfficer;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected Claim() {
        // for JPA
    }

    /** A new claim always starts as SUBMITTED, in its market's currency. */
    public static Claim submit(String claimantId, Market market, ProductType productType,
                               LocalDate incidentDate, String description, BigDecimal claimedAmount) {
        Claim c = new Claim();
        c.id = UUID.randomUUID();
        c.reference = "CLM-" + c.id.toString().substring(0, 8).toUpperCase(Locale.ROOT);
        c.claimantId = claimantId;
        c.market = market;
        c.productType = productType;
        c.incidentDate = incidentDate;
        c.description = description;
        c.claimedAmount = claimedAmount;
        c.currency = market.currency();
        c.status = ClaimStatus.SUBMITTED;
        c.createdAt = Instant.now();
        c.updatedAt = c.createdAt;
        return c;
    }

    /** Moves the claim to a new status, or throws if the lifecycle doesn't allow it. Returns the old status. */
    public ClaimStatus transitionTo(ClaimStatus next) {
        if (!status.canMoveTo(next)) {
            throw new InvalidTransitionException(status, next);
        }
        ClaimStatus from = status;
        status = next;
        updatedAt = Instant.now();
        return from;
    }

    public void assignTo(String officerId) {
        this.assignedOfficer = officerId;
        this.updatedAt = Instant.now();
    }

    public void setReserve(BigDecimal reserveAmount) {
        this.reserveAmount = reserveAmount;
        this.updatedAt = Instant.now();
    }

    public void recordSettlement(BigDecimal settledAmount) {
        this.settledAmount = settledAmount;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getReference() { return reference; }
    public String getClaimantId() { return claimantId; }
    public Market getMarket() { return market; }
    public ProductType getProductType() { return productType; }
    public ClaimStatus getStatus() { return status; }
    public LocalDate getIncidentDate() { return incidentDate; }
    public String getDescription() { return description; }
    public BigDecimal getClaimedAmount() { return claimedAmount; }
    public String getCurrency() { return currency; }
    public BigDecimal getReserveAmount() { return reserveAmount; }
    public BigDecimal getSettledAmount() { return settledAmount; }
    public String getAssignedOfficer() { return assignedOfficer; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
