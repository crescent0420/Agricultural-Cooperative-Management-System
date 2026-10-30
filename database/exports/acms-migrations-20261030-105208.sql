-- ACMS: all migration SQL commands, in the order Flyway applies them
-- Generated:  2026-10-30 10:52:08 +02:00
-- Database:   acms_db on localhost:5432
-- Git commit: 1f6b678

-- ============================================================
-- V20261013165443__baseline.sql
--   file last changed: 2026-10-27 12:35:19
--   applied 2026-10-13 16:54:43 by acms_user
-- ============================================================

SELECT 1;

-- ============================================================
-- V20261015102911__create_districts.sql
--   file last changed: 2026-10-27 12:35:19
--   applied 2026-10-15 10:29:11 by acms_user
-- ============================================================
CREATE TABLE districts (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_districts_name_lower ON districts (lower(name));

-- ============================================================
-- V20261015112859__create_cooperatives.sql
--   file last changed: 2026-10-27 12:35:19
--   applied 2026-10-15 11:28:59 by acms_user
-- ============================================================

CREATE TABLE cooperatives (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    district_id BIGINT       NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_cooperatives_district
        FOREIGN KEY (district_id)
        REFERENCES districts (id)
);

CREATE UNIQUE INDEX uq_cooperatives_district_name
    ON cooperatives (district_id, lower(name));


-- ============================================================
-- V20261015143224__create_trainings.sql
--   file last changed: 2026-10-27 12:35:19
--   applied 2026-10-15 14:32:24 by acms_user
-- ============================================================

CREATE TABLE trainings (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(150) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_trainings_name_lower
    ON trainings (lower(name));


-- ============================================================
-- V20261015143225__create_crop_programmes.sql
--   file last changed: 2026-10-27 12:35:19
--   applied 2026-10-15 14:32:24 by acms_user
-- ============================================================

CREATE TABLE crop_programmes (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    district_id BIGINT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_crop_programmes_district
        FOREIGN KEY (district_id) REFERENCES districts (id)
);

CREATE UNIQUE INDEX uq_crop_programmes_district_name
    ON crop_programmes (district_id, lower(name));

-- Each row links a programme to one required training.
CREATE TABLE programme_prerequisites (
    programme_id BIGINT NOT NULL,
    training_id  BIGINT NOT NULL,

    PRIMARY KEY (programme_id, training_id),

    CONSTRAINT fk_prereq_programme
        FOREIGN KEY (programme_id) REFERENCES crop_programmes (id),

    CONSTRAINT fk_prereq_training
        FOREIGN KEY (training_id) REFERENCES trainings (id)
);

CREATE INDEX idx_prereq_training
    ON programme_prerequisites (training_id);


-- ============================================================
-- V20261015153126__create_farmers.sql
--   file last changed: 2026-10-27 12:35:19
--   applied 2026-10-15 15:31:26 by acms_user
-- ============================================================

CREATE SEQUENCE farmer_membership_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE farmers (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    membership_number VARCHAR(20)  NOT NULL,
    national_id       VARCHAR(16)  NOT NULL,
    full_name         VARCHAR(150) NOT NULL,
    phone_number      VARCHAR(20),
    cooperative_id    BIGINT       NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_farmers_membership_number UNIQUE (membership_number),
    CONSTRAINT uq_farmers_national_id UNIQUE (national_id),
    CONSTRAINT fk_farmers_cooperative
        FOREIGN KEY (cooperative_id) REFERENCES cooperatives (id),
    CONSTRAINT ck_farmers_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'WITHDRAWN', 'RETIRED')),
    CONSTRAINT ck_farmers_national_id
        CHECK (national_id ~ '^[0-9]{16}$')
);

CREATE INDEX idx_farmers_cooperative ON farmers (cooperative_id);


-- ============================================================
-- V20261019115619__add_farmer_version.sql
--   file last changed: 2026-10-27 12:35:19
--   applied 2026-10-19 11:56:19 by acms_user
-- ============================================================
ALTER TABLE farmers ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- ============================================================
-- V20261019161117__create_farmer_trainings.sql
--   file last changed: 2026-10-27 12:35:19
--   applied 2026-10-19 16:11:17 by acms_user
-- ============================================================
CREATE TABLE farmer_trainings (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    farmer_id    BIGINT      NOT NULL,
    training_id  BIGINT      NOT NULL,
    completed_on DATE        NOT NULL,
    recorded_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_farmer_trainings_farmer
        FOREIGN KEY (farmer_id) REFERENCES farmers (id),
    CONSTRAINT fk_farmer_trainings_training
        FOREIGN KEY (training_id) REFERENCES trainings (id),
    CONSTRAINT uq_farmer_trainings UNIQUE (farmer_id, training_id)
);


