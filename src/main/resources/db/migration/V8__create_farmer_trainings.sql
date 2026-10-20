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
