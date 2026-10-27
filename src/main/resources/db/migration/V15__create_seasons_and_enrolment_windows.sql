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