CREATE TABLE badges (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255),
    icon_url VARCHAR(255)
);

CREATE TABLE badge_users (
    badge_id UUID NOT NULL,
    user_uuid UUID,
    CONSTRAINT fk_badge_users_badge FOREIGN KEY (badge_id) REFERENCES badges(id)
);

CREATE TABLE reports (
    id UUID PRIMARY KEY,
    version BIGINT,
    description VARCHAR(2000),
    entity_id UUID NOT NULL,
    reporter_uuid UUID NOT NULL,
    type VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL DEFAULT 'OPEN',
    reason VARCHAR(255),
    created_at TIMESTAMP,
    resolved_at TIMESTAMP,
    resolved_by_uuid UUID,
    mod_message VARCHAR(255)
);

CREATE INDEX idx_report_entity ON reports(entity_id);
CREATE INDEX idx_report_reporter ON reports(reporter_uuid);
CREATE INDEX idx_report_status ON reports(status);
CREATE INDEX idx_report_type ON reports(type);
CREATE INDEX idx_report_created ON reports(created_at DESC);
CREATE INDEX idx_report_status_created ON reports(status, created_at DESC);

CREATE TABLE streaks (
    id UUID PRIMARY KEY,
    user_uuid UUID NOT NULL UNIQUE,
    current_streak INT NOT NULL DEFAULT 0,
    longest_streak INT NOT NULL DEFAULT 0,
    last_activity_date TIMESTAMP,
    created_at TIMESTAMP
);

CREATE INDEX idx_streak_user ON streaks(user_uuid);
CREATE INDEX idx_streak_current ON streaks(current_streak DESC);
CREATE INDEX idx_streak_longest ON streaks(longest_streak DESC);
CREATE INDEX idx_streak_last_activity ON streaks(last_activity_date);
