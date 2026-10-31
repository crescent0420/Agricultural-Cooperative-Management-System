-- V20261028095405__create_contributions_and_payments.sql
-- Created: 2026-10-28 09:54:05 +02:00
-- Purpose: create contributions and payments
-- Never edit this file after it has been applied: add a new migration instead.

-- Input contributions (seed, fertiliser...) each farmer owes per season, the payments against them,
-- and every change of an amount owed. Financial history is permanent: the triggers below refuse
-- to delete it or to rewrite it, even for someone with direct database access through the application.

CREATE TABLE contribution_obligations (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    farmer_id  BIGINT         NOT NULL,
    season_id  BIGINT         NOT NULL,
    amount_due NUMERIC(12, 2) NOT NULL,
    created_by BIGINT         NOT NULL,
    created_at TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT fk_obligations_farmer     FOREIGN KEY (farmer_id)  REFERENCES farmers (id),
    CONSTRAINT fk_obligations_season     FOREIGN KEY (season_id)  REFERENCES seasons (id),
    CONSTRAINT fk_obligations_created_by FOREIGN KEY (created_by) REFERENCES app_users (id),
    CONSTRAINT chk_obligations_amount    CHECK (amount_due > 0),
    -- One contribution per farmer per season.
    CONSTRAINT uq_obligations_farmer_season UNIQUE (farmer_id, season_id)
);

CREATE INDEX idx_obligations_season ON contribution_obligations (season_id);

-- Every change of amount_due, with the old and new value, the reason, who and when.
CREATE TABLE contribution_adjustments (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    obligation_id BIGINT         NOT NULL,
    old_amount    NUMERIC(12, 2) NOT NULL,
    new_amount    NUMERIC(12, 2) NOT NULL,
    reason        VARCHAR(500)   NOT NULL,
    adjusted_by   BIGINT         NOT NULL,
    adjusted_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT fk_adjustments_obligation  FOREIGN KEY (obligation_id) REFERENCES contribution_obligations (id),
    CONSTRAINT fk_adjustments_adjusted_by FOREIGN KEY (adjusted_by)   REFERENCES app_users (id),
    CONSTRAINT chk_adjustments_amounts    CHECK (new_amount > 0 AND new_amount <> old_amount)
);

CREATE INDEX idx_adjustments_obligation ON contribution_adjustments (obligation_id);

CREATE TABLE payments (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    obligation_id         BIGINT         NOT NULL,
    -- Chosen by the client for each payment request. Sending the same request again with the same
    -- reference never creates a second payment: the unique constraint guarantees it.
    transaction_reference VARCHAR(64)    NOT NULL,
    amount                NUMERIC(12, 2) NOT NULL,
    method                VARCHAR(20)    NOT NULL,
    status                VARCHAR(12)    NOT NULL DEFAULT 'PENDING',
    -- The mobile money or bank transaction id received with the confirmation.
    provider_reference    VARCHAR(100),
    requested_by          BIGINT         NOT NULL,
    requested_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    confirmed_by          BIGINT,
    confirmed_at          TIMESTAMPTZ,
    failed_by             BIGINT,
    failed_at             TIMESTAMPTZ,
    failure_reason        VARCHAR(500),
    created_at            TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT fk_payments_obligation   FOREIGN KEY (obligation_id) REFERENCES contribution_obligations (id),
    CONSTRAINT fk_payments_requested_by FOREIGN KEY (requested_by)  REFERENCES app_users (id),
    CONSTRAINT fk_payments_confirmed_by FOREIGN KEY (confirmed_by)  REFERENCES app_users (id),
    CONSTRAINT fk_payments_failed_by    FOREIGN KEY (failed_by)     REFERENCES app_users (id),
    CONSTRAINT uq_payments_transaction_reference UNIQUE (transaction_reference),
    CONSTRAINT chk_payments_amount  CHECK (amount > 0),
    CONSTRAINT chk_payments_method  CHECK (method IN ('MOBILE_MONEY', 'BANK_TRANSFER')),
    CONSTRAINT chk_payments_status  CHECK (status IN ('PENDING', 'CONFIRMED', 'FAILED')),
    -- Each status carries exactly the details that belong to it.
    CONSTRAINT chk_payments_status_details CHECK (
           (status = 'PENDING'   AND confirmed_at IS NULL AND confirmed_by IS NULL AND provider_reference IS NULL
                                 AND failed_at IS NULL AND failed_by IS NULL AND failure_reason IS NULL)
        OR (status = 'CONFIRMED' AND confirmed_at IS NOT NULL AND confirmed_by IS NOT NULL AND provider_reference IS NOT NULL
                                 AND failed_at IS NULL AND failed_by IS NULL AND failure_reason IS NULL)
        OR (status = 'FAILED'    AND failed_at IS NOT NULL AND failed_by IS NOT NULL AND failure_reason IS NOT NULL
                                 AND confirmed_at IS NULL AND confirmed_by IS NULL AND provider_reference IS NULL))
);

