-- HU36-T1: completa app_envio con lo que guarda un envío oficial. V4 ya agregó resultado; aquí se suman el código
-- enviado, el puntaje y el número de intento, y resultado admite también «parcial» (HU38).
ALTER TABLE app_schema.app_envio ADD COLUMN codigo TEXT;
ALTER TABLE app_schema.app_envio ADD COLUMN puntaje NUMERIC(5, 2);
ALTER TABLE app_schema.app_envio ADD COLUMN numero_intento INTEGER;

-- Un envío «parcial» (algunos casos superados, no todos) cuenta como intento: solo «correcto» deja un problema
-- resuelto (D6 en docs/decisiones-tecnicas-adr.md).
ALTER TABLE app_schema.app_envio DROP CONSTRAINT ck_app_envio_resultado;
ALTER TABLE app_schema.app_envio
    ADD CONSTRAINT ck_app_envio_resultado CHECK (resultado IN ('correcto', 'parcial', 'incorrecto'));
ALTER TABLE app_schema.app_envio
    ADD CONSTRAINT ck_app_envio_puntaje CHECK (puntaje BETWEEN 0 AND 100);

-- Los envíos que ya existían se numeran por orden de fecha dentro de cada estudiante y problema.
UPDATE app_schema.app_envio e
SET numero_intento = n.numero
FROM (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY estudiante_id, problema_id ORDER BY fecha_envio, id) AS numero
    FROM app_schema.app_envio
) n
WHERE e.id = n.id;

-- Dos envíos del mismo estudiante al mismo problema no pueden compartir número: si dos llegan a la vez, la base
-- rechaza el segundo y el repositorio lo reintenta con el número siguiente. Las filas sin número (NULL, anteriores
-- o sembradas sin calificar) no chocan entre sí.
CREATE UNIQUE INDEX ux_app_envio_intento ON app_schema.app_envio (estudiante_id, problema_id, numero_intento);
