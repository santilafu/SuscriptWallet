CREATE TABLE password_reset_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    BIGINT NOT NULL REFERENCES users(id),
    token      VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_prt_token   ON password_reset_tokens(token);
CREATE INDEX idx_prt_user    ON password_reset_tokens(user_id);
