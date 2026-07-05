CREATE TABLE user_data (
    uuid UUID PRIMARY KEY,
    created_at TIMESTAMP,
    username VARCHAR(255) NOT NULL UNIQUE,
    interactions INT NOT NULL DEFAULT 0,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    punishment VARCHAR(255),
    left_rerolls INT NOT NULL DEFAULT 0,
    last_reroll TIMESTAMP,
    password_reset_token VARCHAR(255),
    last_login TIMESTAMP,
    password_reset_token_expiry TIMESTAMP,
    verification_code VARCHAR(255),
    verification_expiration TIMESTAMP,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    followers INT NOT NULL DEFAULT 0,
    profile_picture_url VARCHAR(255),
    admin_comment VARCHAR(255)
);

CREATE TABLE user_badge (
    user_id UUID NOT NULL,
    badges UUID,
    CONSTRAINT fk_user_badge_user FOREIGN KEY (user_id) REFERENCES user_data(uuid)
);

CREATE TABLE user_done_quest (
    user_id UUID NOT NULL,
    done_quests UUID,
    CONSTRAINT fk_user_done_quest_user FOREIGN KEY (user_id) REFERENCES user_data(uuid)
);

CREATE TABLE user_authorities (
    user_id UUID NOT NULL,
    authority VARCHAR(255),
    CONSTRAINT fk_user_auth_user FOREIGN KEY (user_id) REFERENCES user_data(uuid)
);

CREATE TABLE follow (
    user_id UUID NOT NULL,
    followed_id UUID NOT NULL,
    timestamp TIMESTAMP,
    PRIMARY KEY (user_id, followed_id),
    CONSTRAINT fk_follow_user FOREIGN KEY (user_id) REFERENCES user_data(uuid),
    CONSTRAINT fk_follow_followed FOREIGN KEY (followed_id) REFERENCES user_data(uuid)
);

CREATE TABLE badge (
    id UUID PRIMARY KEY,
    name VARCHAR(255),
    description VARCHAR(255),
    image BYTEA
);

CREATE TABLE badge_user_ids (
    badge_id UUID NOT NULL,
    user_id UUID,
    CONSTRAINT fk_badge_user_ids_badge FOREIGN KEY (badge_id) REFERENCES badge(id)
);
