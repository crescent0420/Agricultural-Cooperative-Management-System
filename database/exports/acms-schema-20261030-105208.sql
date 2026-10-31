-- ACMS database schema (pg_dump --schema-only, no data)
-- Generated:  2026-10-30 10:52:08 +02:00
-- Git commit: 1f6b678

--
-- PostgreSQL database dump
--

\restrict AwWY8jfuPYqLghng3C88A1eTxEwaUDMx4XTCsSl0W2n6PnzsLQYRZDLbeUaPhSe

-- Dumped from database version 18.6
-- Dumped by pg_dump version 18.6

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: forbid_correction_changes(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.forbid_correction_changes() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    RAISE EXCEPTION 'Corrections are permanent history and can never be changed or deleted.'
        USING ERRCODE = '23514';
END $$;


--
-- Name: forbid_financial_delete(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.forbid_financial_delete() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    RAISE EXCEPTION 'Rows of % are financial history and can never be deleted.', TG_TABLE_NAME
        USING ERRCODE = '23514';
END $$;


--
-- Name: forbid_financial_update(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.forbid_financial_update() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    RAISE EXCEPTION 'Rows of % are financial history and can never be changed.', TG_TABLE_NAME
        USING ERRCODE = '23514';
END $$;


--
-- Name: forbid_permit_record_changes(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.forbid_permit_record_changes() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    RAISE EXCEPTION 'Rows of % are permanent records and can never be changed or deleted.', TG_TABLE_NAME
        USING ERRCODE = '23514';
END $$;


--
-- Name: guard_inspection_results(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.guard_inspection_results() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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


--
-- Name: guard_inspection_scores(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.guard_inspection_scores() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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


--
-- Name: guard_obligation_update(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.guard_obligation_update() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF NEW.farmer_id <> OLD.farmer_id OR NEW.season_id <> OLD.season_id
       OR NEW.created_by <> OLD.created_by OR NEW.created_at <> OLD.created_at THEN
        RAISE EXCEPTION 'The farmer and season of a contribution can never change.'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END $$;


--
-- Name: guard_payment_update(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.guard_payment_update() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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


--
-- Name: prevent_audit_log_change(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.prevent_audit_log_change() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    RAISE EXCEPTION
        'audit_log is append-only: rows cannot be updated or deleted';
END;
$$;


--
-- Name: set_updated_at(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.set_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: app_users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.app_users (
    id bigint NOT NULL,
    username character varying(100) NOT NULL,
    password_hash character varying(255) NOT NULL,
    role character varying(32) NOT NULL,
    enabled boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    password_changed_at timestamp with time zone DEFAULT now() NOT NULL,
    district_id bigint,
    farmer_id bigint,
    token_version bigint DEFAULT 0 NOT NULL,
    email character varying(254),
    email_verified boolean DEFAULT false NOT NULL,
    must_change_password boolean DEFAULT false NOT NULL,
    CONSTRAINT chk_app_users_email_lower CHECK (((email)::text = lower((email)::text))),
    CONSTRAINT chk_app_users_role CHECK (((role)::text = ANY ((ARRAY['FARMER'::character varying, 'FIELD_OFFICER'::character varying, 'DISTRICT_AGRONOMIST'::character varying, 'UNION_REGISTRAR'::character varying, 'FINANCE_OFFICER'::character varying, 'SYSTEM_ADMIN'::character varying])::text[]))),
    CONSTRAINT chk_app_users_role_scope CHECK (((((role)::text = ANY ((ARRAY['FIELD_OFFICER'::character varying, 'DISTRICT_AGRONOMIST'::character varying])::text[])) AND (district_id IS NOT NULL) AND (farmer_id IS NULL)) OR (((role)::text = 'FARMER'::text) AND (district_id IS NULL)) OR (((role)::text = ANY ((ARRAY['UNION_REGISTRAR'::character varying, 'FINANCE_OFFICER'::character varying, 'SYSTEM_ADMIN'::character varying])::text[])) AND (district_id IS NULL) AND (farmer_id IS NULL))))
);


--
-- Name: app_users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.app_users ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.app_users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: audit_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audit_log (
    id bigint NOT NULL,
    occurred_at timestamp with time zone DEFAULT now() NOT NULL,
    event_type character varying(50) NOT NULL,
    success boolean NOT NULL,
    actor_user_id bigint,
    actor_username character varying(100),
    target_type character varying(50),
    target_id bigint,
    details character varying(500),
    ip_address character varying(45)
);


--
-- Name: audit_log_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.audit_log ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.audit_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: collection_permits; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.collection_permits (
    id bigint NOT NULL,
    permit_number character varying(20) NOT NULL,
    enrolment_id bigint NOT NULL,
    farmer_id bigint NOT NULL,
    issued_by bigint NOT NULL,
    issued_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_permits_number_format CHECK (((permit_number)::text ~ '^CP-[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}$'::text))
);


--
-- Name: collection_permits_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.collection_permits ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.collection_permits_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: contribution_adjustments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.contribution_adjustments (
    id bigint NOT NULL,
    obligation_id bigint NOT NULL,
    old_amount numeric(12,2) NOT NULL,
    new_amount numeric(12,2) NOT NULL,
    reason character varying(500) NOT NULL,
    adjusted_by bigint NOT NULL,
    adjusted_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_adjustments_amounts CHECK (((new_amount > (0)::numeric) AND (new_amount <> old_amount)))
);


--
-- Name: contribution_adjustments_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.contribution_adjustments ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.contribution_adjustments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: contribution_obligations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.contribution_obligations (
    id bigint NOT NULL,
    farmer_id bigint NOT NULL,
    season_id bigint NOT NULL,
    amount_due numeric(12,2) NOT NULL,
    created_by bigint NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_obligations_amount CHECK ((amount_due > (0)::numeric))
);


--
-- Name: contribution_obligations_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.contribution_obligations ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.contribution_obligations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: cooperatives; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cooperatives (
    id bigint NOT NULL,
    name character varying(150) NOT NULL,
    district_id bigint NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: cooperatives_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.cooperatives ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.cooperatives_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: crop_programmes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.crop_programmes (
    id bigint NOT NULL,
    name character varying(150) NOT NULL,
    district_id bigint NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: crop_programmes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.crop_programmes ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.crop_programmes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: districts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.districts (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    province_id bigint
);


--
-- Name: districts_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.districts ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.districts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: email_verification_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.email_verification_tokens (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    token_hash character varying(64) NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    used_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: email_verification_tokens_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.email_verification_tokens ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.email_verification_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: enrolment_windows; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.enrolment_windows (
    id bigint NOT NULL,
    season_id bigint NOT NULL,
    status character varying(10) NOT NULL,
    opened_at timestamp with time zone DEFAULT now() NOT NULL,
    opened_by bigint NOT NULL,
    closed_at timestamp with time zone,
    closed_by bigint,
    CONSTRAINT chk_enrolment_windows_closing CHECK (((((status)::text = 'OPEN'::text) AND (closed_at IS NULL) AND (closed_by IS NULL)) OR (((status)::text = 'CLOSED'::text) AND (closed_at IS NOT NULL) AND (closed_by IS NOT NULL) AND (closed_at >= opened_at)))),
    CONSTRAINT chk_enrolment_windows_status CHECK (((status)::text = ANY ((ARRAY['OPEN'::character varying, 'CLOSED'::character varying])::text[])))
);


--
-- Name: enrolment_windows_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.enrolment_windows ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.enrolment_windows_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: enrolments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.enrolments (
    id bigint NOT NULL,
    offering_id bigint NOT NULL,
    farmer_id bigint NOT NULL,
    status character varying(12) DEFAULT 'ENROLLED'::character varying NOT NULL,
    hectares numeric(6,2) NOT NULL,
    enrolled_at timestamp with time zone DEFAULT now() NOT NULL,
    enrolled_by bigint NOT NULL,
    cancelled_at timestamp with time zone,
    cancelled_by bigint,
    cancellation_reason character varying(500),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_enrolments_cancellation CHECK (((((status)::text = 'ENROLLED'::text) AND (cancelled_at IS NULL) AND (cancelled_by IS NULL) AND (cancellation_reason IS NULL)) OR (((status)::text = 'CANCELLED'::text) AND (cancelled_at IS NOT NULL) AND (cancelled_by IS NOT NULL)))),
    CONSTRAINT chk_enrolments_hectares CHECK ((hectares > (0)::numeric)),
    CONSTRAINT chk_enrolments_status CHECK (((status)::text = ANY ((ARRAY['ENROLLED'::character varying, 'CANCELLED'::character varying])::text[])))
);


--
-- Name: enrolments_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.enrolments ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.enrolments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: farmer_membership_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.farmer_membership_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: farmer_trainings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.farmer_trainings (
    id bigint NOT NULL,
    farmer_id bigint NOT NULL,
    training_id bigint NOT NULL,
    completed_on date NOT NULL,
    recorded_at timestamp with time zone DEFAULT now() NOT NULL,
    recorded_by bigint
);


--
-- Name: farmer_trainings_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.farmer_trainings ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.farmer_trainings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: farmers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.farmers (
    id bigint NOT NULL,
    membership_number character varying(20) NOT NULL,
    national_id character varying(16) NOT NULL,
    full_name character varying(150) NOT NULL,
    phone_number character varying(20),
    cooperative_id bigint NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT ck_farmers_national_id CHECK (((national_id)::text ~ '^[0-9]{16}$'::text)),
    CONSTRAINT ck_farmers_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'SUSPENDED'::character varying, 'WITHDRAWN'::character varying, 'RETIRED'::character varying])::text[])))
);


--
-- Name: farmers_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.farmers ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.farmers_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


--
-- Name: grading_bands; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.grading_bands (
    id bigint NOT NULL,
    grade character varying(5) NOT NULL,
    min_score numeric(5,2) NOT NULL,
    description character varying(100),
    CONSTRAINT chk_grading_bands_min CHECK (((min_score >= (0)::numeric) AND (min_score <= (100)::numeric)))
);


--
-- Name: grading_bands_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.grading_bands ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.grading_bands_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: inspection_results; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inspection_results (
    id bigint NOT NULL,
    enrolment_id bigint NOT NULL,
    status character varying(10) DEFAULT 'DRAFT'::character varying NOT NULL,
    final_score numeric(5,2),
    grade character varying(5),
    created_by bigint NOT NULL,
    submitted_by bigint,
    submitted_at timestamp with time zone,
    published_by bigint,
    published_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_results_final_score CHECK (((final_score >= (0)::numeric) AND (final_score <= (100)::numeric))),
    CONSTRAINT chk_results_stage_details CHECK (((((status)::text = 'DRAFT'::text) AND (final_score IS NULL) AND (grade IS NULL) AND (submitted_at IS NULL) AND (submitted_by IS NULL) AND (published_at IS NULL) AND (published_by IS NULL)) OR (((status)::text = 'SUBMITTED'::text) AND (final_score IS NOT NULL) AND (grade IS NOT NULL) AND (submitted_at IS NOT NULL) AND (submitted_by IS NOT NULL) AND (published_at IS NULL) AND (published_by IS NULL)) OR (((status)::text = 'PUBLISHED'::text) AND (final_score IS NOT NULL) AND (grade IS NOT NULL) AND (submitted_at IS NOT NULL) AND (submitted_by IS NOT NULL) AND (published_at IS NOT NULL) AND (published_by IS NOT NULL)))),
    CONSTRAINT chk_results_status CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'SUBMITTED'::character varying, 'PUBLISHED'::character varying])::text[])))
);


--
-- Name: inspection_results_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.inspection_results ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.inspection_results_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: inspection_scores; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inspection_scores (
    id bigint NOT NULL,
    result_id bigint NOT NULL,
    inspection_id bigint NOT NULL,
    score numeric(6,2) NOT NULL,
    entered_by bigint NOT NULL,
    entered_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_scores_not_negative CHECK ((score >= (0)::numeric))
);


--
-- Name: inspection_scores_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.inspection_scores ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.inspection_scores_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: inspections; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inspections (
    id bigint NOT NULL,
    offering_id bigint NOT NULL,
    name character varying(100) NOT NULL,
    max_score numeric(6,2) NOT NULL,
    weight numeric(5,2) NOT NULL,
    created_by bigint NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_inspections_max_score CHECK ((max_score > (0)::numeric)),
    CONSTRAINT chk_inspections_weight CHECK (((weight > (0)::numeric) AND (weight <= (100)::numeric)))
);


--
-- Name: inspections_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.inspections ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.inspections_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: payments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payments (
    id bigint NOT NULL,
    obligation_id bigint NOT NULL,
    transaction_reference character varying(64) NOT NULL,
    amount numeric(12,2) NOT NULL,
    method character varying(20) NOT NULL,
    status character varying(12) DEFAULT 'PENDING'::character varying NOT NULL,
    provider_reference character varying(100),
    requested_by bigint NOT NULL,
    requested_at timestamp with time zone DEFAULT now() NOT NULL,
    confirmed_by bigint,
    confirmed_at timestamp with time zone,
    failed_by bigint,
    failed_at timestamp with time zone,
    failure_reason character varying(500),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_payments_amount CHECK ((amount > (0)::numeric)),
    CONSTRAINT chk_payments_method CHECK (((method)::text = ANY ((ARRAY['MOBILE_MONEY'::character varying, 'BANK_TRANSFER'::character varying])::text[]))),
    CONSTRAINT chk_payments_status CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'CONFIRMED'::character varying, 'FAILED'::character varying])::text[]))),
    CONSTRAINT chk_payments_status_details CHECK (((((status)::text = 'PENDING'::text) AND (confirmed_at IS NULL) AND (confirmed_by IS NULL) AND (provider_reference IS NULL) AND (failed_at IS NULL) AND (failed_by IS NULL) AND (failure_reason IS NULL)) OR (((status)::text = 'CONFIRMED'::text) AND (confirmed_at IS NOT NULL) AND (confirmed_by IS NOT NULL) AND (provider_reference IS NOT NULL) AND (failed_at IS NULL) AND (failed_by IS NULL) AND (failure_reason IS NULL)) OR (((status)::text = 'FAILED'::text) AND (failed_at IS NOT NULL) AND (failed_by IS NOT NULL) AND (failure_reason IS NOT NULL) AND (confirmed_at IS NULL) AND (confirmed_by IS NULL) AND (provider_reference IS NULL))))
);


