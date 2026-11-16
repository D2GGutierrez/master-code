-- Qué ejercicios ya dieron puntos al cinturón (HU50-T2): cada ejercicio suma solo la primera vez que el estudiante lo
-- resuelve, así repetirlo no infla el cinturón. El UNIQUE hace de candado: la inserción dice si es la primera vez.
-- problema_id no tiene clave foránea: el problema vive en admin_schema (ADR-1).
CREATE TABLE app_schema.app_puntos_ejercicio (
    id             BIGINT GENERATED ALWAYS AS IDENTITY,
    estudiante_id  BIGINT      NOT NULL,
    problema_id    BIGINT      NOT NULL,
    lenguaje       VARCHAR(20) NOT NULL,
    puntos         INTEGER     NOT NULL,
    fecha          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_app_puntos_ejercicio PRIMARY KEY (id),
    -- Sin ON DELETE CASCADE: el registro de puntos no debe borrarse en silencio.
    CONSTRAINT fk_app_puntos_ejercicio_estudiante FOREIGN KEY (estudiante_id)
        REFERENCES app_schema.app_estudiante (id),
    CONSTRAINT ck_app_puntos_ejercicio_puntos CHECK (puntos >= 0),
    CONSTRAINT uq_app_puntos_ejercicio_estudiante_problema UNIQUE (estudiante_id, problema_id)
);
