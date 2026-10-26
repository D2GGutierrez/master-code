-- Resultado de cada envío (HU30-T2).
-- Se adelanta desde HU36-T1: el estado «resuelto» de un problema solo se sabe si el envío dice cómo salió.
-- HU36-T1 completa la tabla con código, puntaje y número de intento y NO debe volver a crear esta columna.
ALTER TABLE app_schema.app_envio ADD COLUMN resultado VARCHAR(20);

-- NULL = envío sin calificar (por ejemplo, los anteriores a esta columna): cuenta como intento, no como resuelto.
-- Hoy solo importan dos valores; HU36 puede ampliar este CHECK si el sandbox necesita distinguir más resultados.
ALTER TABLE app_schema.app_envio
    ADD CONSTRAINT ck_app_envio_resultado CHECK (resultado IN ('correcto', 'incorrecto'));

-- El estado de los problemas (HU30) y el avance (HU24) leen todos los envíos de un estudiante:
-- sin este índice cada listado recorrería la tabla entera.
CREATE INDEX ix_app_envio_estudiante_problema ON app_schema.app_envio (estudiante_id, problema_id);
