-- V20261027154820__create_programme_offerings.sql
-- Created: 2026-10-27 15:48:20 +02:00
-- Purpose: create programme offerings
-- Never edit this file after it has been applied: add a new migration instead.

-- A crop programme offered in one season, with its Field Officer, the land each participant
-- farms and the number of places the input stock allows.
CREATE TABLE programme_offerings (
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    programme_id             BIGINT        NOT NULL,
    season_id                BIGINT        NOT NULL,
    field_officer_id         BIGINT        NOT NULL,
    hectares_per_participant NUMERIC(6, 2) NOT NULL,
    max_participants         INTEGER       NOT NULL,
    -- How many places are taken. Step 5c changes it inside the same transaction as the enrolment,
    -- while holding a row lock on this offering.
    enrolled_count           INTEGER       NOT NULL DEFAULT 0,
    created_at               TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_offerings_programme     FOREIGN KEY (programme_id)     REFERENCES crop_programmes (id),
    CONSTRAINT fk_offerings_season        FOREIGN KEY (season_id)        REFERENCES seasons (id),
    CONSTRAINT fk_offerings_field_officer FOREIGN KEY (field_officer_id) REFERENCES app_users (id),
    CONSTRAINT chk_offerings_hectares     CHECK (hectares_per_participant > 0),
    CONSTRAINT chk_offerings_max          CHECK (max_participants > 0),
    -- Last line of defence: even a bug in Java can never store more participants than places.
    CONSTRAINT chk_offerings_capacity     CHECK (enrolled_count >= 0 AND enrolled_count <= max_participants),
    -- A programme is offered at most once per season.
    CONSTRAINT uq_offerings_programme_season UNIQUE (programme_id, season_id)
);
CREATE INDEX idx_offerings_season        ON programme_offerings (season_id);
CREATE INDEX idx_offerings_field_officer ON programme_offerings (field_officer_id);
CREATE TRIGGER trg_programme_offerings_updated_at BEFORE UPDATE ON programme_offerings
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
