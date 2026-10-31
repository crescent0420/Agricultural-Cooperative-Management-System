-- V20261030141154__add_timestamps_to_all_entities.sql
-- Created: 2026-10-30 14:11:54 +02:00
-- Purpose: add timestamps to all entities
-- Never edit this file after it has been applied: add a new migration instead.

-- Every entity now extends BaseEntity, which maps created_at and updated_at, so every entity table needs both.
-- Ten tables had neither, and email_verification_tokens had no updated_at. This adds them.
--
-- Existing rows get their REAL dates, copied from the time column each table already had (issued_at,
-- attempted_at...), never the date of this migration. Copying is an UPDATE, and some of these tables refuse
-- every UPDATE (permanent records). So each such table's own triggers are switched off for the copy only and
-- switched on again straight after. Flyway runs this file as ONE transaction: if anything fails, nothing
-- changes, and no other session ever sees a table without its triggers.

-- ---------- 1. the columns, empty for now ----------
ALTER TABLE audit_log                 ADD COLUMN created_at TIMESTAMPTZ, ADD COLUMN updated_at TIMESTAMPTZ;
ALTER TABLE collection_permits        ADD COLUMN created_at TIMESTAMPTZ, ADD COLUMN updated_at TIMESTAMPTZ;
ALTER TABLE contribution_adjustments  ADD COLUMN created_at TIMESTAMPTZ, ADD COLUMN updated_at TIMESTAMPTZ;
ALTER TABLE enrolment_windows         ADD COLUMN created_at TIMESTAMPTZ, ADD COLUMN updated_at TIMESTAMPTZ;
ALTER TABLE farmer_trainings          ADD COLUMN created_at TIMESTAMPTZ, ADD COLUMN updated_at TIMESTAMPTZ;
ALTER TABLE grading_bands             ADD COLUMN created_at TIMESTAMPTZ, ADD COLUMN updated_at TIMESTAMPTZ;
ALTER TABLE inspection_scores         ADD COLUMN created_at TIMESTAMPTZ, ADD COLUMN updated_at TIMESTAMPTZ;
ALTER TABLE permit_attempts           ADD COLUMN created_at TIMESTAMPTZ, ADD COLUMN updated_at TIMESTAMPTZ;
ALTER TABLE result_corrections        ADD COLUMN created_at TIMESTAMPTZ, ADD COLUMN updated_at TIMESTAMPTZ;
ALTER TABLE email_verification_tokens ADD COLUMN updated_at TIMESTAMPTZ;

-- ---------- 2. fill them from each table's own time column ----------
ALTER TABLE audit_log DISABLE TRIGGER USER;
UPDATE audit_log SET created_at = occurred_at, updated_at = occurred_at;
ALTER TABLE audit_log ENABLE TRIGGER USER;

ALTER TABLE collection_permits DISABLE TRIGGER USER;
UPDATE collection_permits SET created_at = issued_at, updated_at = issued_at;
ALTER TABLE collection_permits ENABLE TRIGGER USER;

ALTER TABLE contribution_adjustments DISABLE TRIGGER USER;
UPDATE contribution_adjustments SET created_at = adjusted_at, updated_at = adjusted_at;
ALTER TABLE contribution_adjustments ENABLE TRIGGER USER;

ALTER TABLE inspection_scores DISABLE TRIGGER USER;
UPDATE inspection_scores SET created_at = entered_at, updated_at = entered_at;
ALTER TABLE inspection_scores ENABLE TRIGGER USER;

ALTER TABLE permit_attempts DISABLE TRIGGER USER;
UPDATE permit_attempts SET created_at = attempted_at, updated_at = attempted_at;
ALTER TABLE permit_attempts ENABLE TRIGGER USER;

ALTER TABLE result_corrections DISABLE TRIGGER USER;
UPDATE result_corrections SET created_at = corrected_at, updated_at = corrected_at;
ALTER TABLE result_corrections ENABLE TRIGGER USER;

-- These have no trigger that refuses an UPDATE.
UPDATE enrolment_windows         SET created_at = opened_at, updated_at = COALESCE(closed_at, opened_at);
UPDATE farmer_trainings          SET created_at = recorded_at, updated_at = recorded_at;
UPDATE grading_bands             SET created_at = now(), updated_at = now();   -- no earlier date was kept
UPDATE email_verification_tokens SET updated_at = COALESCE(used_at, created_at);

-- ---------- 3. required from now on, filled by the application (BaseEntity) or by these defaults ----------
ALTER TABLE audit_log                 ALTER COLUMN created_at SET DEFAULT now(), ALTER COLUMN created_at SET NOT NULL,
                                      ALTER COLUMN updated_at SET DEFAULT now(), ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE collection_permits        ALTER COLUMN created_at SET DEFAULT now(), ALTER COLUMN created_at SET NOT NULL,
                                      ALTER COLUMN updated_at SET DEFAULT now(), ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE contribution_adjustments  ALTER COLUMN created_at SET DEFAULT now(), ALTER COLUMN created_at SET NOT NULL,
                                      ALTER COLUMN updated_at SET DEFAULT now(), ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE enrolment_windows         ALTER COLUMN created_at SET DEFAULT now(), ALTER COLUMN created_at SET NOT NULL,
                                      ALTER COLUMN updated_at SET DEFAULT now(), ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE farmer_trainings          ALTER COLUMN created_at SET DEFAULT now(), ALTER COLUMN created_at SET NOT NULL,
                                      ALTER COLUMN updated_at SET DEFAULT now(), ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE grading_bands             ALTER COLUMN created_at SET DEFAULT now(), ALTER COLUMN created_at SET NOT NULL,
                                      ALTER COLUMN updated_at SET DEFAULT now(), ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE inspection_scores         ALTER COLUMN created_at SET DEFAULT now(), ALTER COLUMN created_at SET NOT NULL,
                                      ALTER COLUMN updated_at SET DEFAULT now(), ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE permit_attempts           ALTER COLUMN created_at SET DEFAULT now(), ALTER COLUMN created_at SET NOT NULL,
                                      ALTER COLUMN updated_at SET DEFAULT now(), ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE result_corrections        ALTER COLUMN created_at SET DEFAULT now(), ALTER COLUMN created_at SET NOT NULL,
                                      ALTER COLUMN updated_at SET DEFAULT now(), ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE email_verification_tokens ALTER COLUMN updated_at SET DEFAULT now(), ALTER COLUMN updated_at SET NOT NULL;

-- ---------- 4. updated_at also follows changes made outside the application (as on the other tables) ----------
-- Only where rows can still change; permanent tables refuse every UPDATE anyway.
CREATE TRIGGER trg_enrolment_windows_updated_at BEFORE UPDATE ON enrolment_windows
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_farmer_trainings_updated_at BEFORE UPDATE ON farmer_trainings
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_grading_bands_updated_at BEFORE UPDATE ON grading_bands
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_inspection_scores_updated_at BEFORE UPDATE ON inspection_scores
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_email_verification_tokens_updated_at BEFORE UPDATE ON email_verification_tokens
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

