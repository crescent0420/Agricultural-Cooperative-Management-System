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
