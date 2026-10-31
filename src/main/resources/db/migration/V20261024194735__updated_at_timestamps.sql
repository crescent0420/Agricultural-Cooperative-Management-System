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