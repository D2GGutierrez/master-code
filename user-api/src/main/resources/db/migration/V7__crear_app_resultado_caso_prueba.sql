-- Resultado de cada caso de prueba evaluado en un envío (HU37): una fila por caso, con si pasó y lo que imprimió el
-- programa. Guarda también la salida de los casos ocultos: nada que lea esta tabla debe devolverla al estudiante (HU39).
-- caso_prueba_id no tiene clave foránea: el caso vive en admin_schema (ADR-1).
CREATE TABLE app_schema.app_resultado_caso_prueba (
    id              BIGINT GENERATED ALWAYS AS IDENTITY,
    envio_id        BIGINT  NOT NULL,
    caso_prueba_id  BIGINT  NOT NULL,
    paso            BOOLEAN NOT NULL,
    -- Lo que imprimió el programa, recortado por user-api; vacío si no imprimió nada.
    salida_obtenida TEXT    NOT NULL DEFAULT '',
    CONSTRAINT pk_app_resultado_caso_prueba PRIMARY KEY (id),
    -- Sin ON DELETE CASCADE: el resultado no debe borrarse en silencio.
    CONSTRAINT fk_app_resultado_caso_prueba_envio FOREIGN KEY (envio_id)
        REFERENCES app_schema.app_envio (id),
    -- Un caso se evalúa una sola vez por envío; el UNIQUE también sirve para buscar los resultados de un envío.
    CONSTRAINT uq_app_resultado_caso_prueba_envio_caso UNIQUE (envio_id, caso_prueba_id)
);
