-- Cada envío de un estudiante para resolver un problema.
-- problema_id no tiene clave foránea: el problema vive en admin_schema (ADR-1).
-- plataforma va desde el inicio para saber desde dónde se resolvió (HU24).
CREATE TABLE app_schema.app_envio (
    id            BIGINT GENERATED ALWAYS AS IDENTITY,
    estudiante_id BIGINT      NOT NULL,
    problema_id   BIGINT      NOT NULL,
    plataforma    VARCHAR(10) NOT NULL,
    fecha_envio   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_app_envio PRIMARY KEY (id),
    -- Sin ON DELETE CASCADE: el historial no debe borrarse en silencio.
    CONSTRAINT fk_app_envio_estudiante FOREIGN KEY (estudiante_id)
        REFERENCES app_schema.app_estudiante (id),
    CONSTRAINT ck_app_envio_plataforma CHECK (plataforma IN ('web', 'movil'))
);
