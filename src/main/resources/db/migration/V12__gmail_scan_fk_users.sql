-- A-06: los tickets y resultados del escaneo de Gmail quedaban huérfanos al borrar la cuenta.
-- Primero se limpian los huérfanos ya existentes (si no, la FK no se puede crear) y después
-- se liga user_id a users(id) con borrado en cascada.
DELETE FROM gmail_scan_ticket WHERE user_id NOT IN (SELECT id FROM users);
DELETE FROM gmail_scan_result WHERE user_id NOT IN (SELECT id FROM users);

ALTER TABLE gmail_scan_ticket
    ADD CONSTRAINT fk_gmail_scan_ticket_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE gmail_scan_result
    ADD CONSTRAINT fk_gmail_scan_result_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

CREATE INDEX idx_gmail_scan_ticket_user ON gmail_scan_ticket (user_id);
