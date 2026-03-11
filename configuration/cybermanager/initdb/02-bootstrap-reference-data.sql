-- JPA creates application tables at startup.
-- This seed file only guarantees stable reference data useful for local startup.

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_catalog.pg_roles
        WHERE rolname = 'cybermanager_readonly'
    ) THEN
        CREATE ROLE cybermanager_readonly NOLOGIN;
    END IF;
END
$$;