-- One provider transaction can confirm only one payment.
CREATE UNIQUE INDEX uq_payments_provider_reference ON payments (provider_reference) WHERE provider_reference IS NOT NULL;
CREATE INDEX idx_payments_obligation ON payments (obligation_id, status);

-- ---------- permanent financial history ----------

-- ERRCODE 23514 (check_violation) is an integrity error that Spring always recognises, so the API answers 409.
CREATE FUNCTION forbid_financial_delete() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Rows of % are financial history and can never be deleted.', TG_TABLE_NAME
        USING ERRCODE = '23514';
END $$;

CREATE FUNCTION forbid_financial_update() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Rows of % are financial history and can never be changed.', TG_TABLE_NAME
        USING ERRCODE = '23514';
END $$;

-- A payment's request details never change. Only a PENDING payment may move on, once, to CONFIRMED or
-- FAILED; after that the row is frozen for good.
CREATE FUNCTION guard_payment_update() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.obligation_id <> OLD.obligation_id
       OR NEW.transaction_reference <> OLD.transaction_reference
       OR NEW.amount <> OLD.amount
       OR NEW.method <> OLD.method
       OR NEW.requested_by <> OLD.requested_by
       OR NEW.requested_at <> OLD.requested_at
       OR NEW.created_at <> OLD.created_at THEN
        RAISE EXCEPTION 'The amount, reference, method and contribution of a payment can never change.'
            USING ERRCODE = '23514';
    END IF;
    IF OLD.status <> 'PENDING' THEN
        RAISE EXCEPTION 'Payment % is % and can no longer change.', OLD.id, OLD.status
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END $$;

-- A contribution always stays with the same farmer and season (its amount changes only through an adjustment).
CREATE FUNCTION guard_obligation_update() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.farmer_id <> OLD.farmer_id OR NEW.season_id <> OLD.season_id
       OR NEW.created_by <> OLD.created_by OR NEW.created_at <> OLD.created_at THEN
        RAISE EXCEPTION 'The farmer and season of a contribution can never change.'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_obligations_no_delete BEFORE DELETE ON contribution_obligations
    FOR EACH ROW EXECUTE FUNCTION forbid_financial_delete();
CREATE TRIGGER trg_obligations_guard BEFORE UPDATE ON contribution_obligations
    FOR EACH ROW EXECUTE FUNCTION guard_obligation_update();
CREATE TRIGGER trg_obligations_updated_at BEFORE UPDATE ON contribution_obligations
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_adjustments_no_delete BEFORE DELETE ON contribution_adjustments
    FOR EACH ROW EXECUTE FUNCTION forbid_financial_delete();
CREATE TRIGGER trg_adjustments_no_update BEFORE UPDATE ON contribution_adjustments
    FOR EACH ROW EXECUTE FUNCTION forbid_financial_update();

CREATE TRIGGER trg_payments_no_delete BEFORE DELETE ON payments
    FOR EACH ROW EXECUTE FUNCTION forbid_financial_delete();
CREATE TRIGGER trg_payments_guard BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION guard_payment_update();
CREATE TRIGGER trg_payments_updated_at BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
