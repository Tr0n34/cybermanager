#!/bin/sh
set -eu

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
    CREATE EXTENSION IF NOT EXISTS pgcrypto;

    CREATE SCHEMA IF NOT EXISTS cybermanager AUTHORIZATION ${POSTGRES_USER};

    ALTER ROLE ${POSTGRES_USER} IN DATABASE ${POSTGRES_DB} SET search_path TO cybermanager, public;
    ALTER DATABASE ${POSTGRES_DB} SET search_path TO cybermanager, public;
    ALTER DATABASE ${POSTGRES_DB} SET timezone TO 'Europe/Paris';
EOSQL
