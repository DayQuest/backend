#!/bin/bash
set -e

# Create databases for each microservice
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE dayquest_videos;
    CREATE DATABASE dayquest_social;
    CREATE DATABASE dayquest_content;

    GRANT ALL PRIVILEGES ON DATABASE dayquest TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE dayquest_videos TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE dayquest_social TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE dayquest_content TO $POSTGRES_USER;
EOSQL

