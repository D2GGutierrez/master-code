-- Dificultad del Dojo (HU54-T3, HU54-T4, regla en docs/regla-dificultad.md).
-- Nivel guardado al terminar una sesión: la siguiente del mismo lenguaje parte de él. Sin fila (estudiante nuevo en ese
-- lenguaje) la sesión parte de «facil» (HU54-T4).
CREATE TABLE app_schema.app_nivel_dificultad (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY,
    estudiante_id        BIGINT      NOT NULL,
    lenguaje             VARCHAR(20) NOT NULL,
    nivel                VARCHAR(10) NOT NULL,
    fecha_actualizacion  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_app_nivel_dificultad PRIMARY KEY (id),
    CONSTRAINT fk_app_nivel_dificultad_estudiante FOREIGN KEY (estudiante_id)
        REFERENCES app_schema.app_estudiante (id),
    CONSTRAINT ck_app_nivel_dificultad_nivel CHECK (nivel IN ('facil', 'medio', 'dificil')),
    CONSTRAINT uq_app_nivel_dificultad_estudiante_lenguaje UNIQUE (estudiante_id, lenguaje)
);

-- El estado de la regla vive en la sesión (nivel y las dos rachas), así sobrevive si la app se cierra y se reanuda.
ALTER TABLE app_schema.app_sesion_dojo
    ADD COLUMN nivel           VARCHAR(10) NOT NULL DEFAULT 'facil',
    ADD COLUMN racha_aciertos  INTEGER     NOT NULL DEFAULT 0,
    ADD COLUMN racha_errores   INTEGER     NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_app_sesion_dojo_nivel CHECK (nivel IN ('facil', 'medio', 'dificil')),
    ADD CONSTRAINT ck_app_sesion_dojo_rachas CHECK (racha_aciertos >= 0 AND racha_errores >= 0);

-- Tema del ejercicio al responderlo (HU56-T1): se apunta aquí para sumar los fallos por tema sin volver a llamar a
-- admin-api. «General» cuando el problema no tiene tema.
ALTER TABLE app_schema.app_sesion_dojo_ejercicio ADD COLUMN tema VARCHAR(60);
