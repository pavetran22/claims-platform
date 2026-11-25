package com.example.claims.claim;

/** Who is making the request. Comes from headers for now (see README: no real auth in scope). */
public record Actor(String id, Role role) {
}
