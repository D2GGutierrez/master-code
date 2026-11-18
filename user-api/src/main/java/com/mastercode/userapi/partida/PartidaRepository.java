package com.mastercode.userapi.partida;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// SQL directo con JdbcClient. Tablas: app_partida y app_partida_jugador (V18), app_partida_ejercicio y
// app_partida_respuesta (V19). El estado «caducada» no se guarda: un reto pendiente más viejo que el límite se lee como
// caducado (HU57-T4, D23), así que cada consulta recibe ese límite.
@Repository
public class PartidaRepository {

    // El estado ya es el que se ve hoy (un pendiente vencido sale «caducada»).
    public record Partida(long id, long cursoId, String estado, OffsetDateTime fechaCreacion, OffsetDateTime fechaInicio,
            OffsetDateTime fechaFin, Long ganadorId, String motivoFin) {
    }

    public record Jugador(long estudianteId, String email, boolean esRetador, int puntaje, int respondidos,
            OffsetDateTime ultimaSenal) {
    }

    public record Invitacion(long id, long cursoId, String emailRetador, OffsetDateTime fechaCreacion) {
    }

    public record Companero(long id, String email) {
    }

    public record FilaDeHistorial(long id, long cursoId, String estado, OffsetDateTime fechaInicio, OffsetDateTime fechaFin,
            String motivoFin, Long ganadorId, int miPuntaje, int puntajeRival, String emailRival) {
    }

    public record FilaDeRanking(long posicion, long estudianteId, String email, int jugadas, int ganadas, int empatadas,
            int perdidas, int puntos, double tasaDeVictorias) {
    }

    // Lo que se lee de cada partida: el estado efectivo (caducidad al leer).
    private static final String PARTIDA = """
            SELECT p.id, p.curso_id,
                   CASE WHEN p.estado = 'pendiente' AND p.fecha_creacion < :limite THEN 'caducada' ELSE p.estado END AS estado,
                   p.fecha_creacion, p.fecha_inicio, p.fecha_fin, p.ganador_id, p.motivo_fin
            FROM app_schema.app_partida p
            """;

    private final JdbcClient jdbc;

    public PartidaRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    static OffsetDateTime utc(Instant instante) {
        return OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
    }

    // ---------- crear ----------

    // HU57-T2 y HU58-T1: la partida pendiente, sus dos jugadores (el retador ya con señal) y la secuencia, todo junto.
    public long crear(long cursoId, long retadorId, long rivalId, List<Long> problemas, Instant ahora) {
        long id = jdbc.sql("""
                INSERT INTO app_schema.app_partida (curso_id, fecha_creacion) VALUES (:curso, :ahora) RETURNING id
                """)
                .param("curso", cursoId)
                .param("ahora", utc(ahora))
                .query(Long.class)
                .single();
        jdbc.sql("""
                INSERT INTO app_schema.app_partida_jugador (partida_id, estudiante_id, es_retador, ultima_senal)
                VALUES (:partida, :retador, TRUE, :ahora)
                """)
                .param("partida", id).param("retador", retadorId).param("ahora", utc(ahora)).update();
        jdbc.sql("""
                INSERT INTO app_schema.app_partida_jugador (partida_id, estudiante_id, es_retador)
                VALUES (:partida, :rival, FALSE)
                """)
                .param("partida", id).param("rival", rivalId).update();
        for (int i = 0; i < problemas.size(); i++) {
            jdbc.sql("INSERT INTO app_schema.app_partida_ejercicio (partida_id, turno, problema_id) VALUES (?, ?, ?)")
                    .params(id, i + 1, problemas.get(i)).update();
        }
        return id;
    }

    // ¿Ya hay un reto vivo (por responder o en curso) entre estos dos en este curso, en cualquier sentido?
    public boolean hayRetoVivo(long cursoId, long unoId, long otroId, Instant limiteDeCaducidad) {
        return jdbc.sql("""
                SELECT EXISTS (
                    SELECT 1
                    FROM app_schema.app_partida p
                    JOIN app_schema.app_partida_jugador a ON a.partida_id = p.id AND a.estudiante_id = :uno
                    JOIN app_schema.app_partida_jugador b ON b.partida_id = p.id AND b.estudiante_id = :otro
                    WHERE p.curso_id = :curso
                      AND (p.estado = 'en_curso' OR (p.estado = 'pendiente' AND p.fecha_creacion >= :limite))
                )
                """)
                .param("curso", cursoId).param("uno", unoId).param("otro", otroId).param("limite", utc(limiteDeCaducidad))
                .query(Boolean.class).single();
    }

    // ---------- leer ----------

    public Optional<Partida> buscar(long partidaId, Instant limiteDeCaducidad) {
        return jdbc.sql(PARTIDA + " WHERE p.id = :id")
                .param("id", partidaId).param("limite", utc(limiteDeCaducidad))
                .query(Partida.class).optional();
    }

