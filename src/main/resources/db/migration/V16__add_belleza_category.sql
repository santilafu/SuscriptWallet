-- ============================================================
-- V16 — Categoría "Belleza"
--
-- Para cargos periódicos de cosmética y cuidado personal: cremas, maquillaje,
-- cajas de belleza por suscripción y servicios recurrentes de peluquería o depilación.
--
-- Igual que V13: la tabla no tiene UNIQUE(name), así que se comprueba por nombre antes
-- de insertar para que la migración sea idempotente aunque alguien ya hubiese creado
-- la categoría a mano. El nombre debe coincidir EXACTAMENTE con
-- CategoryMappingService.NAME_TO_KEY ("Belleza" → "belleza") y con el mapa
-- NOMBRE_A_CLAVE de SuscripcionFormViewModel (shared).
--
-- Color #c026d3 (fuchsia-600): ningún otro seed lo usa y se distingue en el gráfico del
-- dashboard de "Creatividad y foto" (#ec4899) y "Citas y social" (#f43f5e).
--
-- Cómo revertirla a mano (Flyway Community no tiene undo):
--   1. Comprobar que ninguna suscripción la usa:
--        SELECT count(*) FROM subscriptions s JOIN categories c ON c.id = s.category_id
--        WHERE c.name = 'Belleza';
--   2. Si da 0: DELETE FROM categories WHERE name = 'Belleza';
--   3. DELETE FROM flyway_schema_history WHERE version = '16';
-- ============================================================

INSERT INTO categories (name, color, icon)
SELECT 'Belleza', '#c026d3', '💄'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Belleza');
