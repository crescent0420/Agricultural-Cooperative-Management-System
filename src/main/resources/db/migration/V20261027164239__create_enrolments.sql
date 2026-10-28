-- V20261027164239__create_enrolments.sql
-- Created: 2026-10-27 16:42:39 +02:00
-- Purpose: create enrolments
-- Never edit this file after it has been applied: add a new migration instead.

-- Farmers enrolled in programme offerings. A cancelled enrolment is kept as history, never deleted.

CREATE TABLE enrolments (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    offering_id         BIGINT        NOT NULL,
    farmer_id           BIGINT        NOT NULL,
    status              VARCHAR(12)   NOT NULL DEFAULT 'ENROLLED',
    -- The offering's land per participant at the moment of enrolment (used for the season land limit).
    hectares            NUMERIC(6, 2) NOT NULL,
    enrolled_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    enrolled_by         BIGINT        NOT NULL,
    cancelled_at        TIMESTAMPTZ,
    cancelled_by        BIGINT,
    cancellation_reason VARCHAR(500),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_enrolments_offering     FOREIGN KEY (offering_id)  REFERENCES programme_offerings (id),
    CONSTRAINT fk_enrolments_farmer       FOREIGN KEY (farmer_id)    REFERENCES farmers (id),
    CONSTRAINT fk_enrolments_enrolled_by  FOREIGN KEY (enrolled_by)  REFERENCES app_users (id),
    CONSTRAINT fk_enrolments_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES app_users (id),
    CONSTRAINT chk_enrolments_status      CHECK (status IN ('ENROLLED', 'CANCELLED')),
    CONSTRAINT chk_enrolments_hectares    CHECK (hectares > 0),
    -- An active enrolment has no cancellation details; a cancelled one always records when and by whom.
    CONSTRAINT chk_enrolments_cancellation CHECK (
           (status = 'ENROLLED'  AND cancelled_at IS NULL AND cancelled_by IS NULL AND cancellation_reason IS NULL)
        OR (status = 'CANCELLED' AND cancelled_at IS NOT NULL AND cancelled_by IS NOT NULL))
);

-- A farmer can be enrolled in the same offering only once at a time (re-enrolling after a cancellation is allowed).
CREATE UNIQUE INDEX uq_enrolments_one_active_per_farmer
    ON enrolments (offering_id, farmer_id) WHERE status = 'ENROLLED';

CREATE INDEX idx_enrolments_farmer   ON enrolments (farmer_id, status);
CREATE INDEX idx_enrolments_offering ON enrolments (offering_id, status);

CREATE TRIGGER trg_enrolments_updated_at BEFORE UPDATE ON enrolments
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
