
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
