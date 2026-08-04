CREATE TABLE videos (
    uuid UUID PRIMARY KEY,
    version BIGINT,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    file_path VARCHAR(255) NOT NULL,
    thumbnail_path VARCHAR(255),
    user_uuid UUID NOT NULL,
    quest_uuid UUID,
    up_votes INT NOT NULL DEFAULT 0,
    down_votes INT NOT NULL DEFAULT 0,
    views INT NOT NULL DEFAULT 0,
    comments INT NOT NULL DEFAULT 0,
    score INT NOT NULL DEFAULT 0,
    uploader_username VARCHAR(255),
    quest_title VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    status VARCHAR(255) NOT NULL,
    security_level VARCHAR(255) NOT NULL,
    duration REAL NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_video_user ON videos(user_uuid);
CREATE INDEX idx_video_quest ON videos(quest_uuid);
CREATE INDEX idx_video_status ON videos(status);
CREATE INDEX idx_video_created ON videos(created_at);
CREATE INDEX idx_video_status_created ON videos(status, created_at DESC);
CREATE INDEX idx_video_user_status ON videos(user_uuid, status);
CREATE INDEX idx_video_score ON videos(score DESC);

CREATE TABLE video_ratings (
    user_uuid UUID NOT NULL,
    video_uuid UUID NOT NULL,
    is_upvote BOOLEAN NOT NULL,
    PRIMARY KEY (user_uuid, video_uuid)
);

CREATE TABLE viewed_videos (
    user_uuid UUID NOT NULL,
    video_uuid UUID NOT NULL,
    PRIMARY KEY (user_uuid, video_uuid)
);
