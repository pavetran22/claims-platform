-- Claims and their status history.
-- Amounts are NUMERIC (never floating point) and always stored with their currency.

CREATE TABLE claims (
    id               UUID          PRIMARY KEY,
    reference        VARCHAR(20)   NOT NULL UNIQUE,
    claimant_id      VARCHAR(100)  NOT NULL,
    market           VARCHAR(2)    NOT NULL,
    product_type     VARCHAR(20)   NOT NULL,
    status           VARCHAR(20)   NOT NULL,
    incident_date    DATE          NOT NULL,
    description      VARCHAR(4000) NOT NULL,
    claimed_amount   NUMERIC(14,2) NOT NULL CHECK (claimed_amount > 0),
    currency         VARCHAR(3)    NOT NULL,
    reserve_amount   NUMERIC(14,2),            -- officer's estimate of what will be paid (drives exposure)
    settled_amount   NUMERIC(14,2),
    assigned_officer VARCHAR(100),
    created_at       TIMESTAMPTZ   NOT NULL,
    updated_at       TIMESTAMPTZ   NOT NULL,
    version          BIGINT        NOT NULL    -- optimistic locking: two officers can't overwrite each other
);

CREATE INDEX idx_claims_status   ON claims (status);
CREATE INDEX idx_claims_claimant ON claims (claimant_id);
CREATE INDEX idx_claims_officer  ON claims (assigned_officer);

-- Append-only audit trail: every status change, who made it, and why.
CREATE TABLE claim_events (
    id          BIGSERIAL     PRIMARY KEY,
    claim_id    UUID          NOT NULL REFERENCES claims (id),
    from_status VARCHAR(20),
    to_status   VARCHAR(20)   NOT NULL,
    actor_id    VARCHAR(100)  NOT NULL,
    actor_role  VARCHAR(20)   NOT NULL,
    note        VARCHAR(4000),
    created_at  TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_claim_events_claim ON claim_events (claim_id);
