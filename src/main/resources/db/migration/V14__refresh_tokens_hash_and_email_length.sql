-- ============================================================
-- V14 — Endurecimiento de refresh_tokens (auditoría backend 2026-10-10)
--
-- 1) `email` venía de V3 como VARCHAR(64) (columna `username`), pero `users.email` admite 254.
--    Un usuario con un email de más de 64 caracteres no podía iniciar sesión en la app: el
--    INSERT del refresh token fallaba y la API devolvía un 500.
-- 2) M-02: los refresh tokens se guardaban en claro. A partir de ahora la columna `token`
--    guarda el SHA-256 en hexadecimal (TokenService.hash). Se convierten los existentes con la
--    misma función para que las sesiones abiertas de la app sigan funcionando tras el despliegue.
--    sha256() existe en PostgreSQL >= 11.
-- ============================================================

ALTER TABLE refresh_tokens ALTER COLUMN email TYPE VARCHAR(254);

UPDATE refresh_tokens
SET token = encode(sha256(convert_to(token, 'UTF8')), 'hex')
WHERE length(token) <> 64;

-- Índice para la purga programada de tokens caducados (MaintenanceJobs)
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires ON refresh_tokens(expires_at);
CREATE INDEX IF NOT EXISTS idx_security_events_created ON security_events(created_at);