-- ============================================================
-- V20261020161332__create_app_users.sql
--   file last changed: 2026-10-27 12:35:19
--   applied 2026-10-20 16:13:32 by acms_user
-- ============================================================
CREATE TABLE app_users (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_app_users_username UNIQUE (username),
    CONSTRAINT chk_app_users_role CHECK (
        role IN (
            'FARMER',
            'FIELD_OFFICER',
            'DISTRICT_AGRONOMIST',
            'UNION_REGISTRAR',
            'FINANCE_OFFICER',
            'SYSTEM_ADMIN'
        )
    )
);


-- ============================================================
-- V20261021103955__user_administration_and_audit_log.sql
--   file last changed: 2026-10-27 12:35:19
--   applied 2026-10-21 10:39:55 by acms_user
-- ============================================================
-- Track when the password last changed so older JWTs can be rejected.
ALTER TABLE app_users
    ADD COLUMN password_changed_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- Add role-specific account scope.
ALTER TABLE app_users
    ADD COLUMN district_id BIGINT,
    ADD COLUMN farmer_id BIGINT;

ALTER TABLE app_users
    ADD CONSTRAINT fk_app_users_district
        FOREIGN KEY (district_id) REFERENCES districts (id),
    ADD CONSTRAINT fk_app_users_farmer
        FOREIGN KEY (farmer_id) REFERENCES farmers (id);

-- Enforce valid combinations of role and scope.
ALTER TABLE app_users
    ADD CONSTRAINT chk_app_users_role_scope
    CHECK (
        (
            role IN ('FIELD_OFFICER', 'DISTRICT_AGRONOMIST')
            AND district_id IS NOT NULL
            AND farmer_id IS NULL
        )
        OR (
            role = 'FARMER'
            AND district_id IS NULL
            AND farmer_id IS NOT NULL
        )
        OR (
            role IN ('UNION_REGISTRAR', 'FINANCE_OFFICER', 'SYSTEM_ADMIN')
            AND district_id IS NULL
            AND farmer_id IS NULL
        )
    );

-- Each farmer can have at most one login account.
CREATE UNIQUE INDEX uq_app_users_farmer_id
    ON app_users (farmer_id)
    WHERE farmer_id IS NOT NULL;

-- Record security and administration events.
CREATE TABLE audit_log (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    event_type VARCHAR(50) NOT NULL,
    success BOOLEAN NOT NULL,
    actor_user_id BIGINT,
    actor_username VARCHAR(100),
    target_type VARCHAR(50),
    target_id BIGINT,
    details VARCHAR(500),
    ip_address VARCHAR(45)
);

CREATE INDEX idx_audit_log_occurred_at
    ON audit_log (occurred_at DESC);

CREATE INDEX idx_audit_log_event_type
    ON audit_log (event_type);

-- Prevent updates and deletions, including accidental SQL changes.
CREATE FUNCTION prevent_audit_log_change()
RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION
        'audit_log is append-only: rows cannot be updated or deleted';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_log_append_only
    BEFORE UPDATE OR DELETE ON audit_log
    FOR EACH ROW
    EXECUTE FUNCTION prevent_audit_log_change();


-- ============================================================
-- V20261021143051__add_user_token_version.sql
--   file last changed: 2026-10-27 12:35:19
--   applied 2026-10-21 14:30:51 by acms_user
-- ============================================================

ALTER TABLE app_users
ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0;


-- ============================================================
-- V20261022112242__farmer_training_recorded_by.sql
--   file last changed: 2026-10-27 12:35:19
--   applied 2026-10-22 11:22:42 by acms_user
-- ============================================================
-- Track which user recorded each farmer training completion.
-- Keep this nullable so existing historical records remain valid.
ALTER TABLE farmer_trainings
    ADD COLUMN recorded_by BIGINT;

ALTER TABLE farmer_trainings
    ADD CONSTRAINT fk_farmer_trainings_recorded_by
        FOREIGN KEY (recorded_by) REFERENCES app_users (id);

CREATE INDEX idx_farmer_trainings_recorded_by
    ON farmer_trainings (recorded_by);

-- ============================================================
-- V20261024194735__updated_at_timestamps.sql
--   file last changed: 2026-10-27 12:35:19
--   applied 2026-10-24 19:47:35 by acms_user
-- ============================================================
-- Every UPDATE on these tables stamps updated_at automatically, whoever runs it.
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

ALTER TABLE districts       ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE cooperatives    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE trainings       ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE crop_programmes ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE app_users       ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE farmers         ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

