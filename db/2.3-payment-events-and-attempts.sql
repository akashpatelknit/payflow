-- Phase 2.3: payment_events and payment_attempts.
-- Needed only for databases created before this phase if you want the constraints/indexes exactly as
-- mapped by the entities (ddl-auto=update will otherwise create the tables on next startup, and this
-- script is a no-op for anything that already exists). Safe to re-run; it never drops or deletes data.

CREATE TABLE IF NOT EXISTS payment_events (
    id              uuid         PRIMARY KEY,
    payment_id      uuid         NOT NULL,
    event_type      varchar(30)  NOT NULL,
    previous_status varchar(20),
    new_status      varchar(20)  NOT NULL,
    details         varchar(500),
    created_at      timestamptz  NOT NULL,
    CONSTRAINT fk_payment_events_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT payment_events_event_type_check CHECK (event_type IN ('PAYMENT_CREATED', 'STATUS_CHANGED')),
    CONSTRAINT payment_events_previous_status_check
        CHECK (previous_status IS NULL OR previous_status IN ('PENDING', 'PROCESSING', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT payment_events_new_status_check
        CHECK (new_status IN ('PENDING', 'PROCESSING', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT payment_events_created_has_no_previous_status
        CHECK (event_type <> 'PAYMENT_CREATED' OR previous_status IS NULL)
);
CREATE INDEX IF NOT EXISTS idx_payment_events_payment_created ON payment_events (payment_id, created_at);

CREATE TABLE IF NOT EXISTS payment_attempts (
    id             uuid        PRIMARY KEY,
    payment_id     uuid        NOT NULL,
    attempt_number integer     NOT NULL,
    status         varchar(20) NOT NULL,
    started_at     timestamptz NOT NULL,
    completed_at   timestamptz,
    failure_reason varchar(500),
    CONSTRAINT fk_payment_attempts_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT uk_payment_attempts_payment_number UNIQUE (payment_id, attempt_number),
    CONSTRAINT payment_attempts_status_check CHECK (status IN ('IN_PROGRESS', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT payment_attempts_number_positive CHECK (attempt_number > 0),
    CONSTRAINT payment_attempts_completion_consistent
        CHECK ((status = 'IN_PROGRESS' AND completed_at IS NULL)
            OR (status <> 'IN_PROGRESS' AND completed_at IS NOT NULL))
);

-- Optional backfill so pre-existing payments have a history. Each is guarded, so re-running is harmless.
-- 1) A creation event for every payment that has no events yet (timestamp = payment creation time).
INSERT INTO payment_events (id, payment_id, event_type, previous_status, new_status, details, created_at)
SELECT gen_random_uuid(), p.id, 'PAYMENT_CREATED', NULL, 'PENDING', 'Backfilled', p.created_at
FROM payments p
WHERE NOT EXISTS (SELECT 1 FROM payment_events e WHERE e.payment_id = p.id);

-- 2) An open attempt for payments currently PROCESSING, so their later completion updates an attempt.
INSERT INTO payment_attempts (id, payment_id, attempt_number, status, started_at)
SELECT gen_random_uuid(), p.id, 1, 'IN_PROGRESS', p.updated_at
FROM payments p
WHERE p.status = 'PROCESSING'
  AND NOT EXISTS (SELECT 1 FROM payment_attempts a WHERE a.payment_id = p.id);
