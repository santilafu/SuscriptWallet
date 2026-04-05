-- user_id nullable primero para permitir que AdminBootstrapListener inserte el admin
ALTER TABLE subscriptions ADD COLUMN user_id BIGINT REFERENCES users(id);
