
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
