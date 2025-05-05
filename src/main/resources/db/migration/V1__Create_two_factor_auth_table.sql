-- Create two_factor_auth table
CREATE TABLE IF NOT EXISTS two_factor_auth (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    secret_key VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES "user" (uuid) ON DELETE CASCADE,
    CONSTRAINT uk_user_id UNIQUE (user_id)
);

-- Add index for faster lookups
CREATE INDEX idx_two_factor_auth_user_id ON two_factor_auth (user_id);
