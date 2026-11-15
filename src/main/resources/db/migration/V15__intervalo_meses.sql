-- ============================================================
-- V15 — Periodicidad de pago: cada cuántos meses se cobra un gasto
--
-- Añade `intervalo_meses` (1 mensual, 2 bimestral, 3 trimestral, 6 semestral, 12 anual).
-- `billing_cycle` se mantiene y pasa a ser derivado del intervalo (12 → YEARLY, resto →
-- MONTHLY) para que las apps antiguas sigan funcionando; WEEKLY guarda 1 y lo ignora.
-- Los datos existentes no cambian de importe: MONTHLY y WEEKLY quedan con 1 (el DEFAULT) y
-- YEARLY con 12, que da exactamente el mismo gasto mensual y anual que antes.
--
-- CÓMO REVERTIRLA A MANO (no hay migraciones "down" en Flyway Community):
--   ALTER TABLE subscriptions DROP CONSTRAINT chk_subscriptions_intervalo_meses;
--   ALTER TABLE subscriptions DROP COLUMN intervalo_meses;
--   DELETE FROM flyway_schema_history WHERE version = '15';
-- y desplegar una versión del backend que no tenga el campo `intervaloMeses` en la entidad
-- (con ddl-auto=none Hibernate fallaría al leer una columna que ya no existe).
-- ============================================================

ALTER TABLE subscriptions ADD COLUMN intervalo_meses INTEGER NOT NULL DEFAULT 1;

UPDATE subscriptions SET intervalo_meses = 12 WHERE billing_cycle = 'YEARLY';

ALTER TABLE subscriptions ADD CONSTRAINT chk_subscriptions_intervalo_meses
    CHECK (intervalo_meses IN (1, 2, 3, 6, 12));
