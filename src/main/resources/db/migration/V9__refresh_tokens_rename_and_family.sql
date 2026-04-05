ALTER TABLE refresh_tokens RENAME COLUMN username TO email;
ALTER TABLE refresh_tokens ADD COLUMN user_id BIGINT REFERENCES users(id);
ALTER TABLE refresh_tokens ADD COLUMN family_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens(family_id);
CREATE INDEX idx_refresh_tokens_user   ON refresh_tokens(user_id);
