-- Se ejecuta una sola vez, cuando el volumen está vacío.
-- admin_schema lo migra Django y app_schema lo migra Spring con Flyway.
CREATE SCHEMA IF NOT EXISTS admin_schema;
CREATE SCHEMA IF NOT EXISTS app_schema;
