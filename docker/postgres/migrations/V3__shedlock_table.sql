-- V3: ShedLock table for distributed scheduler locking
-- This ensures scheduled tasks run only once across all service instances

-- Create shedlock table for each database that needs it

-- For content-service database
\c content_db;

CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);

-- Create index for efficient lock queries
CREATE INDEX IF NOT EXISTS idx_shedlock_name ON shedlock(name);

-- For quest-service database (if needed)
\c quest_db;

CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_shedlock_name ON shedlock(name);

-- For notification-service database (if needed)
\c notification_db;

CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_shedlock_name ON shedlock(name);

-- Grant permissions
\c content_db;
GRANT ALL PRIVILEGES ON TABLE shedlock TO dayquest_user;

\c quest_db;
GRANT ALL PRIVILEGES ON TABLE shedlock TO dayquest_user;

\c notification_db;
GRANT ALL PRIVILEGES ON TABLE shedlock TO dayquest_user;
