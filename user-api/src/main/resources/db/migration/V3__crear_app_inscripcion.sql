-- Inscripción de un estudiante a un curso (HU26).
-- curso_id no tiene clave foránea: el curso vive en admin_schema (ADR-1).
CREATE TABLE app_schema.app_inscripcion (
    id            BIGINT GENERATED ALWAYS AS IDENTITY,
    estudiante_id BIGINT      NOT NULL,
    curso_id      BIGINT      NOT NULL,
    fecha         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_app_inscripcion PRIMARY KEY (id),
    -- Sin ON DELETE CASCADE: la inscripción no debe borrarse en silencio.
    CONSTRAINT fk_app_inscripcion_estudiante FOREIGN KEY (estudiante_id)
        REFERENCES app_schema.app_estudiante (id),
    -- Un estudiante no puede inscribirse dos veces al mismo curso (CA-026.5).
    CONSTRAINT uq_app_inscripcion_estudiante_curso UNIQUE (estudiante_id, curso_id)
);

-- El docente consulta los inscritos de un curso (HU28); el UNIQUE solo ayuda a buscar por estudiante.
CREATE INDEX ix_app_inscripcion_curso ON app_schema.app_inscripcion (curso_id);
