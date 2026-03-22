ALTER TABLE subscriptions
    ADD COLUMN is_trial      BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN trial_ends_at DATE    NULL;

INSERT INTO categories (name, color, icon)
VALUES ('Prueba gratuita', '#f97316', '🆓');
