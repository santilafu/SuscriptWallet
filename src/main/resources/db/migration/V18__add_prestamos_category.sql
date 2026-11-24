-- ============================================================
-- V18 — Categoría "Préstamos"
--
-- Para hipotecas, préstamos personales o de coche, financiaciones de compras, tarjetas
-- revolving y pago a plazos (Klarna, seQura…). No va en "Finanzas": una cuota de 80-300 €
-- se comería el gráfico de gasto por categoría y mezclaría conceptos.
--
-- Igual que V16: la tabla no tiene UNIQUE(name), así que se comprueba por nombre antes
-- de insertar para que la migración sea idempotente aunque alguien ya hubiese creado
-- la categoría a mano. El nombre debe coincidir EXACTAMENTE con
-- CategoryMappingService.NAME_TO_KEY ("Préstamos" → "prestamos") y con el mapa
-- NOMBRE_A_CLAVE de SuscripcionFormViewModel (shared).
--
-- Color #1e3a8a (blue-900, azul marino "de banco"): ningún otro seed lo usa y se distingue
-- del azul de Software (#3b82f6) y del índigo de Gaming (#6366f1) en el gráfico del dashboard.
--
-- Cómo revertirla a mano (Flyway Community no tiene undo):
--   1. Comprobar que ninguna suscripción la usa:
--        SELECT count(*) FROM subscriptions s JOIN categories c ON c.id = s.category_id
--        WHERE c.name = 'Préstamos';
--   2. Si da 0: DELETE FROM categories WHERE name = 'Préstamos';
--   3. DELETE FROM flyway_schema_history WHERE version = '18';
-- ============================================================

INSERT INTO categories (name, color, icon)
SELECT 'Préstamos', '#1e3a8a', '🏦'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Préstamos');
