-- create-database.sql - one-time setup of the ACMS database and its own login.
-- Run as the PostgreSQL superuser, from the project root:
--   & "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -f database\create-database.sql
-- psql asks for the new password, so it is never written in this file or your shell history.
-- Afterwards, start the application once: Flyway creates every table from the migrations.

\prompt 'Choose a password for acms_user: ' acms_password
CREATE USER acms_user WITH PASSWORD :'acms_password';
CREATE DATABASE acms_db OWNER acms_user;
