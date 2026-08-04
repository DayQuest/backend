-- V2: Scalability Improvements Migration
-- This migration adds indexes, new columns and optimizations for high-scale operations

-- =============================================================================
-- USER DATA IMPROVEMENTS
-- =============================================================================

-- Add version column for optimistic locking
ALTER TABLE user_data ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- Add missing indexes
CREATE INDEX IF NOT EXISTS idx_user_created_at ON user_data(created_at);
CREATE INDEX IF NOT EXISTS idx_user_last_login ON user_data(last_login);
CREATE INDEX IF NOT EXISTS idx_user_followers ON user_data(followers DESC);

-- =============================================================================
-- VIDEO IMPROVEMENTS
-- =============================================================================

-- Add new columns
ALTER TABLE videos ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE videos ADD COLUMN IF NOT EXISTS score INT DEFAULT 0;
ALTER TABLE videos ADD COLUMN IF NOT EXISTS uploader_username VARCHAR(255);
ALTER TABLE videos ADD COLUMN IF NOT EXISTS quest_title VARCHAR(255);
ALTER TABLE videos ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE videos ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- Update score based on existing data
UPDATE videos SET score = up_votes - down_votes WHERE score = 0 OR score IS NULL;

-- Add indexes
CREATE INDEX IF NOT EXISTS idx_video_user ON videos(user_uuid);
CREATE INDEX IF NOT EXISTS idx_video_quest ON videos(quest_uuid);
CREATE INDEX IF NOT EXISTS idx_video_status ON videos(status);
CREATE INDEX IF NOT EXISTS idx_video_created ON videos(created_at);
CREATE INDEX IF NOT EXISTS idx_video_status_created ON videos(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_video_user_status ON videos(user_uuid, status);
CREATE INDEX IF NOT EXISTS idx_video_score ON videos(score DESC);
CREATE INDEX IF NOT EXISTS idx_video_deleted ON videos(deleted);

-- =============================================================================
-- QUEST IMPROVEMENTS
-- =============================================================================

-- Add new columns
ALTER TABLE quest ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE quest ADD COLUMN IF NOT EXISTS score INT DEFAULT 0;
ALTER TABLE quest ADD COLUMN IF NOT EXISTS creator_username VARCHAR(255);
ALTER TABLE quest ADD COLUMN IF NOT EXISTS video_count INT DEFAULT 0;
ALTER TABLE quest ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;
ALTER TABLE quest ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE quest ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- Update score based on existing data
UPDATE quest SET score = likes - dislikes WHERE score = 0 OR score IS NULL;

-- Add indexes
CREATE INDEX IF NOT EXISTS idx_quest_creator ON quest(creator_uuid);
CREATE INDEX IF NOT EXISTS idx_quest_created_at ON quest(created_at);
CREATE INDEX IF NOT EXISTS idx_quest_score ON quest(score DESC);
CREATE INDEX IF NOT EXISTS idx_quest_active ON quest(active, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_quest_deleted ON quest(deleted);

-- =============================================================================
-- COMMENT IMPROVEMENTS
-- =============================================================================

-- Add new columns
ALTER TABLE comments ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE comments ADD COLUMN IF NOT EXISTS depth INT DEFAULT 0;
ALTER TABLE comments ADD COLUMN IF NOT EXISTS reply_count INT DEFAULT 0;
ALTER TABLE comments ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE comments ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- Add indexes
CREATE INDEX IF NOT EXISTS idx_comment_entity ON comments(entity_id, entity_type);
CREATE INDEX IF NOT EXISTS idx_comment_user ON comments(user_uuid);
CREATE INDEX IF NOT EXISTS idx_comment_parent ON comments(parent_comment_id);
CREATE INDEX IF NOT EXISTS idx_comment_created ON comments(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_comment_thread ON comments(entity_id, entity_type, created_at);
CREATE INDEX IF NOT EXISTS idx_comment_deleted ON comments(deleted);

-- =============================================================================
-- FRIENDSHIP IMPROVEMENTS
-- =============================================================================

-- Add version column
ALTER TABLE friendships ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- Add unique constraint if not exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'unique_friendship' AND conrelid = 'friendships'::regclass
    ) THEN
        ALTER TABLE friendships ADD CONSTRAINT unique_friendship
        UNIQUE (user_uuid, friend_uuid);
    END IF;
END $$;

-- Add indexes
CREATE INDEX IF NOT EXISTS idx_friendship_user ON friendships(user_uuid);
CREATE INDEX IF NOT EXISTS idx_friendship_friend ON friendships(friend_uuid);
CREATE INDEX IF NOT EXISTS idx_friendship_status ON friendships(status);
CREATE INDEX IF NOT EXISTS idx_friendship_user_status ON friendships(user_uuid, status);
CREATE INDEX IF NOT EXISTS idx_friendship_created ON friendships(created_at DESC);

-- =============================================================================
-- HASHTAG IMPROVEMENTS
-- =============================================================================

-- Add new columns
ALTER TABLE hashtags ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW();
ALTER TABLE hashtags ADD COLUMN IF NOT EXISTS last_used_at TIMESTAMP;

-- Add indexes
CREATE INDEX IF NOT EXISTS idx_hashtag_name ON hashtags(name);
CREATE INDEX IF NOT EXISTS idx_hashtag_usage ON hashtags(usage_count DESC);

-- =============================================================================
-- STREAK IMPROVEMENTS
-- =============================================================================

-- Add indexes
CREATE INDEX IF NOT EXISTS idx_streak_user ON streaks(user_uuid);
CREATE INDEX IF NOT EXISTS idx_streak_current ON streaks(current_streak DESC);
CREATE INDEX IF NOT EXISTS idx_streak_longest ON streaks(longest_streak DESC);
CREATE INDEX IF NOT EXISTS idx_streak_last_activity ON streaks(last_activity_date);

-- =============================================================================
-- REPORT IMPROVEMENTS
-- =============================================================================

-- Add version column
ALTER TABLE reports ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- Add indexes
CREATE INDEX IF NOT EXISTS idx_report_entity ON reports(entity_id);
CREATE INDEX IF NOT EXISTS idx_report_reporter ON reports(reporter_uuid);
CREATE INDEX IF NOT EXISTS idx_report_status ON reports(status);
CREATE INDEX IF NOT EXISTS idx_report_type ON reports(type);
CREATE INDEX IF NOT EXISTS idx_report_created ON reports(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_report_status_created ON reports(status, created_at DESC);

-- =============================================================================
-- ANALYZE TABLES FOR QUERY PLANNER
-- =============================================================================

ANALYZE user_data;
ANALYZE videos;
ANALYZE quest;
ANALYZE comments;
ANALYZE friendships;
ANALYZE hashtags;
ANALYZE streaks;
ANALYZE reports;

