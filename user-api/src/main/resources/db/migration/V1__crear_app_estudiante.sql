-- Perfil del estudiante en user-api. Se identifica por el email del token:
-- es el dato común con admin_schema, sin claves foráneas entre schemas (ADR-1).
CREATE TABLE app_schema.app_estudiante (
    id             BIGINT GENERATED ALWAYS AS IDENTITY,
    email          VARCHAR(254) NOT NULL,
    institucion    VARCHAR(100) NOT NULL,
    fecha_creacion TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_app_estudiante PRIMARY KEY (id),
    CONSTRAINT uq_app_estudiante_email UNIQUE (email),
    -- Solo minúsculas: así el UNIQUE no deja pasar el mismo correo con otras mayúsculas.
    CONSTRAINT ck_app_estudiante_email_minusculas CHECK (email = lower(email))
);