--
-- Name: payments_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.payments ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.payments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: permit_attempts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.permit_attempts (
    id bigint NOT NULL,
    enrolment_id bigint,
    farmer_id bigint,
    user_id bigint NOT NULL,
    username character varying(100) NOT NULL,
    outcome character varying(16) NOT NULL,
    reason character varying(500),
    permit_id bigint,
    attempted_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_permit_attempts_details CHECK (((((outcome)::text = 'REFUSED'::text) AND (reason IS NOT NULL) AND (permit_id IS NULL)) OR (((outcome)::text = ANY ((ARRAY['ISSUED'::character varying, 'ALREADY_ISSUED'::character varying])::text[])) AND (reason IS NULL) AND (permit_id IS NOT NULL)))),
    CONSTRAINT chk_permit_attempts_outcome CHECK (((outcome)::text = ANY ((ARRAY['ISSUED'::character varying, 'ALREADY_ISSUED'::character varying, 'REFUSED'::character varying])::text[])))
);


--
-- Name: permit_attempts_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.permit_attempts ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.permit_attempts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: programme_offerings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.programme_offerings (
    id bigint NOT NULL,
    programme_id bigint NOT NULL,
    season_id bigint NOT NULL,
    field_officer_id bigint NOT NULL,
    hectares_per_participant numeric(6,2) NOT NULL,
    max_participants integer NOT NULL,
    enrolled_count integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_offerings_capacity CHECK (((enrolled_count >= 0) AND (enrolled_count <= max_participants))),
    CONSTRAINT chk_offerings_hectares CHECK ((hectares_per_participant > (0)::numeric)),
    CONSTRAINT chk_offerings_max CHECK ((max_participants > 0))
);