DROP TRIGGER IF EXISTS trg_districts_updated_at       ON districts;
DROP TRIGGER IF EXISTS trg_cooperatives_updated_at    ON cooperatives;
DROP TRIGGER IF EXISTS trg_trainings_updated_at       ON trainings;
DROP TRIGGER IF EXISTS trg_crop_programmes_updated_at ON crop_programmes;
DROP TRIGGER IF EXISTS trg_app_users_updated_at       ON app_users;
DROP TRIGGER IF EXISTS trg_farmers_updated_at         ON farmers;

CREATE TRIGGER trg_districts_updated_at       BEFORE UPDATE ON districts       FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_cooperatives_updated_at    BEFORE UPDATE ON cooperatives    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_trainings_updated_at       BEFORE UPDATE ON trainings       FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_crop_programmes_updated_at BEFORE UPDATE ON crop_programmes FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_app_users_updated_at       BEFORE UPDATE ON app_users       FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_farmers_updated_at         BEFORE UPDATE ON farmers         FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ============================================================
-- V20261026103926__registration_and_email_verification.sql
--   file last changed: 2026-10-27 12:35:19
--   applied 2026-10-26 10:39:26 by acms_user
-- ============================================================
-- Part D: self-registration, email verification and one-time passwords.

-- 1. New account columns.
ALTER TABLE app_users
    ADD COLUMN email                VARCHAR(254),
    ADD COLUMN email_verified       BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

-- Accounts created before this step keep working: treat them as already verified.
UPDATE app_users SET email_verified = TRUE;

-- Emails are stored in lower case and each one may belong to only one account.
ALTER TABLE app_users
    ADD CONSTRAINT chk_app_users_email_lower CHECK (email = lower(email)),
    ADD CONSTRAINT uq_app_users_email UNIQUE (email);

-- 2. A FARMER account may now exist before the administrator links it to a farmer record.
ALTER TABLE app_users DROP CONSTRAINT chk_app_users_role_scope;
ALTER TABLE app_users ADD CONSTRAINT chk_app_users_role_scope CHECK (
       (role IN ('FIELD_OFFICER', 'DISTRICT_AGRONOMIST') AND district_id IS NOT NULL AND farmer_id IS NULL)
    OR (role = 'FARMER' AND district_id IS NULL)
    OR (role IN ('UNION_REGISTRAR', 'FINANCE_OFFICER', 'SYSTEM_ADMIN') AND district_id IS NULL AND farmer_id IS NULL)
);

