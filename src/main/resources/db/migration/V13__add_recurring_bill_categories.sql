-- ============================================================
-- V13 — Categorías para recibos recurrentes
--
-- Amplía el concepto de "suscripción" a cualquier cargo periódico: luz, gas, agua,
-- alarma, seguros, telecos y transporte. "Finanzas" y "Educación" ya existen (V5)
-- y se reutilizan para comisiones bancarias, cuota de autónomo, colegios y academias.
--
-- La tabla no tiene UNIQUE(name), así que se comprueba por nombre antes de insertar
-- para que la migración sea idempotente aunque alguien ya hubiese creado la categoría a mano.
-- El nombre debe coincidir EXACTAMENTE con CategoryMappingService.NAME_TO_KEY.
-- ============================================================

INSERT INTO categories (name, color, icon)
SELECT 'Hogar y suministros', '#eab308', '🏠'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Hogar y suministros');

INSERT INTO categories (name, color, icon)
SELECT 'Seguros', '#0d9488', '🛡️'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Seguros');

INSERT INTO categories (name, color, icon)
SELECT 'Telecomunicaciones', '#06b6d4', '📡'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Telecomunicaciones');

INSERT INTO categories (name, color, icon)
SELECT 'Transporte', '#84cc16', '🚌'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Transporte');
