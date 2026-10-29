-- V20261029124727__create_collection_permits.sql
-- Created: 2026-10-29 12:47:27 +02:00
-- Purpose: create collection permits
-- Never edit this file after it has been applied: add a new migration instead.

CREATE TABLE collection_permits (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- Random, not a counter: a storekeeper can verify it, but nobody can guess other farmers' numbers.
    permit_number VARCHAR(20)  NOT NULL,
    enrolment_id  BIGINT       NOT NULL,
    farmer_id     BIGINT       NOT NULL,
    issued_by     BIGINT       NOT NULL,
    issued_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_permits_enrolment FOREIGN KEY (enrolment_id) REFERENCES enrolments (id),
    CONSTRAINT fk_permits_farmer    FOREIGN KEY (farmer_id)    REFERENCES farmers (id),
    CONSTRAINT fk_permits_issued_by FOREIGN KEY (issued_by)    REFERENCES app_users (id),
    CONSTRAINT uq_permits_number    UNIQUE (permit_number),
    -- One permit per enrolment: asking again returns the same permit.
    CONSTRAINT uq_permits_enrolment UNIQUE (enrolment_id),
    CONSTRAINT chk_permits_number_format CHECK (permit_number ~ '^CP-[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}$')
);
CREATE INDEX idx_permits_farmer ON collection_permits (farmer_id);
-- Every attempt. Deliberately WITHOUT foreign keys, like the audit log: an attempt with a wrong or
-- someone else's enrolment id must be recorded too, and refusals are written in their own transaction.
CREATE TABLE permit_attempts (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    enrolment_id BIGINT,
    farmer_id    BIGINT,
    user_id      BIGINT       NOT NULL,
    username     VARCHAR(100) NOT NULL,
    outcome      VARCHAR(16)  NOT NULL,
    reason       VARCHAR(500),
    permit_id    BIGINT,  attempted_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_permit_attempts_outcome CHECK (outcome IN ('ISSUED', 'ALREADY_ISSUED', 'REFUSED')),
    CONSTRAINT chk_permit_attempts_details CHECK (
           (outcome = 'REFUSED' AND reason IS NOT NULL AND permit_id IS NULL)
        OR (outcome IN ('ISSUED', 'ALREADY_ISSUED') AND reason IS NULL AND permit_id IS NOT NULL))
);
CREATE INDEX idx_permit_attempts_farmer ON permit_attempts (farmer_id, attempted_at);
CREATE INDEX idx_permit_attempts_enrolment ON permit_attempts (enrolment_id);
-- Permits and attempts are permanent records.
CREATE FUNCTION forbid_permit_record_changes() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Rows of % are permanent records and can never be changed or deleted.', TG_TABLE_NAME
        USING ERRCODE = '23514';
END $$;
CREATE TRIGGER trg_permits_permanent BEFORE UPDATE OR DELETE ON collection_permits
    FOR EACH ROW EXECUTE FUNCTION forbid_permit_record_changes();
CREATE TRIGGER trg_permit_attempts_permanent BEFORE UPDATE OR DELETE ON permit_attempts
    FOR EACH ROW EXECUTE FUNCTION forbid_permit_record_changes();
