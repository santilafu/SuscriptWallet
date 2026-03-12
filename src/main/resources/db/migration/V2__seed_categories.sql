-- ============================================================
-- V2 — Categorías predefinidas
--
-- Se insertan al arrancar la app por primera vez (base de datos vacía).
-- Cada categoría tiene un nombre, un color hexadecimal y un emoji.
-- ============================================================

INSERT INTO categories (name, color, icon) VALUES ('IA',                  '#8b5cf6', '🤖');
INSERT INTO categories (name, color, icon) VALUES ('Streaming',           '#ef4444', '🎬');
INSERT INTO categories (name, color, icon) VALUES ('Música',              '#22c55e', '🎵');
INSERT INTO categories (name, color, icon) VALUES ('Software',            '#3b82f6', '💻');
INSERT INTO categories (name, color, icon) VALUES ('Cloud',               '#f59e0b', '☁️');
INSERT INTO categories (name, color, icon) VALUES ('Gaming',              '#6366f1', '🎮');
INSERT INTO categories (name, color, icon) VALUES ('Seguridad',           '#f97316', '🔒');
INSERT INTO categories (name, color, icon) VALUES ('Noticias y Lectura',  '#64748b', '📰');
INSERT INTO categories (name, color, icon) VALUES ('Salud y Deporte',     '#10b981', '🏃');
INSERT INTO categories (name, color, icon) VALUES ('Desarrollo',          '#0ea5e9', '🛠️');
