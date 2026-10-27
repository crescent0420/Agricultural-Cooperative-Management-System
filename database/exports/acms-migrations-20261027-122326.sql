-- ACMS: all migration SQL commands, in the order Flyway applies them
-- Generated:  2026-10-27 12:23:26 +02:00
-- Database:   acms_db on localhost:5432
-- Git commit: c6a6e9a

-- ============================================================
-- V15__create_seasons_and_enrolment_windows.sql
--   file last changed: 2026-10-27 12:07:05
--   NOT YET APPLIED to this database
-- ============================================================
-- Seasons and the enrolment windows the Union Registrar opens and closes.

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
-- V20261013165443__baseline.sql
--   file last changed: 2026-10-13 16:33:33
--   applied 2026-10-13 16:54:43 by acms_user
-- ============================================================

SELECT 1;

-- ============================================================
-- V20261015102911__create_districts.sql
--   file last changed: 2026-10-15 10:59:25
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
--   file last changed: 2026-10-26 09:18:20
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
--   file last changed: 2026-10-26 09:18:20
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
--   file last changed: 2026-10-15 15:01:03
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
--   file last changed: 2026-10-19 11:09:48
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
--   file last changed: 2026-10-19 15:53:15
--   applied 2026-10-19 11:56:19 by acms_user
-- ============================================================
ALTER TABLE farmers ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- ============================================================
-- V20261019161117__create_farmer_trainings.sql
--   file last changed: 2026-10-20 09:42:25
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
--   file last changed: 2026-10-21 10:26:54
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
--   file last changed: 2026-10-22 09:37:38
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
--   file last changed: 2026-10-22 09:37:38
--   applied 2026-10-21 14:30:51 by acms_user
-- ============================================================

ALTER TABLE app_users
ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0;


-- ============================================================
-- V20261022112242__farmer_training_recorded_by.sql
--   file last changed: 2026-10-24 18:44:21
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
--   file last changed: 2026-10-26 10:17:57
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
--   file last changed: 2026-10-26 14:13:51
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
--   file last changed: 2026-10-26 15:00:40
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
--   file last changed: 2026-10-27 11:26:22
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


