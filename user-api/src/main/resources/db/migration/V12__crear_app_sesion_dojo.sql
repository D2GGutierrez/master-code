-- Sesión del Dojo (HU53-T1): una tanda de práctica libre de ejercicios cortos del celular, de un lenguaje. Guarda los
-- ejercicios que ya se entregaron en ella (para no repetirlos, HU53-T2) y si el estudiante acertó cada uno (el contador
-- de aciertos de la pantalla, HU53-T3).
CREATE TABLE app_schema.app_sesion_dojo (
    id             BIGINT GENERATED ALWAYS AS IDENTITY,
    estudiante_id  BIGINT      NOT NULL,
    lenguaje       VARCHAR(20) NOT NULL,
    fecha_inicio   TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- null mientras la sesión sigue abierta.
    fecha_fin      TIMESTAMPTZ,
    CONSTRAINT pk_app_sesion_dojo PRIMARY KEY (id),
    -- Sin ON DELETE CASCADE: la sesión no debe borrarse en silencio.
    CONSTRAINT fk_app_sesion_dojo_estudiante FOREIGN KEY (estudiante_id)
        REFERENCES app_schema.app_estudiante (id)
);

-- Para encontrar las sesiones de un estudiante (el resumen de la sesión, HU55).
CREATE INDEX ix_app_sesion_dojo_estudiante ON app_schema.app_sesion_dojo (estudiante_id);

-- Un ejercicio entregado dentro de una sesión. problema_id no tiene clave foránea: el problema vive en admin_schema
-- (ADR-1).
CREATE TABLE app_schema.app_sesion_dojo_ejercicio (
    id             BIGINT GENERATED ALWAYS AS IDENTITY,
    sesion_id      BIGINT      NOT NULL,
    problema_id    BIGINT      NOT NULL,
    -- 1 para el primero que se entrega, 2 para el segundo, y así.
    orden          INTEGER     NOT NULL,
    fecha_entrega  TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- null mientras el estudiante no ha respondido; true o false después.
    acerto         BOOLEAN,
    CONSTRAINT pk_app_sesion_dojo_ejercicio PRIMARY KEY (id),
    CONSTRAINT fk_app_sesion_dojo_ejercicio_sesion FOREIGN KEY (sesion_id)
        REFERENCES app_schema.app_sesion_dojo (id),
    CONSTRAINT ck_app_sesion_dojo_ejercicio_orden CHECK (orden >= 1),
    -- Un ejercicio no se entrega dos veces en la misma sesión (CA-53.3): lo garantiza la base, no solo el código. El
    -- UNIQUE también sirve para buscar lo ya entregado.
    CONSTRAINT uq_app_sesion_dojo_ejercicio_problema UNIQUE (sesion_id, problema_id),
    CONSTRAINT uq_app_sesion_dojo_ejercicio_orden UNIQUE (sesion_id, orden)
);
