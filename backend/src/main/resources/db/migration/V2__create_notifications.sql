-- Filled by the Kafka consumer. event_id is UNIQUE so a redelivered event never creates a duplicate.
CREATE TABLE notifications (
    id           BIGSERIAL     PRIMARY KEY,
    event_id     UUID          NOT NULL UNIQUE,
    recipient_id VARCHAR(100)  NOT NULL,
    claim_id     UUID          NOT NULL,
    message      VARCHAR(4000) NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_notifications_recipient ON notifications (recipient_id);