    // Toma la fila de la partida para que dos peticiones a la vez (los dos jugadores) no se pisen hasta el fin de la
    // transacción actual.
    public void bloquear(long partidaId) {
        jdbc.sql("SELECT id FROM app_schema.app_partida WHERE id = ? FOR UPDATE").param(partidaId).query(Long.class).optional();
    }

    // El retador primero.
    public List<Jugador> jugadores(long partidaId) {
        return jdbc.sql("""
                SELECT j.estudiante_id, e.email, j.es_retador, j.puntaje, j.respondidos, j.ultima_senal
                FROM app_schema.app_partida_jugador j
                JOIN app_schema.app_estudiante e ON e.id = j.estudiante_id
                WHERE j.partida_id = ?
                ORDER BY j.es_retador DESC
                """)
                .param(partidaId).query(Jugador.class).list();
    }

    public int totalDeEjercicios(long partidaId) {
        return jdbc.sql("SELECT count(*) FROM app_schema.app_partida_ejercicio WHERE partida_id = ?")
                .param(partidaId).query(Integer.class).single();
    }

    // El ejercicio de un turno (HU58-T2): el mismo para los dos jugadores.
    public Optional<Long> ejercicioDelTurno(long partidaId, int turno) {
        return jdbc.sql("SELECT problema_id FROM app_schema.app_partida_ejercicio WHERE partida_id = ? AND turno = ?")
                .params(partidaId, turno).query(Long.class).optional();
    }

    // ---------- escribir ----------

    // D20: cada petición de un jugador sobre su partida deja su última señal.
    public void senal(long partidaId, long estudianteId, Instant ahora) {
        jdbc.sql("""
                UPDATE app_schema.app_partida_jugador SET ultima_senal = :ahora
                WHERE partida_id = :partida AND estudiante_id = :estudiante
                """)
                .param("ahora", utc(ahora)).param("partida", partidaId).param("estudiante", estudianteId).update();
    }

    // H7: el rival acepta. Los dos jugadores empiezan con señal de ahora: el retador tiene el plazo completo para abrir la
    // pantalla aunque haya dejado el reto esperando horas.
    public boolean aceptar(long partidaId, Instant ahora) {
        int filas = jdbc.sql("""
                UPDATE app_schema.app_partida SET estado = 'en_curso', fecha_inicio = :ahora
                WHERE id = :id AND estado = 'pendiente'
                """)
                .param("ahora", utc(ahora)).param("id", partidaId).update();
        if (filas == 0) {
            return false;
        }
        jdbc.sql("UPDATE app_schema.app_partida_jugador SET ultima_senal = :ahora WHERE partida_id = :id")
                .param("ahora", utc(ahora)).param("id", partidaId).update();
        return true;
    }

    public boolean rechazar(long partidaId, Instant ahora) {
        return jdbc.sql("""
                UPDATE app_schema.app_partida SET estado = 'rechazada', fecha_fin = :ahora
                WHERE id = :id AND estado = 'pendiente'
                """)
                .param("ahora", utc(ahora)).param("id", partidaId).update() == 1;
    }

    // Termina una partida en curso. ganadorId nulo es un empate.
    public boolean cerrar(long partidaId, Long ganadorId, String motivo, Instant ahora) {
        return jdbc.sql("""
                UPDATE app_schema.app_partida
                SET estado = 'terminada', fecha_fin = :ahora, ganador_id = :ganador, motivo_fin = :motivo
                WHERE id = :id AND estado = 'en_curso'
                """)
                .param("ahora", utc(ahora)).param("ganador", ganadorId).param("motivo", motivo).param("id", partidaId)
                .update() == 1;
    }

    // HU58-T3: false si ese jugador ya respondió ese turno (el UNIQUE lo impide aunque lleguen dos a la vez).
    public boolean registrarRespuesta(long partidaId, long estudianteId, int turno, boolean correcta) {
        return jdbc.sql("""
                INSERT INTO app_schema.app_partida_respuesta (partida_id, estudiante_id, turno, correcta)
                VALUES (:partida, :estudiante, :turno, :correcta)
                ON CONFLICT (partida_id, estudiante_id, turno) DO NOTHING
                """)
                .param("partida", partidaId).param("estudiante", estudianteId).param("turno", turno)
                .param("correcta", correcta).update() == 1;
    }

    // El marcador del jugador: un ejercicio más respondido y los puntos de ese acierto (0 si falló).
    public void sumarAlMarcador(long partidaId, long estudianteId, int puntos) {
        jdbc.sql("""
                UPDATE app_schema.app_partida_jugador SET respondidos = respondidos + 1, puntaje = puntaje + :puntos
                WHERE partida_id = :partida AND estudiante_id = :estudiante
                """)
                .param("puntos", puntos).param("partida", partidaId).param("estudiante", estudianteId).update();
    }

    // ---------- listados ----------

