package com.example.claims.dashboard;

import com.example.claims.claim.Actor;
import com.example.claims.claim.Claim;
import com.example.claims.claim.ClaimRepository;
import com.example.claims.claim.ClaimStatus;
import com.example.claims.claim.ForbiddenException;
import com.example.claims.claim.Market;
import com.example.claims.claim.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Workload and exposure figures, computed from the claims table on request so they are always current.
 * Done in Java for clarity; at real volumes this becomes GROUP BY queries or a Kafka-fed read model.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final ClaimRepository claims;

    public DashboardService(ClaimRepository claims) {
        this.claims = claims;
    }

    public record OfficerStats(String officerId, long underReview, long infoRequested,
                               long awaitingSettlement, long open, long closed, Double avgDaysToClose) {
    }

    public record Workload(long unassigned, List<OfficerStats> officers) {
    }

    public record MarketExposure(Market market, String currency, long openClaims, BigDecimal exposure) {
    }

    public Workload workload(Actor actor) {
        requireStaff(actor);
        List<Claim> all = claims.findAll();
        long unassigned = all.stream().filter(c -> c.getStatus() == ClaimStatus.SUBMITTED).count();

        Map<String, List<Claim>> byOfficer = all.stream()
                .filter(c -> c.getAssignedOfficer() != null)
                .collect(Collectors.groupingBy(Claim::getAssignedOfficer, TreeMap::new, Collectors.toList()));

        List<OfficerStats> stats = new ArrayList<>();
        byOfficer.forEach((officer, list) -> {
            List<Claim> closed = list.stream().filter(c -> !c.getStatus().isOpen()).toList();
            Double avgDays = closed.isEmpty() ? null : closed.stream()
                    .mapToDouble(c -> Duration.between(c.getCreatedAt(), c.getUpdatedAt()).toHours() / 24.0)
                    .average().orElse(0);
            stats.add(new OfficerStats(officer,
                    count(list, ClaimStatus.UNDER_REVIEW),
                    count(list, ClaimStatus.INFO_REQUESTED),
                    count(list, ClaimStatus.APPROVED),
                    list.stream().filter(c -> c.getStatus().isOpen()).count(),
                    closed.size(),
                    avgDays));
        });
        return new Workload(unassigned, stats);
    }

    /**
     * Liability exposure of open claims, per market and in that market's currency.
     * Uses the approved amount once a claim is approved, otherwise the claimed amount (worst case).
     */
    public List<MarketExposure> exposure(Actor actor) {
        if (actor.role() != Role.MANAGER) {
            throw new ForbiddenException("Only managers can see exposure");
        }
        Map<Market, List<Claim>> open = claims.findAll().stream()
                .filter(c -> c.getStatus().isOpen())
                .collect(Collectors.groupingBy(Claim::getMarket));

        return open.entrySet().stream()
                .map(e -> new MarketExposure(e.getKey(), e.getKey().currency(), e.getValue().size(),
                        e.getValue().stream()
                                .map(c -> c.getReserveAmount() != null ? c.getReserveAmount() : c.getClaimedAmount())
                                .reduce(BigDecimal.ZERO, BigDecimal::add)))
                .sorted(Comparator.comparing(MarketExposure::market))
                .toList();
    }

    private static long count(List<Claim> list, ClaimStatus status) {
        return list.stream().filter(c -> c.getStatus() == status).count();
    }

    private static void requireStaff(Actor actor) {
        if (actor.role() == Role.CLAIMANT) {
            throw new ForbiddenException("Only claims staff can see workload");
        }
    }
}
