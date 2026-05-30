CREATE TABLE gmail_scan_ticket (
    id          VARCHAR(64) PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    months      INT         NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    used        BOOLEAN     NOT NULL DEFAULT FALSE
);

CREATE TABLE gmail_scan_result (
    id               BIGSERIAL    PRIMARY KEY,
    scan_id          VARCHAR(64)  NOT NULL,
    user_id          BIGINT       NOT NULL,
    service_name     VARCHAR(255) NOT NULL,
    description      VARCHAR(255) NOT NULL DEFAULT '',
    domain           VARCHAR(255) NOT NULL,
    sender_email     VARCHAR(255) NOT NULL,
    last_seen        VARCHAR(32)  NOT NULL,
    price            NUMERIC(12,2) NOT NULL,
    currency         VARCHAR(8)   NOT NULL,
    billing_cycle    VARCHAR(16)  NOT NULL,
    price_from_email BOOLEAN      NOT NULL,
    category_key     VARCHAR(64)  NOT NULL
);

CREATE INDEX idx_gmail_scan_result_user ON gmail_scan_result (user_id);
CREATE INDEX idx_gmail_scan_ticket_expires ON gmail_scan_ticket (expires_at);
