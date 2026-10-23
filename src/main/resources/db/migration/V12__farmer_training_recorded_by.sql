-- Track which user recorded each farmer training completion.
-- Keep this nullable so existing historical records remain valid.
ALTER TABLE farmer_trainings
    ADD COLUMN recorded_by BIGINT;

ALTER TABLE farmer_trainings
    ADD CONSTRAINT fk_farmer_trainings_recorded_by
        FOREIGN KEY (recorded_by) REFERENCES app_users (id);

CREATE INDEX idx_farmer_trainings_recorded_by
    ON farmer_trainings (recorded_by);