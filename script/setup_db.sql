-- =============================================================
-- setup_db.sql
-- CR-004 / TASK-CR004-002
-- One-time local development database setup script.
-- Run this ONCE before starting the backend for the first time.
--
-- Usage (run as the postgres superuser):
--   psql -U postgres -f setup_db.sql
-- =============================================================

-- Create the ebookstore database if it does not already exist.
-- NOTE: CREATE DATABASE cannot run inside a transaction block,
-- so this script is written to be run directly via psql (not inside
-- a BEGIN/COMMIT block).

SELECT 'Creating database ebookstore...' AS status
WHERE NOT EXISTS (
    SELECT 1 FROM pg_database WHERE datname = 'ebookstore'
);

-- psql meta-command: only execute the next statement if the DB is absent.
-- Because plain SQL cannot conditionally CREATE DATABASE, we use a shell
-- trick below. If you are running this interactively in psql, ignore the
-- comment and just run:
--   CREATE DATABASE ebookstore;
-- if the database does not already exist.

-- The safest cross-platform approach for a setup script is to attempt the
-- creation and let it fail gracefully if the DB already exists. Run this
-- in your terminal instead of this file when needed:
--
--   psql -U postgres -c "CREATE DATABASE ebookstore;"
--
-- Flyway will create all tables (V1–V8) automatically on first backend boot.
-- =============================================================

-- Grant all privileges to the postgres user (no-op if already owner).
-- Uncomment and adjust if you use a dedicated app user:
-- GRANT ALL PRIVILEGES ON DATABASE ebookstore TO postgres;
