-- Cinturones del estudiante (HU50-T1): un cinturón por estudiante y lenguaje, que sube de nivel al acumular puntos.
-- Los niveles y los puntos que exige cada uno viven en una tabla de referencia, no en el código: cambiar un umbral es una
-- migración y HU50-T2, HU51-T1 y la app leen los mismos valores. La regla va a docs/puntos-cinturon.md (HU50-T4).
CREATE TABLE app_schema.app_nivel_cinturon (
    nivel              SMALLINT    NOT NULL,
    nombre             VARCHAR(20) NOT NULL,
    -- Puntos acumulados desde los que se tiene este cinturón. El blanco es el primero: se empieza con él.
    puntos_requeridos  INTEGER     NOT NULL,
    CONSTRAINT pk_app_nivel_cinturon PRIMARY KEY (nivel),
    CONSTRAINT uq_app_nivel_cinturon_nombre UNIQUE (nombre),
    -- Dos niveles con los mismos puntos no se podrían distinguir al subir de nivel.
    CONSTRAINT uq_app_nivel_cinturon_puntos UNIQUE (puntos_requeridos),
    CONSTRAINT ck_app_nivel_cinturon_nivel CHECK (nivel >= 1),
    CONSTRAINT ck_app_nivel_cinturon_puntos CHECK (puntos_requeridos >= 0)
);

-- De blanco a negro (CA-50.4). nivel 1 = blanco: todos empiezan ahí, con 0 puntos.
INSERT INTO app_schema.app_nivel_cinturon (nivel, nombre, puntos_requeridos) VALUES
    (1, 'blanco', 0),
    (2, 'amarillo', 100),
    (3, 'naranja', 250),
    (4, 'verde', 500),
    (5, 'azul', 900),
    (6, 'marron', 1400),
    (7, 'negro', 2000);

CREATE TABLE app_schema.app_cinturon (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY,
    estudiante_id        BIGINT      NOT NULL,
    -- python, java o javascript, el mismo valor que el lenguaje del problema; el curso es de admin_schema (ADR-1).
    lenguaje             VARCHAR(20) NOT NULL,
    nivel                SMALLINT    NOT NULL DEFAULT 1,
    puntos               INTEGER     NOT NULL DEFAULT 0,
    fecha_actualizacion  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_app_cinturon PRIMARY KEY (id),
    -- Sin ON DELETE CASCADE: el cinturón no debe borrarse en silencio.
    CONSTRAINT fk_app_cinturon_estudiante FOREIGN KEY (estudiante_id)
        REFERENCES app_schema.app_estudiante (id),
    CONSTRAINT fk_app_cinturon_nivel FOREIGN KEY (nivel)
        REFERENCES app_schema.app_nivel_cinturon (nivel),
    -- Los puntos solo se suman: nunca bajan de 0.
    CONSTRAINT ck_app_cinturon_puntos CHECK (puntos >= 0),
    -- Un solo cinturón por estudiante y lenguaje. El UNIQUE también sirve para buscarlo y para sumar con ON CONFLICT.
    CONSTRAINT uq_app_cinturon_estudiante_lenguaje UNIQUE (estudiante_id, lenguaje)
);
