-- Zona horaria del estudiante (H6): un nombre IANA como America/Lima, que la app manda desde el teléfono. Sirve para
-- decidir qué día es para él cuando se cuenta la racha (HU49-T2). Vacía hasta que la app la envíe por primera vez: mientras
-- tanto se usa la zona por defecto de la configuración (estudiante.zona-horaria-por-defecto).
ALTER TABLE app_schema.app_estudiante ADD COLUMN zona_horaria VARCHAR(50);
