package com.example.claims.claim;

public class InvalidTransitionException extends RuntimeException {
    public InvalidTransitionException(ClaimStatus from, ClaimStatus to) {
        super("Cannot move a claim from " + from + " to " + to);
    }
}
