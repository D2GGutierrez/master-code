-- Dispositivos del estudiante para las notificaciones push (HU52-T1, ADR-9 D16): guarda el token que Firebase Cloud
-- Messaging le da a la app en cada teléfono. Un estudiante puede tener varios dispositivos.
CREATE TABLE app_schema.app_dispositivo (
    id              BIGINT GENERATED ALWAYS AS IDENTITY,
    estudiante_id   BIGINT      NOT NULL,
    token           TEXT        NOT NULL,
    fecha_registro  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_app_dispositivo PRIMARY KEY (id),
    -- Sin ON DELETE CASCADE: el dispositivo no debe borrarse en silencio.
    CONSTRAINT fk_app_dispositivo_estudiante FOREIGN KEY (estudiante_id)
        REFERENCES app_schema.app_estudiante (id),
    -- Un token es de un solo teléfono: si otro estudiante inicia sesión en ese teléfono, el token cambia de dueño en vez
    -- de repetirse. El UNIQUE también sirve para buscarlo.
    CONSTRAINT uq_app_dispositivo_token UNIQUE (token)
);

-- Para llegar a todos los dispositivos de un estudiante (los avisos de HU52-T2).
CREATE INDEX ix_app_dispositivo_estudiante ON app_schema.app_dispositivo (estudiante_id);
