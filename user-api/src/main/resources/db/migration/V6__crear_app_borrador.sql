-- Borrador de código de un estudiante en un problema (HU35): lo que escribió y aún no envió.
-- problema_id no tiene clave foránea: el problema vive en admin_schema (ADR-1).
CREATE TABLE app_schema.app_borrador (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    estudiante_id       BIGINT      NOT NULL,
    problema_id         BIGINT      NOT NULL,
    codigo              TEXT        NOT NULL,
    -- Se actualiza en cada autoguardado; la pantalla puede mostrar «guardado hace…» (HU35).
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_app_borrador PRIMARY KEY (id),
    -- Sin ON DELETE CASCADE: el borrador no debe borrarse en silencio.
    CONSTRAINT fk_app_borrador_estudiante FOREIGN KEY (estudiante_id)
        REFERENCES app_schema.app_estudiante (id),
    -- Un solo borrador por estudiante y problema (CA-35.1); el UNIQUE también sirve para buscarlo.
    CONSTRAINT uq_app_borrador_estudiante_problema UNIQUE (estudiante_id, problema_id)
);
