
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
