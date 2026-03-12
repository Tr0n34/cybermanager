#!/bin/sh
set -eu

# Defensive normalization in case environment values are passed with CRLF-originated trailing carriage returns.
POSTGRES_USER_CLEAN=$(printf '%s' "$POSTGRES_USER" | tr -d '\r')
POSTGRES_DB_CLEAN=$(printf '%s' "$POSTGRES_DB" | tr -d '\r')

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER_CLEAN" --dbname "$POSTGRES_DB_CLEAN" <<-EOSQL
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
    CREATE EXTENSION IF NOT EXISTS pgcrypto;

    CREATE SCHEMA IF NOT EXISTS cybermanager AUTHORIZATION ${POSTGRES_USER_CLEAN};

    ALTER ROLE ${POSTGRES_USER_CLEAN} IN DATABASE ${POSTGRES_DB_CLEAN} SET search_path TO cybermanager, public;
    ALTER DATABASE ${POSTGRES_DB_CLEAN} SET search_path TO cybermanager, public;
    ALTER DATABASE ${POSTGRES_DB_CLEAN} SET timezone TO 'Europe/Paris';
EOSQL
