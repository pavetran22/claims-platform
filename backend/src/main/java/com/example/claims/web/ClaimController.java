package com.example.claims.web;

import com.example.claims.claim.Actor;
import com.example.claims.claim.ClaimEvent;
import com.example.claims.claim.ClaimService;
import com.example.claims.claim.Role;
import com.example.claims.claim.SubmitClaimRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST is used for everything a user does and waits on: they need an immediate answer
 * (the new claim, or "you can't do that"). Side effects such as notifications go via Kafka.
 */
@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final ClaimService service;

    public ClaimController(ClaimService service) {
        this.service = service;
    }

    // ----- claimant -----

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimView submit(@RequestHeader("X-User-Id") String userId, @RequestHeader("X-User-Role") Role role,
                            @Valid @RequestBody SubmitClaimRequest body) {
        return ClaimView.of(service.submit(new Actor(userId, role), body));
    }

    @GetMapping("/mine")
    public List<ClaimView> mine(@RequestHeader("X-User-Id") String userId, @RequestHeader("X-User-Role") Role role) {
        return service.myClaims(new Actor(userId, role)).stream().map(ClaimView::of).toList();
    }

    @PostMapping("/{id}/info")
    public ClaimView provideInfo(@RequestHeader("X-User-Id") String userId, @RequestHeader("X-User-Role") Role role,
                                 @PathVariable UUID id, @Valid @RequestBody Requests.Message body) {
        return ClaimView.of(service.provideInfo(new Actor(userId, role), id, body.message()));
    }

    // ----- shared -----

    @GetMapping("/{id}")
    public ClaimView get(@RequestHeader("X-User-Id") String userId, @RequestHeader("X-User-Role") Role role,
                         @PathVariable UUID id) {
        return ClaimView.of(service.get(new Actor(userId, role), id));
    }

    @GetMapping("/{id}/history")
    public List<ClaimEvent> history(@RequestHeader("X-User-Id") String userId, @RequestHeader("X-User-Role") Role role,
                                    @PathVariable UUID id) {
        return service.history(new Actor(userId, role), id);
    }

    // ----- claims officer -----

    @GetMapping("/queue")
    public List<ClaimView> queue(@RequestHeader("X-User-Id") String userId, @RequestHeader("X-User-Role") Role role) {
        return service.unassignedQueue(new Actor(userId, role)).stream().map(ClaimView::of).toList();
    }

    @GetMapping("/assigned")
    public List<ClaimView> assigned(@RequestHeader("X-User-Id") String userId, @RequestHeader("X-User-Role") Role role) {
        return service.assignedToMe(new Actor(userId, role)).stream().map(ClaimView::of).toList();
    }

    @PostMapping("/{id}/pick-up")
    public ClaimView pickUp(@RequestHeader("X-User-Id") String userId, @RequestHeader("X-User-Role") Role role,
                            @PathVariable UUID id) {
        return ClaimView.of(service.pickUp(new Actor(userId, role), id));
    }

    @PostMapping("/{id}/request-info")
    public ClaimView requestInfo(@RequestHeader("X-User-Id") String userId, @RequestHeader("X-User-Role") Role role,
                                 @PathVariable UUID id, @Valid @RequestBody Requests.Message body) {
        return ClaimView.of(service.requestInfo(new Actor(userId, role), id, body.message()));
    }

    @PostMapping("/{id}/approve")
    public ClaimView approve(@RequestHeader("X-User-Id") String userId, @RequestHeader("X-User-Role") Role role,
                             @PathVariable UUID id, @Valid @RequestBody Requests.Amount body) {
        return ClaimView.of(service.approve(new Actor(userId, role), id, body.amount(), body.note()));
    }

    @PostMapping("/{id}/reject")
    public ClaimView reject(@RequestHeader("X-User-Id") String userId, @RequestHeader("X-User-Role") Role role,
                            @PathVariable UUID id, @Valid @RequestBody Requests.Message body) {
        return ClaimView.of(service.reject(new Actor(userId, role), id, body.message()));
    }

    @PostMapping("/{id}/settle")
    public ClaimView settle(@RequestHeader("X-User-Id") String userId, @RequestHeader("X-User-Role") Role role,
                            @PathVariable UUID id, @Valid @RequestBody Requests.Amount body) {
        return ClaimView.of(service.settle(new Actor(userId, role), id, body.amount()));
    }
}
