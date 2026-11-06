-- Intento de un estudiante en una evaluación (HU64-T3, ADR-8 D11): guarda cuándo empezó. El cierre por tiempo es
-- perezoso: no hay columna de estado ni tarea programada; se compara la hora actual con este inicio más el límite de
-- minutos de la evaluación, que está en admin-api, al entrar y al enviar.
-- evaluacion_id no tiene clave foránea: la evaluación vive en admin_schema (ADR-1).
CREATE TABLE app_schema.app_intento_evaluacion (
    id             BIGINT GENERATED ALWAYS AS IDENTITY,
    estudiante_id  BIGINT      NOT NULL,
    evaluacion_id  BIGINT      NOT NULL,
    fecha_inicio   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_app_intento_evaluacion PRIMARY KEY (id),
    -- Sin ON DELETE CASCADE: el intento no debe borrarse en silencio.
    CONSTRAINT fk_app_intento_evaluacion_estudiante FOREIGN KEY (estudiante_id)
        REFERENCES app_schema.app_estudiante (id),
    -- Un solo intento por estudiante y evaluación: el reloj corre desde la primera vez y no se reinicia al volver a
    -- entrar. El UNIQUE también sirve para buscarlo.
    CONSTRAINT uq_app_intento_evaluacion_estudiante_evaluacion UNIQUE (estudiante_id, evaluacion_id)
);
