CREATE TABLE comments (
    id UUID PRIMARY KEY,
    version BIGINT,
    content VARCHAR(2000) NOT NULL,
    entity_id UUID NOT NULL,
    entity_type VARCHAR(255) NOT NULL,
    user_uuid UUID NOT NULL,
    username VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    parent_comment_id UUID,
    likes INT NOT NULL DEFAULT 0,
    depth INT NOT NULL DEFAULT 0,
    reply_count INT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_comment_id) REFERENCES comments(id)
);

CREATE INDEX idx_comment_entity ON comments(entity_id, entity_type);
CREATE INDEX idx_comment_user ON comments(user_uuid);
CREATE INDEX idx_comment_parent ON comments(parent_comment_id);
CREATE INDEX idx_comment_created ON comments(created_at DESC);
CREATE INDEX idx_comment_thread ON comments(entity_id, entity_type, created_at);
CREATE INDEX idx_comment_deleted ON comments(deleted);

CREATE TABLE friendships (
    id UUID PRIMARY KEY,
    version BIGINT,
    user_uuid UUID NOT NULL,
    friend_uuid UUID NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_friendships_user_friend UNIQUE (user_uuid, friend_uuid)
);

CREATE INDEX idx_friendship_user ON friendships(user_uuid);
CREATE INDEX idx_friendship_friend ON friendships(friend_uuid);
CREATE INDEX idx_friendship_status ON friendships(status);
CREATE INDEX idx_friendship_user_status ON friendships(user_uuid, status);
CREATE INDEX idx_friendship_created ON friendships(created_at DESC);

CREATE TABLE hashtags (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    usage_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP,
    last_used_at TIMESTAMP
);

CREATE INDEX idx_hashtag_name ON hashtags(name);
CREATE INDEX idx_hashtag_usage ON hashtags(usage_count DESC);

CREATE TABLE entity_hashtags (
    entity_id UUID NOT NULL,
    hashtag_id UUID NOT NULL,
    entity_type VARCHAR(255),
    PRIMARY KEY (entity_id, hashtag_id)
);
