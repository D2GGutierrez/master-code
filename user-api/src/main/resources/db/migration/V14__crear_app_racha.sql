-- Racha de práctica (HU49-T1): los días seguidos en que el estudiante practicó un lenguaje. Una fila por estudiante y
-- lenguaje. La fecha de última actividad (un día, sin hora) permite decidir si hoy continúa la racha, se mantiene o se
-- reinicia; quien actualiza la fila es HU49-T2.
CREATE TABLE app_schema.app_racha (
    id                       BIGINT GENERATED ALWAYS AS IDENTITY,
    estudiante_id            BIGINT      NOT NULL,
    lenguaje                 VARCHAR(20) NOT NULL,
    racha_actual             INTEGER     NOT NULL DEFAULT 0,
    racha_maxima             INTEGER     NOT NULL DEFAULT 0,
    fecha_ultima_actividad   DATE,
    CONSTRAINT pk_app_racha PRIMARY KEY (id),
    -- Sin ON DELETE CASCADE: la racha no debe borrarse en silencio.
    CONSTRAINT fk_app_racha_estudiante FOREIGN KEY (estudiante_id)
        REFERENCES app_schema.app_estudiante (id),
    CONSTRAINT ck_app_racha_actual CHECK (racha_actual >= 0),
    -- La máxima nunca es menor que la actual.
    CONSTRAINT ck_app_racha_maxima CHECK (racha_maxima >= racha_actual),
    CONSTRAINT uq_app_racha_estudiante_lenguaje UNIQUE (estudiante_id, lenguaje)
);
