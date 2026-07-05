CREATE TABLE quest (
    uuid UUID PRIMARY KEY,
    creator_uuid UUID,
    title VARCHAR(255),
    description VARCHAR(255),
    likes INT NOT NULL DEFAULT 0,
    dislikes INT NOT NULL DEFAULT 0,
    creator_username VARCHAR(255),
    score INT NOT NULL DEFAULT 0,
    video_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

CREATE TABLE daily_quest_assignment (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    quest_id UUID NOT NULL,
    assigned_date DATE NOT NULL,
    rerolls_used INT NOT NULL DEFAULT 0,
    max_rerolls INT NOT NULL DEFAULT 3,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT uk_daily_quest_user_date UNIQUE (user_id, assigned_date)
);

CREATE INDEX idx_daily_quest_user_date ON daily_quest_assignment(user_id, assigned_date);
CREATE INDEX idx_daily_quest_quest ON daily_quest_assignment(quest_id);

CREATE TABLE quest_rating (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    quest_id UUID NOT NULL,
    is_like BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_quest_rating_user_quest UNIQUE (user_id, quest_id)
);

CREATE INDEX idx_quest_rating_user ON quest_rating(user_id);
CREATE INDEX idx_quest_rating_quest ON quest_rating(quest_id);
CREATE INDEX idx_quest_rating_like ON quest_rating(user_id, is_like);
