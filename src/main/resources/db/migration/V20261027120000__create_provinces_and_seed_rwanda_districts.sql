-- V20261027120000__create_provinces_and_seed_rwanda_districts.sql
-- Created: 2026-10-27 12:00:00 +02:00
-- Purpose: Rwanda's provinces and its 30 districts are created automatically when the application
--          starts (Flyway runs this once), so nobody has to type them in.
--          Source: the administrative structure in force since 2006: Kigali City and 4 provinces, 30 districts.

CREATE TABLE provinces (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_provinces_name_lower ON provinces (lower(name));

CREATE TRIGGER trg_provinces_updated_at BEFORE UPDATE ON provinces
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

INSERT INTO provinces (name) VALUES
    ('Kigali City'),
    ('Northern Province'),
    ('Southern Province'),
    ('Eastern Province'),
    ('Western Province');

-- Each district belongs to one province. Districts created by hand before this migration that are not
-- official Rwandan districts keep province_id = NULL, so no existing data is lost.
ALTER TABLE districts
    ADD COLUMN province_id BIGINT,
    ADD CONSTRAINT fk_districts_province FOREIGN KEY (province_id) REFERENCES provinces (id);

CREATE INDEX idx_districts_province ON districts (province_id);

CREATE TEMPORARY TABLE official_districts (province VARCHAR(100), district VARCHAR(100)) ON COMMIT DROP;

INSERT INTO official_districts (province, district) VALUES
    ('Kigali City',       'Gasabo'),
    ('Kigali City',       'Kicukiro'),
    ('Kigali City',       'Nyarugenge'),
    ('Northern Province', 'Burera'),
    ('Northern Province', 'Gakenke'),
    ('Northern Province', 'Gicumbi'),
    ('Northern Province', 'Musanze'),
    ('Northern Province', 'Rulindo'),
    ('Southern Province', 'Gisagara'),
    ('Southern Province', 'Huye'),
    ('Southern Province', 'Kamonyi'),
    ('Southern Province', 'Muhanga'),
    ('Southern Province', 'Nyamagabe'),
    ('Southern Province', 'Nyanza'),
    ('Southern Province', 'Nyaruguru'),
    ('Southern Province', 'Ruhango'),
    ('Eastern Province',  'Bugesera'),
    ('Eastern Province',  'Gatsibo'),
    ('Eastern Province',  'Kayonza'),
    ('Eastern Province',  'Kirehe'),
    ('Eastern Province',  'Ngoma'),
    ('Eastern Province',  'Nyagatare'),
    ('Eastern Province',  'Rwamagana'),
    ('Western Province',  'Karongi'),
    ('Western Province',  'Ngororero'),
    ('Western Province',  'Nyabihu'),
    ('Western Province',  'Nyamasheke'),
    ('Western Province',  'Rubavu'),
    ('Western Province',  'Rusizi'),
    ('Western Province',  'Rutsiro');

-- Districts that already exist (for example "Huye" typed in earlier) keep their id, so cooperatives,
-- programmes, farmers and officers linked to them are untouched; they only get their province.
UPDATE districts d
SET province_id = p.id
FROM official_districts o
JOIN provinces p ON p.name = o.province
WHERE lower(d.name) = lower(o.district);

-- The missing official districts are added.
INSERT INTO districts (name, province_id)
SELECT o.district, p.id
FROM official_districts o
JOIN provinces p ON p.name = o.province
WHERE NOT EXISTS (SELECT 1 FROM districts d WHERE lower(d.name) = lower(o.district));
