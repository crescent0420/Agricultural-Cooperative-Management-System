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
