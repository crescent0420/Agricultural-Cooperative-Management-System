-- create-database.sql - creates the ACMS login and databases. Safe to run more than once.
-- Normally run through scripts\create-database.bat, which takes the values from .env.
-- By hand (as the PostgreSQL superuser):
--   psql -U postgres -v db_user=acms_user -v db_password=... -v db_name=acms_db -v test_db_name=acms_test -f database/create-database.sql
-- CREATE DATABASE cannot run inside a transaction or a DO block, so each command is built with
-- format() and executed by psql's \gexec only when the object does not exist yet.

SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'db_user', :'db_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'db_user') \gexec

SELECT format('CREATE DATABASE %I OWNER %I', :'db_name', :'db_user')
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'db_name') \gexec

SELECT format('CREATE DATABASE %I OWNER %I', :'test_db_name', :'db_user')
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'test_db_name') \gexec
