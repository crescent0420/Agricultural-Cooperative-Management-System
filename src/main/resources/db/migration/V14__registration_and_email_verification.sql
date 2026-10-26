-- Part D: self-registration, email verification and one-time passwords.

-- 1. New account columns.
ALTER TABLE app_users
    ADD COLUMN email                VARCHAR(254),
    ADD COLUMN email_verified       BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

-- Accounts created before this step keep working: treat them as already verified.
UPDATE app_users SET email_verified = TRUE;

-- Emails are stored in lower case and each one may belong to only one account.
ALTER TABLE app_users
    ADD CONSTRAINT chk_app_users_email_lower CHECK (email = lower(email)),
    ADD CONSTRAINT uq_app_users_email UNIQUE (email);

-- 2. A FARMER account may now exist before the administrator links it to a farmer record.
ALTER TABLE app_users DROP CONSTRAINT chk_app_users_role_scope;
ALTER TABLE app_users ADD CONSTRAINT chk_app_users_role_scope CHECK (
       (role IN ('FIELD_OFFICER', 'DISTRICT_AGRONOMIST') AND district_id IS NOT NULL AND farmer_id IS NULL)
    OR (role = 'FARMER' AND district_id IS NULL)
    OR (role IN ('UNION_REGISTRAR', 'FINANCE_OFFICER', 'SYSTEM_ADMIN') AND district_id IS NULL AND farmer_id IS NULL)
);

-- 3. One-time email verification links. Only a SHA-256 hash of each link token is stored,
--    so a copy of the database cannot be used to verify someone else's account.
CREATE TABLE email_verification_tokens (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_email_verification_tokens_user FOREIGN KEY (user_id) REFERENCES app_users (id),
    CONSTRAINT uq_email_verification_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_email_verification_tokens_user ON email_verification_tokens (user_id);
