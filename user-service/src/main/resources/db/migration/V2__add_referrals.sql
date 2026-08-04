ALTER TABLE user_data
    ADD COLUMN referral_code VARCHAR(10) UNIQUE,
ADD COLUMN referred_by_id UUID,
ADD COLUMN streak_freezers INT NOT NULL DEFAULT 0,
ADD CONSTRAINT fk_user_referred_by FOREIGN KEY (referred_by_id) REFERENCES user_data(uuid);

CREATE INDEX idx_user_data_referral_code ON user_data(referral_code);

CREATE TABLE referrals (
                           id UUID PRIMARY KEY,
                           inviter_id UUID NOT NULL,
                           invitee_id UUID NOT NULL UNIQUE,
                           status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, COMPLETED, REJECTED
                           created_at TIMESTAMP NOT NULL,
                           completed_at TIMESTAMP,
                           CONSTRAINT fk_referrals_inviter FOREIGN KEY (inviter_id) REFERENCES user_data(uuid),
                           CONSTRAINT fk_referrals_invitee FOREIGN KEY (invitee_id) REFERENCES user_data(uuid)
);

CREATE TABLE milestones (
                            id SERIAL PRIMARY KEY,
                            required_invites INT NOT NULL UNIQUE,
                            reward_name VARCHAR(255) NOT NULL
);

INSERT INTO milestones (required_invites, reward_name) VALUES
                                                           (1, 'Streak Freezer'),
                                                           (3, 'Avatar-Rahmen (Pionier)'),
                                                           (5, 'Double XP Boost (24h)'),
                                                           (10, 'Premium Theme');

CREATE TABLE user_milestones (
                                 user_id UUID NOT NULL,
                                 milestone_id INT NOT NULL,
                                 unlocked_at TIMESTAMP NOT NULL,
                                 PRIMARY KEY (user_id, milestone_id),
                                 CONSTRAINT fk_user_milestones_user FOREIGN KEY (user_id) REFERENCES user_data(uuid),
                                 CONSTRAINT fk_user_milestones_milestone FOREIGN KEY (milestone_id) REFERENCES milestones(id)
);