    // H7: los retos que otros le hicieron y siguen por responder, el más reciente primero.
    public List<Invitacion> invitaciones(long estudianteId, Instant limiteDeCaducidad) {
        return jdbc.sql("""
                SELECT p.id, p.curso_id, r.email AS email_retador, p.fecha_creacion
                FROM app_schema.app_partida_jugador yo
                JOIN app_schema.app_partida p ON p.id = yo.partida_id
                JOIN app_schema.app_partida_jugador rj ON rj.partida_id = p.id AND rj.es_retador
                JOIN app_schema.app_estudiante r ON r.id = rj.estudiante_id
                WHERE yo.estudiante_id = :yo AND NOT yo.es_retador
                  AND p.estado = 'pendiente' AND p.fecha_creacion >= :limite
                ORDER BY p.fecha_creacion DESC, p.id DESC
                """)
                .param("yo", estudianteId).param("limite", utc(limiteDeCaducidad))
                .query(Invitacion.class).list();
    }

    // HU57-T3: los compañeros inscritos en el curso, sin el propio estudiante.
    public List<Companero> companeros(long cursoId, long estudianteId) {
        return jdbc.sql("""
                SELECT e.id, e.email
                FROM app_schema.app_inscripcion i
                JOIN app_schema.app_estudiante e ON e.id = i.estudiante_id
                WHERE i.curso_id = :curso AND i.estudiante_id <> :yo
                ORDER BY e.email
                """)
                .param("curso", cursoId).param("yo", estudianteId).query(Companero.class).list();
    }

    // HU60-T1: las partidas en curso o terminadas del estudiante, la más reciente primero.
    public List<FilaDeHistorial> historial(long estudianteId, int limite, int desde) {
        return jdbc.sql("""
                SELECT p.id, p.curso_id, p.estado, p.fecha_inicio, p.fecha_fin, p.motivo_fin, p.ganador_id,
                       yo.puntaje AS mi_puntaje, rj.puntaje AS puntaje_rival, r.email AS email_rival
                FROM app_schema.app_partida_jugador yo
                JOIN app_schema.app_partida p ON p.id = yo.partida_id
                JOIN app_schema.app_partida_jugador rj ON rj.partida_id = p.id AND rj.estudiante_id <> yo.estudiante_id
                JOIN app_schema.app_estudiante r ON r.id = rj.estudiante_id
                WHERE yo.estudiante_id = :yo AND p.estado IN ('en_curso', 'terminada')
                ORDER BY COALESCE(p.fecha_fin, p.fecha_inicio) DESC, p.id DESC
                LIMIT :limite OFFSET :desde
                """)
                .param("yo", estudianteId).param("limite", limite).param("desde", desde)
                .query(FilaDeHistorial.class).list();
    }

    public int totalDeHistorial(long estudianteId) {
        return jdbc.sql("""
                SELECT count(*)
                FROM app_schema.app_partida_jugador yo
                JOIN app_schema.app_partida p ON p.id = yo.partida_id
                WHERE yo.estudiante_id = ? AND p.estado IN ('en_curso', 'terminada')
                """)
                .param(estudianteId).query(Integer.class).single();
    }

    // HU60-T2: el ranking completo del curso (todos los inscritos, también los que no jugaron). Orden: partidas ganadas,
    // luego porcentaje de victorias, luego puntos acumulados en partidas y, por último, el id del estudiante para que el
    // orden sea siempre el mismo. Solo cuentan las partidas terminadas del curso; un abandono ya es una victoria del otro
    // (D20) y un empate es una partida jugada que no es ni ganada ni perdida.
    public List<FilaDeRanking> ranking(long cursoId) {
        return jdbc.sql("""
                SELECT ROW_NUMBER() OVER (ORDER BY ganadas DESC, tasa DESC, puntos DESC, estudiante_id ASC) AS posicion,
                       estudiante_id, email, jugadas, ganadas, empatadas, jugadas - ganadas - empatadas AS perdidas, puntos,
                       tasa AS tasa_de_victorias
                FROM (
                    SELECT i.estudiante_id, e.email,
                           count(j.id)::int AS jugadas,
                           (count(j.id) FILTER (WHERE p.ganador_id = i.estudiante_id))::int AS ganadas,
                           (count(j.id) FILTER (WHERE p.id IS NOT NULL AND p.ganador_id IS NULL))::int AS empatadas,
                           COALESCE(sum(j.puntaje), 0)::int AS puntos,
                           CASE WHEN count(j.id) = 0 THEN 0.0
                                ELSE (count(j.id) FILTER (WHERE p.ganador_id = i.estudiante_id))::float8 / count(j.id) END AS tasa
                    FROM app_schema.app_inscripcion i
                    JOIN app_schema.app_estudiante e ON e.id = i.estudiante_id
                    LEFT JOIN (app_schema.app_partida_jugador j
                               JOIN app_schema.app_partida p
                                 ON p.id = j.partida_id AND p.curso_id = :curso AND p.estado = 'terminada')
                           ON j.estudiante_id = i.estudiante_id
                    WHERE i.curso_id = :curso
                    GROUP BY i.estudiante_id, e.email
                ) por_estudiante
                ORDER BY posicion
                """)
                .param("curso", cursoId).query(FilaDeRanking.class).list();
    }
}