--
-- Name: programme_offerings_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.programme_offerings ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.programme_offerings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: programme_prerequisites; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.programme_prerequisites (
    programme_id bigint NOT NULL,
    training_id bigint NOT NULL
);


--
-- Name: provinces; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.provinces (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: provinces_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.provinces ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.provinces_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: result_corrections; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.result_corrections (
    id bigint NOT NULL,
    result_id bigint NOT NULL,
    inspection_id bigint NOT NULL,
    original_score numeric(6,2) NOT NULL,
    corrected_score numeric(6,2) NOT NULL,
    original_final_score numeric(5,2) NOT NULL,
    corrected_final_score numeric(5,2) NOT NULL,
    original_grade character varying(5) NOT NULL,
    corrected_grade character varying(5) NOT NULL,
    reason character varying(500) NOT NULL,
    corrected_by bigint NOT NULL,
    corrected_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_corrections_scores CHECK (((corrected_score >= (0)::numeric) AND (corrected_score <> original_score)))
);


--
-- Name: result_corrections_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.result_corrections ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.result_corrections_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: seasons; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.seasons (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    starts_on date NOT NULL,
    ends_on date NOT NULL,
    max_land_hectares numeric(8,2) NOT NULL,
    distribution_date date NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_seasons_dates CHECK ((ends_on > starts_on)),
    CONSTRAINT chk_seasons_distribution CHECK (((distribution_date >= starts_on) AND (distribution_date <= ends_on))),
    CONSTRAINT chk_seasons_max_land CHECK ((max_land_hectares > (0)::numeric))
);


--
-- Name: seasons_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.seasons ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.seasons_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: trainings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.trainings (
    id bigint NOT NULL,
    name character varying(150) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: trainings_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.trainings ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.trainings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: app_users app_users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_users
    ADD CONSTRAINT app_users_pkey PRIMARY KEY (id);


--
-- Name: audit_log audit_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_log
    ADD CONSTRAINT audit_log_pkey PRIMARY KEY (id);


--
-- Name: collection_permits collection_permits_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collection_permits
    ADD CONSTRAINT collection_permits_pkey PRIMARY KEY (id);


--
-- Name: contribution_adjustments contribution_adjustments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contribution_adjustments
    ADD CONSTRAINT contribution_adjustments_pkey PRIMARY KEY (id);


--
-- Name: contribution_obligations contribution_obligations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contribution_obligations
    ADD CONSTRAINT contribution_obligations_pkey PRIMARY KEY (id);


--
-- Name: cooperatives cooperatives_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cooperatives
    ADD CONSTRAINT cooperatives_pkey PRIMARY KEY (id);


--
-- Name: crop_programmes crop_programmes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crop_programmes
    ADD CONSTRAINT crop_programmes_pkey PRIMARY KEY (id);


--
-- Name: districts districts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.districts
    ADD CONSTRAINT districts_pkey PRIMARY KEY (id);


--
-- Name: email_verification_tokens email_verification_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.email_verification_tokens
    ADD CONSTRAINT email_verification_tokens_pkey PRIMARY KEY (id);


--
-- Name: enrolment_windows enrolment_windows_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enrolment_windows
    ADD CONSTRAINT enrolment_windows_pkey PRIMARY KEY (id);


--
-- Name: enrolments enrolments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enrolments
    ADD CONSTRAINT enrolments_pkey PRIMARY KEY (id);


--
-- Name: farmer_trainings farmer_trainings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.farmer_trainings
    ADD CONSTRAINT farmer_trainings_pkey PRIMARY KEY (id);


--
-- Name: farmers farmers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.farmers
    ADD CONSTRAINT farmers_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: grading_bands grading_bands_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grading_bands
    ADD CONSTRAINT grading_bands_pkey PRIMARY KEY (id);


--
-- Name: inspection_results inspection_results_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inspection_results
    ADD CONSTRAINT inspection_results_pkey PRIMARY KEY (id);


--
-- Name: inspection_scores inspection_scores_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inspection_scores
    ADD CONSTRAINT inspection_scores_pkey PRIMARY KEY (id);


--
-- Name: inspections inspections_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inspections
    ADD CONSTRAINT inspections_pkey PRIMARY KEY (id);


--
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);


--
-- Name: permit_attempts permit_attempts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permit_attempts
    ADD CONSTRAINT permit_attempts_pkey PRIMARY KEY (id);


--
-- Name: programme_offerings programme_offerings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.programme_offerings
    ADD CONSTRAINT programme_offerings_pkey PRIMARY KEY (id);


--
-- Name: programme_prerequisites programme_prerequisites_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.programme_prerequisites
    ADD CONSTRAINT programme_prerequisites_pkey PRIMARY KEY (programme_id, training_id);


--
-- Name: provinces provinces_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.provinces
    ADD CONSTRAINT provinces_pkey PRIMARY KEY (id);


--
-- Name: result_corrections result_corrections_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.result_corrections
    ADD CONSTRAINT result_corrections_pkey PRIMARY KEY (id);


--
-- Name: seasons seasons_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.seasons
    ADD CONSTRAINT seasons_pkey PRIMARY KEY (id);


--
-- Name: trainings trainings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trainings
    ADD CONSTRAINT trainings_pkey PRIMARY KEY (id);


--
-- Name: app_users uq_app_users_email; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_users
    ADD CONSTRAINT uq_app_users_email UNIQUE (email);


--
-- Name: app_users uq_app_users_username; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_users
    ADD CONSTRAINT uq_app_users_username UNIQUE (username);


--
-- Name: email_verification_tokens uq_email_verification_tokens_hash; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.email_verification_tokens
    ADD CONSTRAINT uq_email_verification_tokens_hash UNIQUE (token_hash);


--
-- Name: farmer_trainings uq_farmer_trainings; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.farmer_trainings
    ADD CONSTRAINT uq_farmer_trainings UNIQUE (farmer_id, training_id);


--
-- Name: farmers uq_farmers_membership_number; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.farmers
    ADD CONSTRAINT uq_farmers_membership_number UNIQUE (membership_number);


--
-- Name: farmers uq_farmers_national_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.farmers
    ADD CONSTRAINT uq_farmers_national_id UNIQUE (national_id);


--
-- Name: grading_bands uq_grading_bands_min; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grading_bands
    ADD CONSTRAINT uq_grading_bands_min UNIQUE (min_score);


--
-- Name: contribution_obligations uq_obligations_farmer_season; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contribution_obligations
    ADD CONSTRAINT uq_obligations_farmer_season UNIQUE (farmer_id, season_id);


--
-- Name: programme_offerings uq_offerings_programme_season; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.programme_offerings
    ADD CONSTRAINT uq_offerings_programme_season UNIQUE (programme_id, season_id);


--
-- Name: payments uq_payments_transaction_reference; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT uq_payments_transaction_reference UNIQUE (transaction_reference);


--
-- Name: collection_permits uq_permits_enrolment; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collection_permits
    ADD CONSTRAINT uq_permits_enrolment UNIQUE (enrolment_id);


--
-- Name: collection_permits uq_permits_number; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collection_permits
    ADD CONSTRAINT uq_permits_number UNIQUE (permit_number);


--
-- Name: inspection_results uq_results_enrolment; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inspection_results
    ADD CONSTRAINT uq_results_enrolment UNIQUE (enrolment_id);


--
-- Name: inspection_scores uq_scores_result_inspection; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inspection_scores
    ADD CONSTRAINT uq_scores_result_inspection UNIQUE (result_id, inspection_id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_adjustments_obligation; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_adjustments_obligation ON public.contribution_adjustments USING btree (obligation_id);


--
-- Name: idx_audit_log_event_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_log_event_type ON public.audit_log USING btree (event_type);


--
-- Name: idx_audit_log_occurred_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_log_occurred_at ON public.audit_log USING btree (occurred_at DESC);


--
-- Name: idx_corrections_result; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_corrections_result ON public.result_corrections USING btree (result_id);


--
-- Name: idx_districts_province; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_districts_province ON public.districts USING btree (province_id);


--
-- Name: idx_email_verification_tokens_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_email_verification_tokens_user ON public.email_verification_tokens USING btree (user_id);


--
-- Name: idx_enrolment_windows_season; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_enrolment_windows_season ON public.enrolment_windows USING btree (season_id);


--
-- Name: idx_enrolments_farmer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_enrolments_farmer ON public.enrolments USING btree (farmer_id, status);


--
-- Name: idx_enrolments_offering; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_enrolments_offering ON public.enrolments USING btree (offering_id, status);


--
-- Name: idx_farmer_trainings_recorded_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_farmer_trainings_recorded_by ON public.farmer_trainings USING btree (recorded_by);


--
-- Name: idx_farmers_cooperative; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_farmers_cooperative ON public.farmers USING btree (cooperative_id);


--
-- Name: idx_obligations_season; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_obligations_season ON public.contribution_obligations USING btree (season_id);


--
-- Name: idx_offerings_field_officer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_offerings_field_officer ON public.programme_offerings USING btree (field_officer_id);


--
-- Name: idx_offerings_season; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_offerings_season ON public.programme_offerings USING btree (season_id);


--
-- Name: idx_payments_obligation; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payments_obligation ON public.payments USING btree (obligation_id, status);


--
-- Name: idx_permit_attempts_enrolment; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_permit_attempts_enrolment ON public.permit_attempts USING btree (enrolment_id);


--
-- Name: idx_permit_attempts_farmer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_permit_attempts_farmer ON public.permit_attempts USING btree (farmer_id, attempted_at);


--
-- Name: idx_permits_farmer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_permits_farmer ON public.collection_permits USING btree (farmer_id);


--
-- Name: idx_prereq_training; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_prereq_training ON public.programme_prerequisites USING btree (training_id);


--
-- Name: idx_scores_inspection; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scores_inspection ON public.inspection_scores USING btree (inspection_id);


--
-- Name: uq_app_users_farmer_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_app_users_farmer_id ON public.app_users USING btree (farmer_id) WHERE (farmer_id IS NOT NULL);


--
-- Name: uq_cooperatives_district_name; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_cooperatives_district_name ON public.cooperatives USING btree (district_id, lower((name)::text));


--
-- Name: uq_crop_programmes_district_name; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_crop_programmes_district_name ON public.crop_programmes USING btree (district_id, lower((name)::text));


--
-- Name: uq_districts_name_lower; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_districts_name_lower ON public.districts USING btree (lower((name)::text));


--
-- Name: uq_enrolment_windows_one_open_per_season; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_enrolment_windows_one_open_per_season ON public.enrolment_windows USING btree (season_id) WHERE ((status)::text = 'OPEN'::text);


--
-- Name: uq_enrolments_one_active_per_farmer; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_enrolments_one_active_per_farmer ON public.enrolments USING btree (offering_id, farmer_id) WHERE ((status)::text = 'ENROLLED'::text);


--
-- Name: uq_grading_bands_grade_lower; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_grading_bands_grade_lower ON public.grading_bands USING btree (lower((grade)::text));


--
-- Name: uq_inspections_offering_name; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_inspections_offering_name ON public.inspections USING btree (offering_id, lower((name)::text));


--
-- Name: uq_payments_provider_reference; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_payments_provider_reference ON public.payments USING btree (provider_reference) WHERE (provider_reference IS NOT NULL);


--
-- Name: uq_provinces_name_lower; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_provinces_name_lower ON public.provinces USING btree (lower((name)::text));


--
-- Name: uq_seasons_name_lower; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_seasons_name_lower ON public.seasons USING btree (lower((name)::text));


--
-- Name: uq_trainings_name_lower; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_trainings_name_lower ON public.trainings USING btree (lower((name)::text));


--
-- Name: contribution_adjustments trg_adjustments_no_delete; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_adjustments_no_delete BEFORE DELETE ON public.contribution_adjustments FOR EACH ROW EXECUTE FUNCTION public.forbid_financial_delete();


--
-- Name: contribution_adjustments trg_adjustments_no_update; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_adjustments_no_update BEFORE UPDATE ON public.contribution_adjustments FOR EACH ROW EXECUTE FUNCTION public.forbid_financial_update();


--
-- Name: app_users trg_app_users_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_app_users_updated_at BEFORE UPDATE ON public.app_users FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();


--
-- Name: audit_log trg_audit_log_append_only; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_audit_log_append_only BEFORE DELETE OR UPDATE ON public.audit_log FOR EACH ROW EXECUTE FUNCTION public.prevent_audit_log_change();


--
-- Name: cooperatives trg_cooperatives_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_cooperatives_updated_at BEFORE UPDATE ON public.cooperatives FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();


--
-- Name: result_corrections trg_corrections_permanent; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_corrections_permanent BEFORE DELETE OR UPDATE ON public.result_corrections FOR EACH ROW EXECUTE FUNCTION public.forbid_correction_changes();


--
-- Name: crop_programmes trg_crop_programmes_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_crop_programmes_updated_at BEFORE UPDATE ON public.crop_programmes FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();


--
-- Name: districts trg_districts_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_districts_updated_at BEFORE UPDATE ON public.districts FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();


--
-- Name: enrolments trg_enrolments_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_enrolments_updated_at BEFORE UPDATE ON public.enrolments FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();


--
-- Name: farmers trg_farmers_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_farmers_updated_at BEFORE UPDATE ON public.farmers FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();


--
-- Name: inspections trg_inspections_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_inspections_updated_at BEFORE UPDATE ON public.inspections FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();


--
-- Name: contribution_obligations trg_obligations_guard; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_obligations_guard BEFORE UPDATE ON public.contribution_obligations FOR EACH ROW EXECUTE FUNCTION public.guard_obligation_update();


--
-- Name: contribution_obligations trg_obligations_no_delete; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_obligations_no_delete BEFORE DELETE ON public.contribution_obligations FOR EACH ROW EXECUTE FUNCTION public.forbid_financial_delete();


--
-- Name: contribution_obligations trg_obligations_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_obligations_updated_at BEFORE UPDATE ON public.contribution_obligations FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();


--
-- Name: payments trg_payments_guard; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_payments_guard BEFORE UPDATE ON public.payments FOR EACH ROW EXECUTE FUNCTION public.guard_payment_update();


--
-- Name: payments trg_payments_no_delete; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_payments_no_delete BEFORE DELETE ON public.payments FOR EACH ROW EXECUTE FUNCTION public.forbid_financial_delete();


--
-- Name: payments trg_payments_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_payments_updated_at BEFORE UPDATE ON public.payments FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();


--
-- Name: permit_attempts trg_permit_attempts_permanent; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_permit_attempts_permanent BEFORE DELETE OR UPDATE ON public.permit_attempts FOR EACH ROW EXECUTE FUNCTION public.forbid_permit_record_changes();


--
-- Name: collection_permits trg_permits_permanent; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_permits_permanent BEFORE DELETE OR UPDATE ON public.collection_permits FOR EACH ROW EXECUTE FUNCTION public.forbid_permit_record_changes();


--
-- Name: programme_offerings trg_programme_offerings_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_programme_offerings_updated_at BEFORE UPDATE ON public.programme_offerings FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();


--
-- Name: provinces trg_provinces_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_provinces_updated_at BEFORE UPDATE ON public.provinces FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();


--
-- Name: inspection_results trg_results_guard; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_results_guard BEFORE DELETE OR UPDATE ON public.inspection_results FOR EACH ROW EXECUTE FUNCTION public.guard_inspection_results();


--
-- Name: inspection_results trg_results_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_results_updated_at BEFORE UPDATE ON public.inspection_results FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();


--
-- Name: inspection_scores trg_scores_guard; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_scores_guard BEFORE INSERT OR DELETE OR UPDATE ON public.inspection_scores FOR EACH ROW EXECUTE FUNCTION public.guard_inspection_scores();


--
-- Name: seasons trg_seasons_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_seasons_updated_at BEFORE UPDATE ON public.seasons FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();


--
-- Name: trainings trg_trainings_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_trainings_updated_at BEFORE UPDATE ON public.trainings FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();


--
-- Name: contribution_adjustments fk_adjustments_adjusted_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contribution_adjustments
    ADD CONSTRAINT fk_adjustments_adjusted_by FOREIGN KEY (adjusted_by) REFERENCES public.app_users(id);


--
-- Name: contribution_adjustments fk_adjustments_obligation; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contribution_adjustments
    ADD CONSTRAINT fk_adjustments_obligation FOREIGN KEY (obligation_id) REFERENCES public.contribution_obligations(id);


--
-- Name: app_users fk_app_users_district; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_users
    ADD CONSTRAINT fk_app_users_district FOREIGN KEY (district_id) REFERENCES public.districts(id);


--
-- Name: app_users fk_app_users_farmer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_users
    ADD CONSTRAINT fk_app_users_farmer FOREIGN KEY (farmer_id) REFERENCES public.farmers(id);


--
-- Name: cooperatives fk_cooperatives_district; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cooperatives
    ADD CONSTRAINT fk_cooperatives_district FOREIGN KEY (district_id) REFERENCES public.districts(id);


--
-- Name: result_corrections fk_corrections_corrected_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.result_corrections
    ADD CONSTRAINT fk_corrections_corrected_by FOREIGN KEY (corrected_by) REFERENCES public.app_users(id);


--
-- Name: result_corrections fk_corrections_inspection; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.result_corrections
    ADD CONSTRAINT fk_corrections_inspection FOREIGN KEY (inspection_id) REFERENCES public.inspections(id);


--
-- Name: result_corrections fk_corrections_result; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.result_corrections
    ADD CONSTRAINT fk_corrections_result FOREIGN KEY (result_id) REFERENCES public.inspection_results(id);


--
-- Name: crop_programmes fk_crop_programmes_district; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crop_programmes
    ADD CONSTRAINT fk_crop_programmes_district FOREIGN KEY (district_id) REFERENCES public.districts(id);


--
-- Name: districts fk_districts_province; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.districts
    ADD CONSTRAINT fk_districts_province FOREIGN KEY (province_id) REFERENCES public.provinces(id);


--
-- Name: email_verification_tokens fk_email_verification_tokens_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.email_verification_tokens
    ADD CONSTRAINT fk_email_verification_tokens_user FOREIGN KEY (user_id) REFERENCES public.app_users(id);


--
-- Name: enrolment_windows fk_enrolment_windows_closed_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enrolment_windows
    ADD CONSTRAINT fk_enrolment_windows_closed_by FOREIGN KEY (closed_by) REFERENCES public.app_users(id);


--
-- Name: enrolment_windows fk_enrolment_windows_opened_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enrolment_windows
    ADD CONSTRAINT fk_enrolment_windows_opened_by FOREIGN KEY (opened_by) REFERENCES public.app_users(id);


--
-- Name: enrolment_windows fk_enrolment_windows_season; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enrolment_windows
    ADD CONSTRAINT fk_enrolment_windows_season FOREIGN KEY (season_id) REFERENCES public.seasons(id);


--
-- Name: enrolments fk_enrolments_cancelled_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enrolments
    ADD CONSTRAINT fk_enrolments_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES public.app_users(id);


--
-- Name: enrolments fk_enrolments_enrolled_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enrolments
    ADD CONSTRAINT fk_enrolments_enrolled_by FOREIGN KEY (enrolled_by) REFERENCES public.app_users(id);


--
-- Name: enrolments fk_enrolments_farmer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enrolments
    ADD CONSTRAINT fk_enrolments_farmer FOREIGN KEY (farmer_id) REFERENCES public.farmers(id);


--
-- Name: enrolments fk_enrolments_offering; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enrolments
    ADD CONSTRAINT fk_enrolments_offering FOREIGN KEY (offering_id) REFERENCES public.programme_offerings(id);


--
-- Name: farmer_trainings fk_farmer_trainings_farmer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.farmer_trainings
    ADD CONSTRAINT fk_farmer_trainings_farmer FOREIGN KEY (farmer_id) REFERENCES public.farmers(id);


--
-- Name: farmer_trainings fk_farmer_trainings_recorded_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.farmer_trainings
    ADD CONSTRAINT fk_farmer_trainings_recorded_by FOREIGN KEY (recorded_by) REFERENCES public.app_users(id);


--
-- Name: farmer_trainings fk_farmer_trainings_training; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.farmer_trainings
    ADD CONSTRAINT fk_farmer_trainings_training FOREIGN KEY (training_id) REFERENCES public.trainings(id);


--
-- Name: farmers fk_farmers_cooperative; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.farmers
    ADD CONSTRAINT fk_farmers_cooperative FOREIGN KEY (cooperative_id) REFERENCES public.cooperatives(id);


--
-- Name: inspections fk_inspections_created_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inspections
    ADD CONSTRAINT fk_inspections_created_by FOREIGN KEY (created_by) REFERENCES public.app_users(id);


--
-- Name: inspections fk_inspections_offering; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inspections
    ADD CONSTRAINT fk_inspections_offering FOREIGN KEY (offering_id) REFERENCES public.programme_offerings(id);


--
-- Name: contribution_obligations fk_obligations_created_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contribution_obligations
    ADD CONSTRAINT fk_obligations_created_by FOREIGN KEY (created_by) REFERENCES public.app_users(id);


--
-- Name: contribution_obligations fk_obligations_farmer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contribution_obligations
    ADD CONSTRAINT fk_obligations_farmer FOREIGN KEY (farmer_id) REFERENCES public.farmers(id);


--
-- Name: contribution_obligations fk_obligations_season; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contribution_obligations
    ADD CONSTRAINT fk_obligations_season FOREIGN KEY (season_id) REFERENCES public.seasons(id);


--
-- Name: programme_offerings fk_offerings_field_officer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.programme_offerings
    ADD CONSTRAINT fk_offerings_field_officer FOREIGN KEY (field_officer_id) REFERENCES public.app_users(id);


--
-- Name: programme_offerings fk_offerings_programme; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.programme_offerings
    ADD CONSTRAINT fk_offerings_programme FOREIGN KEY (programme_id) REFERENCES public.crop_programmes(id);


--
-- Name: programme_offerings fk_offerings_season; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.programme_offerings
    ADD CONSTRAINT fk_offerings_season FOREIGN KEY (season_id) REFERENCES public.seasons(id);


--
-- Name: payments fk_payments_confirmed_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fk_payments_confirmed_by FOREIGN KEY (confirmed_by) REFERENCES public.app_users(id);


--
-- Name: payments fk_payments_failed_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fk_payments_failed_by FOREIGN KEY (failed_by) REFERENCES public.app_users(id);


--
-- Name: payments fk_payments_obligation; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fk_payments_obligation FOREIGN KEY (obligation_id) REFERENCES public.contribution_obligations(id);


--
-- Name: payments fk_payments_requested_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fk_payments_requested_by FOREIGN KEY (requested_by) REFERENCES public.app_users(id);


--
-- Name: collection_permits fk_permits_enrolment; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collection_permits
    ADD CONSTRAINT fk_permits_enrolment FOREIGN KEY (enrolment_id) REFERENCES public.enrolments(id);


--
-- Name: collection_permits fk_permits_farmer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collection_permits
    ADD CONSTRAINT fk_permits_farmer FOREIGN KEY (farmer_id) REFERENCES public.farmers(id);


--
-- Name: collection_permits fk_permits_issued_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collection_permits
    ADD CONSTRAINT fk_permits_issued_by FOREIGN KEY (issued_by) REFERENCES public.app_users(id);


--
-- Name: programme_prerequisites fk_prereq_programme; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.programme_prerequisites
    ADD CONSTRAINT fk_prereq_programme FOREIGN KEY (programme_id) REFERENCES public.crop_programmes(id);


--
-- Name: programme_prerequisites fk_prereq_training; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.programme_prerequisites
    ADD CONSTRAINT fk_prereq_training FOREIGN KEY (training_id) REFERENCES public.trainings(id);


--
-- Name: inspection_results fk_results_created_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inspection_results
    ADD CONSTRAINT fk_results_created_by FOREIGN KEY (created_by) REFERENCES public.app_users(id);


--
-- Name: inspection_results fk_results_enrolment; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inspection_results
    ADD CONSTRAINT fk_results_enrolment FOREIGN KEY (enrolment_id) REFERENCES public.enrolments(id);


--
-- Name: inspection_results fk_results_published_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inspection_results
    ADD CONSTRAINT fk_results_published_by FOREIGN KEY (published_by) REFERENCES public.app_users(id);


--
-- Name: inspection_results fk_results_submitted_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inspection_results
    ADD CONSTRAINT fk_results_submitted_by FOREIGN KEY (submitted_by) REFERENCES public.app_users(id);


--
-- Name: inspection_scores fk_scores_entered_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inspection_scores
    ADD CONSTRAINT fk_scores_entered_by FOREIGN KEY (entered_by) REFERENCES public.app_users(id);


--
-- Name: inspection_scores fk_scores_inspection; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inspection_scores
    ADD CONSTRAINT fk_scores_inspection FOREIGN KEY (inspection_id) REFERENCES public.inspections(id);


--
-- Name: inspection_scores fk_scores_result; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inspection_scores
    ADD CONSTRAINT fk_scores_result FOREIGN KEY (result_id) REFERENCES public.inspection_results(id);


--
-- PostgreSQL database dump complete
--

\unrestrict AwWY8jfuPYqLghng3C88A1eTxEwaUDMx4XTCsSl0W2n6PnzsLQYRZDLbeUaPhSe

