-- ============================================================
-- V17 — Préstamos y financiaciones: fecha de inicio y de fin (opcionales) de un gasto
--
-- Con `fecha_fin` el gasto pasa a "Terminado" cuando ya no le quedan cobros en [hoy, fin]: se
-- DERIVA al leer (RenewalDates.isTerminada), no se guarda ningún estado. `fecha_inicio` recorta
-- la serie de cobros (no hay cobros antes) y permite calcular las cuotas totales y la barra.
-- Las filas existentes quedan con NULL en las dos columnas: mismo comportamiento que antes.
-- Una fecha de fin pasada se acepta (historial de algo ya terminado).
--
-- CÓMO REVERTIRLA A MANO (no hay migraciones "down" en Flyway Community):
--   ALTER TABLE subscriptions DROP CONSTRAINT chk_subscriptions_fechas;
--   ALTER TABLE subscriptions DROP COLUMN fecha_fin;
--   ALTER TABLE subscriptions DROP COLUMN fecha_inicio;
--   DELETE FROM flyway_schema_history WHERE version = '17';
-- y desplegar una versión del backend sin `fechaInicio`/`fechaFin` en la entidad.
-- ============================================================

ALTER TABLE subscriptions ADD COLUMN fecha_inicio DATE NULL;
ALTER TABLE subscriptions ADD COLUMN fecha_fin DATE NULL;

ALTER TABLE subscriptions ADD CONSTRAINT chk_subscriptions_fechas
    CHECK (fecha_inicio IS NULL OR fecha_fin IS NULL OR fecha_inicio <= fecha_fin);
