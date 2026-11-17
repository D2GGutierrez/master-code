-- Partida (HU57-T1, ADR-13 D23): un reto entre dos estudiantes del mismo curso. La fila de app_partida lleva el curso y el
-- estado del reto; cada uno de los dos estudiantes tiene una fila en app_partida_jugador con su marcador y su última señal
-- (D20). Los dos jugadores están inscritos en el curso: eso lo comprueba el código al crear el reto (HU57-T2), no la base,
-- porque el curso vive en admin_schema (ADR-1) y la inscripción es otra tabla.
CREATE TABLE app_schema.app_partida (
    id              BIGINT GENERATED ALWAYS AS IDENTITY,
    -- Sin clave foránea: el curso vive en admin_schema (ADR-1).
    curso_id        BIGINT      NOT NULL,
    -- pendiente: el rival todavía no responde al reto. Si pasan 24 horas se muestra como caducada sin esperar a un
    -- proceso (HU57-T4); rechazada y caducada son finales, igual que terminada.
    estado          VARCHAR(20) NOT NULL DEFAULT 'pendiente',
    fecha_creacion  TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Cuando el rival acepta y la partida empieza (H7).
    fecha_inicio    TIMESTAMPTZ,
    -- Cuando termina por cualquier motivo.
    fecha_fin       TIMESTAMPTZ,
    -- Quién ganó. null mientras no termina y también en un empate (H9 define el criterio).
    ganador_id      BIGINT,
    -- Por qué terminó (D20): completada, o abandono si un jugador pasó el plazo sin volver.
    motivo_fin      VARCHAR(20),
    CONSTRAINT pk_app_partida PRIMARY KEY (id),
    -- Sin ON DELETE CASCADE: una partida no debe borrarse en silencio.
    CONSTRAINT fk_app_partida_ganador FOREIGN KEY (ganador_id) REFERENCES app_schema.app_estudiante (id),
    CONSTRAINT ck_app_partida_estado CHECK (estado IN ('pendiente', 'en_curso', 'terminada', 'rechazada', 'caducada')),
    CONSTRAINT ck_app_partida_motivo_fin CHECK (motivo_fin IS NULL OR motivo_fin IN ('completada', 'abandono')),
    -- Solo una partida terminada tiene ganador y motivo de fin.
    CONSTRAINT ck_app_partida_ganador CHECK (ganador_id IS NULL OR estado = 'terminada'),
    CONSTRAINT ck_app_partida_motivo CHECK (motivo_fin IS NULL OR estado = 'terminada')
);

-- Las partidas de un curso, y las que siguen abiertas (invitaciones por responder y partidas en curso).
CREATE INDEX ix_app_partida_curso ON app_schema.app_partida (curso_id);
CREATE INDEX ix_app_partida_abiertas ON app_schema.app_partida (estado) WHERE estado IN ('pendiente', 'en_curso');

-- Los dos jugadores de una partida. Un estudiante aparece una sola vez por partida; que sean exactamente dos lo cuida el
-- código al crearla (HU57-T2).
CREATE TABLE app_schema.app_partida_jugador (
    id              BIGINT GENERATED ALWAYS AS IDENTITY,
    partida_id      BIGINT      NOT NULL,
    estudiante_id   BIGINT      NOT NULL,
    -- true para quien lanzó el reto, false para el invitado.
    es_retador      BOOLEAN     NOT NULL,
    -- El marcador: puntos de la partida (H9 define cuánto vale cada respuesta) y ejercicios que ya respondió, que también
    -- dice en qué turno de la secuencia va (HU58-T3).
    puntaje         INTEGER     NOT NULL DEFAULT 0,
    respondidos     INTEGER     NOT NULL DEFAULT 0,
    -- Última petición de este jugador sobre la partida (D20): de ahí salen el «rival desconectado» a los 10 s y la derrota
    -- por abandono a los 30 s. null hasta la primera petición.
    ultima_senal    TIMESTAMPTZ,
    CONSTRAINT pk_app_partida_jugador PRIMARY KEY (id),
    CONSTRAINT fk_app_partida_jugador_partida FOREIGN KEY (partida_id) REFERENCES app_schema.app_partida (id),
    CONSTRAINT fk_app_partida_jugador_estudiante FOREIGN KEY (estudiante_id) REFERENCES app_schema.app_estudiante (id),
    CONSTRAINT ck_app_partida_jugador_puntaje CHECK (puntaje >= 0),
    CONSTRAINT ck_app_partida_jugador_respondidos CHECK (respondidos >= 0),
    -- Un estudiante no puede ser los dos jugadores de la misma partida (nadie se reta a sí mismo).
    CONSTRAINT uq_app_partida_jugador_estudiante UNIQUE (partida_id, estudiante_id)
);

-- Un solo retador por partida.
CREATE UNIQUE INDEX ux_app_partida_jugador_retador ON app_schema.app_partida_jugador (partida_id) WHERE es_retador;

-- El historial y las invitaciones de un estudiante (HU60-T1, H7) empiezan por él.
CREATE INDEX ix_app_partida_jugador_estudiante ON app_schema.app_partida_jugador (estudiante_id);
