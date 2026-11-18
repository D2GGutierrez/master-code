-- Secuencia de la partida, respuestas de la partida y ajustes de los avisos (HU58-T1, H9, HU52-T2; ADR-15 D26).

-- HU58-T1 (CA-58.1): los ejercicios de la partida, en su orden. Se guardan al crear el reto, así los dos jugadores reciben
-- los mismos y en el mismo orden (CA-58.2 y CA-58.3). problema_id es solo una referencia lógica: el problema vive en
-- admin_schema (ADR-1). turno empieza en 1.
CREATE TABLE app_schema.app_partida_ejercicio (
    partida_id  BIGINT  NOT NULL,
    turno       INTEGER NOT NULL,
    problema_id BIGINT  NOT NULL,
    CONSTRAINT pk_app_partida_ejercicio PRIMARY KEY (partida_id, turno),
    CONSTRAINT fk_app_partida_ejercicio_partida FOREIGN KEY (partida_id) REFERENCES app_schema.app_partida (id),
    CONSTRAINT ck_app_partida_ejercicio_turno CHECK (turno >= 1),
    -- Un ejercicio no se repite dentro de la misma partida.
    CONSTRAINT uq_app_partida_ejercicio_problema UNIQUE (partida_id, problema_id)
);

-- H9: lo que respondió cada jugador en cada turno. La respuesta no se guarda, solo si acertó: lo único que la partida
-- necesita (el marcador, el ganador y el ranking). El UNIQUE impide responder dos veces el mismo turno aunque lleguen dos
-- peticiones a la vez (HU58-T3, CA-58.4).
CREATE TABLE app_schema.app_partida_respuesta (
    id            BIGINT GENERATED ALWAYS AS IDENTITY,
    partida_id    BIGINT      NOT NULL,
    estudiante_id BIGINT      NOT NULL,
    turno         INTEGER     NOT NULL,
    correcta      BOOLEAN     NOT NULL,
    fecha         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_app_partida_respuesta PRIMARY KEY (id),
    CONSTRAINT fk_app_partida_respuesta_ejercicio FOREIGN KEY (partida_id, turno)
        REFERENCES app_schema.app_partida_ejercicio (partida_id, turno),
    CONSTRAINT fk_app_partida_respuesta_estudiante FOREIGN KEY (estudiante_id) REFERENCES app_schema.app_estudiante (id),
    CONSTRAINT uq_app_partida_respuesta_turno UNIQUE (partida_id, estudiante_id, turno)
);

-- HU52-T2 (CA-52.3 y CA-52.4): los ajustes de los avisos de racha que el estudiante cambia en la app. Sin fila valen los
-- mismos valores por defecto de la app (avisos activados, a las 20:00). hora es hora local del estudiante. ultimo_aviso es
-- el día local en que ya se le avisó: así no se le avisa dos veces el mismo día.
CREATE TABLE app_schema.app_aviso_ajuste (
    estudiante_id BIGINT  NOT NULL,
    activados     BOOLEAN NOT NULL DEFAULT TRUE,
    hora          TIME    NOT NULL DEFAULT '20:00',
    ultimo_aviso  DATE,
    CONSTRAINT pk_app_aviso_ajuste PRIMARY KEY (estudiante_id),
    CONSTRAINT fk_app_aviso_ajuste_estudiante FOREIGN KEY (estudiante_id) REFERENCES app_schema.app_estudiante (id)
);