-- 3. One-time email verification links. Only a SHA-256 hash of each link token is stored,
--    so a copy of the database cannot be used to verify someone else's account.
CREATE TABLE email_verification_tokens (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_email_verification_tokens_user FOREIGN KEY (user_id) REFERENCES app_users (id),
    CONSTRAINT uq_email_verification_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_email_verification_tokens_user ON email_verification_tokens (user_id);


-- ============================================================
-- V20261026151129__create_seasons_and_enrolment_windows.sql
--   file last changed: 2026-10-27 12:35:19
--   applied 2026-10-26 15:11:29 by acms_user
-- ============================================================
-- Step 5a: seasons and the enrolment windows the Union Registrar opens and closes.

CREATE TABLE seasons (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name              VARCHAR(100)  NOT NULL,
    starts_on         DATE          NOT NULL,
    ends_on           DATE          NOT NULL,
    max_land_hectares NUMERIC(8, 2) NOT NULL,   -- most land one farmer may enrol in this season
    distribution_date DATE          NOT NULL,   -- collection permits are issued before this day
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT chk_seasons_dates        CHECK (ends_on > starts_on),
    CONSTRAINT chk_seasons_distribution CHECK (distribution_date BETWEEN starts_on AND ends_on),
    CONSTRAINT chk_seasons_max_land     CHECK (max_land_hectares > 0)
);

CREATE UNIQUE INDEX uq_seasons_name_lower ON seasons (lower(name));

CREATE TRIGGER trg_seasons_updated_at BEFORE UPDATE ON seasons
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Every window ever opened is kept as history: closing a window changes its status, never deletes it.
CREATE TABLE enrolment_windows (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    season_id  BIGINT      NOT NULL,
    status     VARCHAR(10) NOT NULL,
    opened_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    opened_by  BIGINT      NOT NULL,
    closed_at  TIMESTAMPTZ,
    closed_by  BIGINT,
    CONSTRAINT fk_enrolment_windows_season    FOREIGN KEY (season_id) REFERENCES seasons (id),
    CONSTRAINT fk_enrolment_windows_opened_by FOREIGN KEY (opened_by) REFERENCES app_users (id),
    CONSTRAINT fk_enrolment_windows_closed_by FOREIGN KEY (closed_by) REFERENCES app_users (id),
    CONSTRAINT chk_enrolment_windows_status   CHECK (status IN ('OPEN', 'CLOSED')),
    -- An OPEN window has no closing details; a CLOSED one always records when and by whom.
    CONSTRAINT chk_enrolment_windows_closing CHECK (
           (status = 'OPEN'   AND closed_at IS NULL     AND closed_by IS NULL)
        OR (status = 'CLOSED' AND closed_at IS NOT NULL AND closed_by IS NOT NULL AND closed_at >= opened_at))
);

-- At most ONE open window per season, enforced by the database even if two requests race.
CREATE UNIQUE INDEX uq_enrolment_windows_one_open_per_season
    ON enrolment_windows (season_id) WHERE status = 'OPEN';

CREATE INDEX idx_enrolment_windows_season ON enrolment_windows (season_id);


-- ============================================================
-- V20261027120000__create_provinces_and_seed_rwanda_districts.sql
--   file last changed: 2026-10-27 12:35:19
--   applied 2026-10-27 11:54:28 by acms_user
-- ============================================================
-- V20261027120000__create_provinces_and_seed_rwanda_districts.sql
-- Created: 2026-10-27 12:00:00 +02:00
-- Purpose: Rwanda's provinces and its 30 districts are created automatically when the application
--          starts (Flyway runs this once), so nobody has to type them in.
--          Source: the administrative structure in force since 2006: Kigali City and 4 provinces, 30 districts.

CREATE TABLE provinces (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_provinces_name_lower ON provinces (lower(name));

CREATE TRIGGER trg_provinces_updated_at BEFORE UPDATE ON provinces
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

INSERT INTO provinces (name) VALUES
    ('Kigali City'),
    ('Northern Province'),
    ('Southern Province'),
    ('Eastern Province'),
    ('Western Province');

-- Each district belongs to one province. Districts created by hand before this migration that are not
-- official Rwandan districts keep province_id = NULL, so no existing data is lost.
ALTER TABLE districts
    ADD COLUMN province_id BIGINT,
    ADD CONSTRAINT fk_districts_province FOREIGN KEY (province_id) REFERENCES provinces (id);

CREATE INDEX idx_districts_province ON districts (province_id);

CREATE TEMPORARY TABLE official_districts (province VARCHAR(100), district VARCHAR(100)) ON COMMIT DROP;

INSERT INTO official_districts (province, district) VALUES
    ('Kigali City',       'Gasabo'),
    ('Kigali City',       'Kicukiro'),
    ('Kigali City',       'Nyarugenge'),
    ('Northern Province', 'Burera'),
    ('Northern Province', 'Gakenke'),
    ('Northern Province', 'Gicumbi'),
    ('Northern Province', 'Musanze'),
    ('Northern Province', 'Rulindo'),
    ('Southern Province', 'Gisagara'),
    ('Southern Province', 'Huye'),
    ('Southern Province', 'Kamonyi'),
    ('Southern Province', 'Muhanga'),
    ('Southern Province', 'Nyamagabe'),
    ('Southern Province', 'Nyanza'),
    ('Southern Province', 'Nyaruguru'),
    ('Southern Province', 'Ruhango'),
    ('Eastern Province',  'Bugesera'),
    ('Eastern Province',  'Gatsibo'),
    ('Eastern Province',  'Kayonza'),
    ('Eastern Province',  'Kirehe'),
    ('Eastern Province',  'Ngoma'),
    ('Eastern Province',  'Nyagatare'),
    ('Eastern Province',  'Rwamagana'),
    ('Western Province',  'Karongi'),
    ('Western Province',  'Ngororero'),
    ('Western Province',  'Nyabihu'),
    ('Western Province',  'Nyamasheke'),
    ('Western Province',  'Rubavu'),
    ('Western Province',  'Rusizi'),
    ('Western Province',  'Rutsiro');

-- Districts that already exist (for example "Huye" typed in earlier) keep their id, so cooperatives,
-- programmes, farmers and officers linked to them are untouched; they only get their province.
UPDATE districts d
SET province_id = p.id
FROM official_districts o
JOIN provinces p ON p.name = o.province
WHERE lower(d.name) = lower(o.district);

-- The missing official districts are added.
INSERT INTO districts (name, province_id)
SELECT o.district, p.id
FROM official_districts o
JOIN provinces p ON p.name = o.province
WHERE NOT EXISTS (SELECT 1 FROM districts d WHERE lower(d.name) = lower(o.district));


-- ============================================================
-- V20261027154820__create_programme_offerings.sql
--   file last changed: 2026-10-27 16:25:51
--   applied 2026-10-27 16:17:20 by acms_user
-- ============================================================
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


-- ============================================================
-- V20261027164239__create_enrolments.sql
--   file last changed: 2026-10-28 09:19:46
--   applied 2026-10-28 09:12:03 by acms_user
-- ============================================================
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


-- ============================================================
-- V20261028095405__create_contributions_and_payments.sql
--   file last changed: 2026-10-28 16:33:02
--   applied 2026-10-28 10:09:25 by acms_user
-- ============================================================
-- V20261028095405__create_contributions_and_payments.sql
-- Created: 2026-10-28 09:54:05 +02:00
-- Purpose: create contributions and payments
-- Never edit this file after it has been applied: add a new migration instead.

-- Input contributions (seed, fertiliser...) each farmer owes per season, the payments against them,
-- and every change of an amount owed. Financial history is permanent: the triggers below refuse
-- to delete it or to rewrite it, even for someone with direct database access through the application.

CREATE TABLE contribution_obligations (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    farmer_id  BIGINT         NOT NULL,
    season_id  BIGINT         NOT NULL,
    amount_due NUMERIC(12, 2) NOT NULL,
    created_by BIGINT         NOT NULL,
    created_at TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT fk_obligations_farmer     FOREIGN KEY (farmer_id)  REFERENCES farmers (id),
    CONSTRAINT fk_obligations_season     FOREIGN KEY (season_id)  REFERENCES seasons (id),
    CONSTRAINT fk_obligations_created_by FOREIGN KEY (created_by) REFERENCES app_users (id),
    CONSTRAINT chk_obligations_amount    CHECK (amount_due > 0),
    -- One contribution per farmer per season.
    CONSTRAINT uq_obligations_farmer_season UNIQUE (farmer_id, season_id)
);

CREATE INDEX idx_obligations_season ON contribution_obligations (season_id);

-- Every change of amount_due, with the old and new value, the reason, who and when.
CREATE TABLE contribution_adjustments (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    obligation_id BIGINT         NOT NULL,
    old_amount    NUMERIC(12, 2) NOT NULL,
    new_amount    NUMERIC(12, 2) NOT NULL,
    reason        VARCHAR(500)   NOT NULL,
    adjusted_by   BIGINT         NOT NULL,
    adjusted_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT fk_adjustments_obligation  FOREIGN KEY (obligation_id) REFERENCES contribution_obligations (id),
    CONSTRAINT fk_adjustments_adjusted_by FOREIGN KEY (adjusted_by)   REFERENCES app_users (id),
    CONSTRAINT chk_adjustments_amounts    CHECK (new_amount > 0 AND new_amount <> old_amount)
);

CREATE INDEX idx_adjustments_obligation ON contribution_adjustments (obligation_id);

CREATE TABLE payments (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    obligation_id         BIGINT         NOT NULL,
    -- Chosen by the client for each payment request. Sending the same request again with the same
    -- reference never creates a second payment: the unique constraint guarantees it.
    transaction_reference VARCHAR(64)    NOT NULL,
    amount                NUMERIC(12, 2) NOT NULL,
    method                VARCHAR(20)    NOT NULL,
    status                VARCHAR(12)    NOT NULL DEFAULT 'PENDING',
    -- The mobile money or bank transaction id received with the confirmation.
    provider_reference    VARCHAR(100),
    requested_by          BIGINT         NOT NULL,
    requested_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    confirmed_by          BIGINT,
    confirmed_at          TIMESTAMPTZ,
    failed_by             BIGINT,
    failed_at             TIMESTAMPTZ,
    failure_reason        VARCHAR(500),
    created_at            TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT fk_payments_obligation   FOREIGN KEY (obligation_id) REFERENCES contribution_obligations (id),
    CONSTRAINT fk_payments_requested_by FOREIGN KEY (requested_by)  REFERENCES app_users (id),
    CONSTRAINT fk_payments_confirmed_by FOREIGN KEY (confirmed_by)  REFERENCES app_users (id),
    CONSTRAINT fk_payments_failed_by    FOREIGN KEY (failed_by)     REFERENCES app_users (id),
    CONSTRAINT uq_payments_transaction_reference UNIQUE (transaction_reference),
    CONSTRAINT chk_payments_amount  CHECK (amount > 0),
    CONSTRAINT chk_payments_method  CHECK (method IN ('MOBILE_MONEY', 'BANK_TRANSFER')),
    CONSTRAINT chk_payments_status  CHECK (status IN ('PENDING', 'CONFIRMED', 'FAILED')),
    -- Each status carries exactly the details that belong to it.
    CONSTRAINT chk_payments_status_details CHECK (
           (status = 'PENDING'   AND confirmed_at IS NULL AND confirmed_by IS NULL AND provider_reference IS NULL
                                 AND failed_at IS NULL AND failed_by IS NULL AND failure_reason IS NULL)
        OR (status = 'CONFIRMED' AND confirmed_at IS NOT NULL AND confirmed_by IS NOT NULL AND provider_reference IS NOT NULL
                                 AND failed_at IS NULL AND failed_by IS NULL AND failure_reason IS NULL)
        OR (status = 'FAILED'    AND failed_at IS NOT NULL AND failed_by IS NOT NULL AND failure_reason IS NOT NULL
                                 AND confirmed_at IS NULL AND confirmed_by IS NULL AND provider_reference IS NULL))
);

-- One provider transaction can confirm only one payment.
CREATE UNIQUE INDEX uq_payments_provider_reference ON payments (provider_reference) WHERE provider_reference IS NOT NULL;
CREATE INDEX idx_payments_obligation ON payments (obligation_id, status);

-- ---------- permanent financial history ----------

-- ERRCODE 23514 (check_violation) is an integrity error that Spring always recognises, so the API answers 409.
CREATE FUNCTION forbid_financial_delete() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Rows of % are financial history and can never be deleted.', TG_TABLE_NAME
        USING ERRCODE = '23514';
END $$;

CREATE FUNCTION forbid_financial_update() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Rows of % are financial history and can never be changed.', TG_TABLE_NAME
        USING ERRCODE = '23514';
END $$;

-- A payment's request details never change. Only a PENDING payment may move on, once, to CONFIRMED or
-- FAILED; after that the row is frozen for good.
CREATE FUNCTION guard_payment_update() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.obligation_id <> OLD.obligation_id
       OR NEW.transaction_reference <> OLD.transaction_reference
       OR NEW.amount <> OLD.amount
       OR NEW.method <> OLD.method
       OR NEW.requested_by <> OLD.requested_by
       OR NEW.requested_at <> OLD.requested_at
       OR NEW.created_at <> OLD.created_at THEN
        RAISE EXCEPTION 'The amount, reference, method and contribution of a payment can never change.'
            USING ERRCODE = '23514';
    END IF;
    IF OLD.status <> 'PENDING' THEN
        RAISE EXCEPTION 'Payment % is % and can no longer change.', OLD.id, OLD.status
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END $$;

-- A contribution always stays with the same farmer and season (its amount changes only through an adjustment).
CREATE FUNCTION guard_obligation_update() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.farmer_id <> OLD.farmer_id OR NEW.season_id <> OLD.season_id
       OR NEW.created_by <> OLD.created_by OR NEW.created_at <> OLD.created_at THEN
        RAISE EXCEPTION 'The farmer and season of a contribution can never change.'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_obligations_no_delete BEFORE DELETE ON contribution_obligations
    FOR EACH ROW EXECUTE FUNCTION forbid_financial_delete();
CREATE TRIGGER trg_obligations_guard BEFORE UPDATE ON contribution_obligations
    FOR EACH ROW EXECUTE FUNCTION guard_obligation_update();
CREATE TRIGGER trg_obligations_updated_at BEFORE UPDATE ON contribution_obligations
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_adjustments_no_delete BEFORE DELETE ON contribution_adjustments
    FOR EACH ROW EXECUTE FUNCTION forbid_financial_delete();
CREATE TRIGGER trg_adjustments_no_update BEFORE UPDATE ON contribution_adjustments
    FOR EACH ROW EXECUTE FUNCTION forbid_financial_update();

CREATE TRIGGER trg_payments_no_delete BEFORE DELETE ON payments
    FOR EACH ROW EXECUTE FUNCTION forbid_financial_delete();
CREATE TRIGGER trg_payments_guard BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION guard_payment_update();
CREATE TRIGGER trg_payments_updated_at BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ============================================================
-- V20261028162848__create_inspections_and_results.sql
--   file last changed: 2026-10-29 12:42:16
--   applied 2026-10-28 14:44:31 by acms_user
-- ============================================================
-- V20261028162848__create_inspections_and_results.sql
-- Created: 2026-10-28 16:28:48 +02:00
-- Purpose: create inspections and results
-- Never edit this file after it has been applied: add a new migration instead.

-- Field inspections of programme offerings, the farmers' results (Draft -> Submitted -> Published),
-- authorised corrections of published results, and the configurable grading scale.

-- ---------- grading scale (the Union Registrar can replace it through the API) ----------
CREATE TABLE grading_bands (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    grade       VARCHAR(5)    NOT NULL,
    min_score   NUMERIC(5, 2) NOT NULL,   -- a final score >= min_score gets this grade (highest band wins)
    description VARCHAR(100),
    CONSTRAINT chk_grading_bands_min CHECK (min_score >= 0 AND min_score <= 100),
    CONSTRAINT uq_grading_bands_min UNIQUE (min_score)
);
CREATE UNIQUE INDEX uq_grading_bands_grade_lower ON grading_bands (lower(grade));

INSERT INTO grading_bands (grade, min_score, description) VALUES
    ('A', 80, 'Excellent'),
    ('B', 70, 'Very good'),
    ('C', 60, 'Good'),
    ('D', 50, 'Fair'),
    ('E',  0, 'Below standard');

-- ---------- inspections of an offering ----------
CREATE TABLE inspections (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    offering_id BIGINT        NOT NULL,
    name        VARCHAR(100)  NOT NULL,
    max_score   NUMERIC(6, 2) NOT NULL,
    weight      NUMERIC(5, 2) NOT NULL,   -- percent of the final score; an offering's weights must total 100
    created_by  BIGINT        NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_inspections_offering   FOREIGN KEY (offering_id) REFERENCES programme_offerings (id),
    CONSTRAINT fk_inspections_created_by FOREIGN KEY (created_by)  REFERENCES app_users (id),
    CONSTRAINT chk_inspections_max_score CHECK (max_score > 0),
    CONSTRAINT chk_inspections_weight    CHECK (weight > 0 AND weight <= 100)
);
CREATE UNIQUE INDEX uq_inspections_offering_name ON inspections (offering_id, lower(name));

CREATE TRIGGER trg_inspections_updated_at BEFORE UPDATE ON inspections
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------- one result per enrolment ----------
CREATE TABLE inspection_results (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    enrolment_id BIGINT        NOT NULL,
    status       VARCHAR(10)   NOT NULL DEFAULT 'DRAFT',
    final_score  NUMERIC(5, 2),
    grade        VARCHAR(5),
    created_by   BIGINT        NOT NULL,
    submitted_by BIGINT,
    submitted_at TIMESTAMPTZ,
    published_by BIGINT,                  -- the District Agronomist who approved
    published_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_results_enrolment    FOREIGN KEY (enrolment_id) REFERENCES enrolments (id),
    CONSTRAINT fk_results_created_by   FOREIGN KEY (created_by)   REFERENCES app_users (id),
    CONSTRAINT fk_results_submitted_by FOREIGN KEY (submitted_by) REFERENCES app_users (id),
    CONSTRAINT fk_results_published_by FOREIGN KEY (published_by) REFERENCES app_users (id),
    CONSTRAINT uq_results_enrolment    UNIQUE (enrolment_id),
    CONSTRAINT chk_results_status      CHECK (status IN ('DRAFT', 'SUBMITTED', 'PUBLISHED')),
    CONSTRAINT chk_results_final_score CHECK (final_score >= 0 AND final_score <= 100),
    -- Each stage carries exactly the details that belong to it.
    CONSTRAINT chk_results_stage_details CHECK (
           (status = 'DRAFT'     AND final_score IS NULL AND grade IS NULL
                                 AND submitted_at IS NULL AND submitted_by IS NULL
                                 AND published_at IS NULL AND published_by IS NULL)
        OR (status = 'SUBMITTED' AND final_score IS NOT NULL AND grade IS NOT NULL
                                 AND submitted_at IS NOT NULL AND submitted_by IS NOT NULL
                                 AND published_at IS NULL AND published_by IS NULL)
        OR (status = 'PUBLISHED' AND final_score IS NOT NULL AND grade IS NOT NULL
                                 AND submitted_at IS NOT NULL AND submitted_by IS NOT NULL
                                 AND published_at IS NOT NULL AND published_by IS NOT NULL))
);

CREATE TRIGGER trg_results_updated_at BEFORE UPDATE ON inspection_results
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------- the scores of a result, one per inspection: these are the ORIGINAL values ----------
CREATE TABLE inspection_scores (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    result_id     BIGINT        NOT NULL,
    inspection_id BIGINT        NOT NULL,
    score         NUMERIC(6, 2) NOT NULL,
    entered_by    BIGINT        NOT NULL,
    entered_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_scores_result     FOREIGN KEY (result_id)     REFERENCES inspection_results (id),
    CONSTRAINT fk_scores_inspection FOREIGN KEY (inspection_id) REFERENCES inspections (id),
    CONSTRAINT fk_scores_entered_by FOREIGN KEY (entered_by)    REFERENCES app_users (id),
    CONSTRAINT chk_scores_not_negative CHECK (score >= 0),
    CONSTRAINT uq_scores_result_inspection UNIQUE (result_id, inspection_id)
);
CREATE INDEX idx_scores_inspection ON inspection_scores (inspection_id);

-- ---------- authorised corrections of published results (append-only history) ----------
CREATE TABLE result_corrections (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    result_id             BIGINT        NOT NULL,
    inspection_id         BIGINT        NOT NULL,
    original_score        NUMERIC(6, 2) NOT NULL,
    corrected_score       NUMERIC(6, 2) NOT NULL,
    original_final_score  NUMERIC(5, 2) NOT NULL,
    corrected_final_score NUMERIC(5, 2) NOT NULL,
    original_grade        VARCHAR(5)    NOT NULL,
    corrected_grade       VARCHAR(5)    NOT NULL,
    reason                VARCHAR(500)  NOT NULL,
    corrected_by          BIGINT        NOT NULL,
    corrected_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_corrections_result       FOREIGN KEY (result_id)     REFERENCES inspection_results (id),
    CONSTRAINT fk_corrections_inspection   FOREIGN KEY (inspection_id) REFERENCES inspections (id),
    CONSTRAINT fk_corrections_corrected_by FOREIGN KEY (corrected_by)  REFERENCES app_users (id),
    CONSTRAINT chk_corrections_scores CHECK (corrected_score >= 0 AND corrected_score <> original_score)
);
CREATE INDEX idx_corrections_result ON result_corrections (result_id);

-- ---------- rules the database enforces itself ----------

-- Scores can be entered, changed or removed only while their result is a DRAFT.
-- After that, the only way to change a value is a correction (a new row in result_corrections).
CREATE FUNCTION guard_inspection_scores() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    result_status VARCHAR(10);
BEGIN
    SELECT status INTO result_status FROM inspection_results
     WHERE id = CASE WHEN TG_OP = 'DELETE' THEN OLD.result_id ELSE NEW.result_id END;
    IF result_status <> 'DRAFT' THEN
        RAISE EXCEPTION 'Scores can be changed only while the result is a DRAFT (it is %); use a correction.',
            result_status USING ERRCODE = '23514';
    END IF;
    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END $$;

CREATE TRIGGER trg_scores_guard BEFORE INSERT OR UPDATE OR DELETE ON inspection_scores
    FOR EACH ROW EXECUTE FUNCTION guard_inspection_scores();

-- Allowed stage moves: DRAFT -> SUBMITTED -> PUBLISHED, and SUBMITTED -> DRAFT (returned for rework).
-- A PUBLISHED result never goes back, and its score and grade change only to the values of its
-- latest correction.
CREATE FUNCTION guard_inspection_results() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'Inspection results are never deleted.' USING ERRCODE = '23514';
    END IF;
    IF NEW.enrolment_id <> OLD.enrolment_id THEN
        RAISE EXCEPTION 'A result always belongs to the same enrolment.' USING ERRCODE = '23514';
    END IF;
    IF NOT ((OLD.status = NEW.status)
         OR (OLD.status = 'DRAFT'     AND NEW.status = 'SUBMITTED')
         OR (OLD.status = 'SUBMITTED' AND NEW.status IN ('DRAFT', 'PUBLISHED'))) THEN
        RAISE EXCEPTION 'A result cannot move from % to %.', OLD.status, NEW.status USING ERRCODE = '23514';
    END IF;
    IF OLD.status = 'PUBLISHED' THEN
        IF NEW.published_at <> OLD.published_at OR NEW.published_by <> OLD.published_by
           OR NEW.submitted_at <> OLD.submitted_at OR NEW.submitted_by <> OLD.submitted_by THEN
            RAISE EXCEPTION 'The approval of a published result can never change.' USING ERRCODE = '23514';
        END IF;
        IF (NEW.final_score <> OLD.final_score OR NEW.grade <> OLD.grade)
           AND NOT EXISTS (SELECT 1 FROM result_corrections c
                            WHERE c.id = (SELECT max(id) FROM result_corrections WHERE result_id = NEW.id)
                              AND c.corrected_final_score = NEW.final_score
                              AND c.corrected_grade = NEW.grade) THEN
            RAISE EXCEPTION 'A published result changes only through a recorded correction.' USING ERRCODE = '23514';
        END IF;
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_results_guard BEFORE UPDATE OR DELETE ON inspection_results
    FOR EACH ROW EXECUTE FUNCTION guard_inspection_results();

CREATE FUNCTION forbid_correction_changes() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Corrections are permanent history and can never be changed or deleted.'
        USING ERRCODE = '23514';
END $$;

CREATE TRIGGER trg_corrections_permanent BEFORE UPDATE OR DELETE ON result_corrections
    FOR EACH ROW EXECUTE FUNCTION forbid_correction_changes();


-- ============================================================
-- V20261029124727__create_collection_permits.sql
--   file last changed: 2026-10-29 14:29:19
--   applied 2026-10-29 12:20:51 by acms_user
-- ============================================================
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


