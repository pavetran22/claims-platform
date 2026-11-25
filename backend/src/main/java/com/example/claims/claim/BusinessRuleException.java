package com.example.claims.claim;

/** The request is well-formed but breaks a claims rule (for example, paying more than was approved). */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
