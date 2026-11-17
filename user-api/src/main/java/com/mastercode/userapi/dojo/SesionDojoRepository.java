package com.mastercode.userapi.dojo;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// SQL directo con JdbcClient, igual que DispositivoRepository. Tablas: app_sesion_dojo y app_sesion_dojo_ejercicio (V12),
// app_nivel_dificultad y las columnas de nivel, rachas y tema (V15).
@Repository
public class SesionDojoRepository {

    // abierta: la sesión no tiene fecha de fin. nivel y rachas: el estado de la regla de dificultad (HU54).
    public record Sesion(long id, long estudianteId, String lenguaje, boolean abierta, String nivel, int rachaAciertos,
            int rachaErrores) {

        public ReglaDificultad.Estado estado() {
            return new ReglaDificultad.Estado(Nivel.de(nivel).orElse(Nivel.INICIAL), rachaAciertos, rachaErrores);
        }
    }

    // Los fallos de un tema en una sesión (HU56-T1).
    public record TemaFallado(String tema, int fallos) {
    }

    private final JdbcClient jdbc;

    public SesionDojoRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // La sesión empieza en el nivel dado, con las dos rachas en 0.
    public long crear(long estudianteId, String lenguaje, Nivel nivel) {
        return jdbc.sql("""
                INSERT INTO app_schema.app_sesion_dojo (estudiante_id, lenguaje, nivel) VALUES (?, ?, ?) RETURNING id
                """)
                .params(estudianteId, lenguaje, nivel.valor())
                .query(Long.class)
                .single();
    }

    // Solo la sesión del propio estudiante: la de otro es como si no existiera.
    public Optional<Sesion> buscar(long sesionId, long estudianteId) {
        return jdbc.sql("""
                SELECT id, estudiante_id, lenguaje, fecha_fin IS NULL AS abierta, nivel, racha_aciertos, racha_errores
                FROM app_schema.app_sesion_dojo
                WHERE id = ? AND estudiante_id = ?
                """)
                .params(sesionId, estudianteId)
                .query(Sesion.class)
                .optional();
    }

    // Los problemas que ya se entregaron en la sesión.
    public Set<Long> entregados(long sesionId) {
        return jdbc.sql("SELECT problema_id FROM app_schema.app_sesion_dojo_ejercicio WHERE sesion_id = ?")
                .param(sesionId)
                .query(Long.class)
                .list()
                .stream()
                .collect(Collectors.toSet());
    }

    // Apunta la entrega con el siguiente número de orden. true si quedó guardada; false si ese problema ya estaba entregado
    // o si otra petición a la vez tomó el mismo número de orden (el ON CONFLICT sin destino cubre los dos UNIQUE de V12).
    public boolean registrarEntrega(long sesionId, long problemaId) {
        return jdbc.sql("""
                INSERT INTO app_schema.app_sesion_dojo_ejercicio (sesion_id, problema_id, orden)
                VALUES (:sesion, :problema,
                        (SELECT coalesce(max(orden), 0) + 1 FROM app_schema.app_sesion_dojo_ejercicio WHERE sesion_id = :sesion))
                ON CONFLICT DO NOTHING
                """)
                .param("sesion", sesionId)
                .param("problema", problemaId)
                .update() == 1;
    }

    // El nivel que dejó el estudiante al terminar su última sesión de ese lenguaje (HU54-T3); vacío si es nuevo (HU54-T4).
    public Optional<Nivel> nivelGuardado(long estudianteId, String lenguaje) {
        return jdbc.sql("SELECT nivel FROM app_schema.app_nivel_dificultad WHERE estudiante_id = ? AND lenguaje = ?")
                .params(estudianteId, lenguaje)
                .query(String.class)
                .optional()
                .flatMap(Nivel::de);
    }

    // true si el ejercicio se entregó en la sesión y todavía no se respondió.
    public boolean pendiente(long sesionId, long problemaId) {
        return jdbc.sql("""
                SELECT count(*) FROM app_schema.app_sesion_dojo_ejercicio
                WHERE sesion_id = ? AND problema_id = ? AND acerto IS NULL
                """)
                .params(sesionId, problemaId)
                .query(Long.class)
                .single() > 0;
    }

    // Apunta si acertó y el tema, solo si seguía sin responder: false si ya tenía respuesta (dos respuestas a la vez) o no
    // se entregó en la sesión.
    public boolean registrarRespuesta(long sesionId, long problemaId, boolean acerto, String tema) {
        return jdbc.sql("""
                UPDATE app_schema.app_sesion_dojo_ejercicio SET acerto = ?, tema = ?
                WHERE sesion_id = ? AND problema_id = ? AND acerto IS NULL
                """)
                .params(acerto, tema, sesionId, problemaId)
                .update() == 1;
    }

    public void guardarEstado(long sesionId, ReglaDificultad.Estado estado) {
        jdbc.sql("""
                UPDATE app_schema.app_sesion_dojo SET nivel = ?, racha_aciertos = ?, racha_errores = ? WHERE id = ?
                """)
                .params(estado.nivel().valor(), estado.rachaAciertos(), estado.rachaErrores(), sesionId)
                .update();
    }

    // Cierra la sesión. false si ya estaba cerrada (dos cierres a la vez: solo uno gana).
    public boolean cerrar(long sesionId) {
        return jdbc.sql("UPDATE app_schema.app_sesion_dojo SET fecha_fin = now() WHERE id = ? AND fecha_fin IS NULL")
                .param(sesionId)
                .update() == 1;
    }

    // Deja el nivel como el que parte la próxima sesión del lenguaje (HU54-T3).
    public void guardarNivel(long estudianteId, String lenguaje, Nivel nivel) {
        jdbc.sql("""
                INSERT INTO app_schema.app_nivel_dificultad (estudiante_id, lenguaje, nivel) VALUES (?, ?, ?)
                ON CONFLICT (estudiante_id, lenguaje) DO UPDATE SET nivel = EXCLUDED.nivel, fecha_actualizacion = now()
                """)
                .params(estudianteId, lenguaje, nivel.valor())
                .update();
    }

    public int respondidos(long sesionId) {
        return contar(sesionId, "acerto IS NOT NULL");
    }

    public int aciertos(long sesionId) {
        return contar(sesionId, "acerto");
    }

    private int contar(long sesionId, String condicion) {
        return jdbc.sql("SELECT count(*) FROM app_schema.app_sesion_dojo_ejercicio WHERE sesion_id = ? AND " + condicion)
                .param(sesionId)
                .query(Integer.class)
                .single();
    }

    // HU56-T1: los temas con más fallos en la sesión, de más a menos; a igual cantidad, por nombre. Sin tema cuenta como
    // «General» (D13).
    public List<TemaFallado> temasMasFallados(long sesionId, int limite) {
        return jdbc.sql("""
                SELECT coalesce(nullif(trim(tema), ''), 'General') AS tema, count(*)::int AS fallos
                FROM app_schema.app_sesion_dojo_ejercicio
                WHERE sesion_id = ? AND acerto = false
                GROUP BY 1
                ORDER BY fallos DESC, tema
                LIMIT ?
                """)
                .params(sesionId, limite)
                .query(TemaFallado.class)
                .list();
    }

    // HU56-T3: los puntos que esta sesión sumó al cinturón. Se suman al acertar (HU50-T2) y solo la primera vez que el
    // estudiante resuelve el ejercicio, por eso se leen del registro de puntos y no se calculan de nuevo.
    public int puntosDeLaSesion(long sesionId) {
        return jdbc.sql("""
                SELECT coalesce(sum(p.puntos), 0)::int
                FROM app_schema.app_sesion_dojo s
                JOIN app_schema.app_sesion_dojo_ejercicio e ON e.sesion_id = s.id AND e.acerto
                JOIN app_schema.app_puntos_ejercicio p ON p.estudiante_id = s.estudiante_id
                                                      AND p.problema_id = e.problema_id AND p.fecha >= s.fecha_inicio
                WHERE s.id = ?
                """)
                .param(sesionId)
                .query(Integer.class)
                .single();
    }
}
