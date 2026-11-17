package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import com.mastercode.userapi.perfil.EstudianteRepository;

// HU57-T1 (CA-57.2, ADR-13 D23): la migración V18 crea app_partida y app_partida_jugador. Las filas que deben fallar van
// al final de cada prueba: en PostgreSQL, una sentencia fallida deja abortada la transacción.
@SpringBootTest
@Transactional
class PartidaMigracionTest {

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private EstudianteRepository estudiantes;

    private long crearEstudiante() {
        String email = "prueba-" + UUID.randomUUID() + "@ejemplo.edu.pe";
        estudiantes.insertarSiNoExiste(email, "ejemplo.edu.pe");
        return estudiantes.findByEmail(email).orElseThrow().getId();
    }

    private long crearPartida() {
        return jdbc.sql("INSERT INTO app_schema.app_partida (curso_id) VALUES (10) RETURNING id")
                .query(Long.class).single();
    }

    private void agregarJugador(long partida, long estudiante, boolean retador) {
        jdbc.sql("INSERT INTO app_schema.app_partida_jugador (partida_id, estudiante_id, es_retador) VALUES (?, ?, ?)")
                .params(partida, estudiante, retador).update();
    }

    // ---------- la partida ----------

    @Test
    void una_partida_nueva_nace_pendiente_sin_inicio_fin_ni_ganador() {
        long partida = crearPartida();

        Map<String, Object> fila = jdbc.sql("SELECT * FROM app_schema.app_partida WHERE id = ?")
                .param(partida).query().singleRow();

        assertThat(fila.get("estado")).isEqualTo("pendiente");
        assertThat(fila.get("curso_id")).isEqualTo(10L);
        assertThat(fila.get("fecha_creacion")).isNotNull();
        assertThat(fila.get("fecha_inicio")).isNull();
        assertThat(fila.get("fecha_fin")).isNull();
        assertThat(fila.get("ganador_id")).isNull();
        assertThat(fila.get("motivo_fin")).isNull();
    }

    @Test
    void una_partida_sigue_el_ciclo_pendiente_en_curso_terminada_con_ganador_y_motivo() {
        long partida = crearPartida();
        long ganador = crearEstudiante();

        jdbc.sql("UPDATE app_schema.app_partida SET estado = 'en_curso', fecha_inicio = now() WHERE id = ?")
                .param(partida).update();
        jdbc.sql("""
                UPDATE app_schema.app_partida
                SET estado = 'terminada', fecha_fin = now(), ganador_id = ?, motivo_fin = 'abandono'
                WHERE id = ?
                """).params(ganador, partida).update();

        Map<String, Object> fila = jdbc.sql("SELECT estado, ganador_id, motivo_fin FROM app_schema.app_partida WHERE id = ?")
                .param(partida).query().singleRow();
        assertThat(fila.get("estado")).isEqualTo("terminada");
        assertThat(fila.get("ganador_id")).isEqualTo(ganador);
        assertThat(fila.get("motivo_fin")).isEqualTo("abandono");
    }

    @Test
    void un_empate_termina_sin_ganador() {
        long partida = crearPartida();

        jdbc.sql("UPDATE app_schema.app_partida SET estado = 'terminada', motivo_fin = 'completada' WHERE id = ?")
                .param(partida).update();

        assertThat(jdbc.sql("SELECT ganador_id FROM app_schema.app_partida WHERE id = ?")
                .param(partida).query(Long.class).optional()).isEmpty();
    }

    @Test
    void los_cinco_estados_validos_se_aceptan() {
        for (String estado : new String[] { "pendiente", "en_curso", "terminada", "rechazada", "caducada" }) {
            jdbc.sql("INSERT INTO app_schema.app_partida (curso_id, estado) VALUES (10, ?)").param(estado).update();
        }
    }

    @Test
    void un_estado_desconocido_se_rechaza() {
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO app_schema.app_partida (curso_id, estado) VALUES (10, 'empezada')")
                .update()).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void un_motivo_de_fin_desconocido_se_rechaza() {
        assertThatThrownBy(() -> jdbc.sql(
                "INSERT INTO app_schema.app_partida (curso_id, estado, motivo_fin) VALUES (10, 'terminada', 'aburrimiento')")
                .update()).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void solo_una_partida_terminada_puede_tener_ganador() {
        long ganador = crearEstudiante();

        assertThatThrownBy(() -> jdbc.sql(
                "INSERT INTO app_schema.app_partida (curso_id, estado, ganador_id) VALUES (10, 'en_curso', ?)")
                .param(ganador).update()).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void solo_una_partida_terminada_puede_tener_motivo_de_fin() {
        assertThatThrownBy(() -> jdbc.sql(
                "INSERT INTO app_schema.app_partida (curso_id, estado, motivo_fin) VALUES (10, 'pendiente', 'completada')")
                .update()).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void el_ganador_debe_ser_un_estudiante_que_existe() {
        assertThatThrownBy(() -> jdbc.sql(
                "INSERT INTO app_schema.app_partida (curso_id, estado, ganador_id) VALUES (10, 'terminada', -1)")
                .update()).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void la_partida_necesita_curso() {
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO app_schema.app_partida (estado) VALUES ('pendiente')").update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------- los jugadores ----------

    @Test
    void un_jugador_nuevo_empieza_en_cero_y_sin_senal() {
        long partida = crearPartida();
        long retador = crearEstudiante();

        agregarJugador(partida, retador, true);

        Map<String, Object> fila = jdbc.sql("""
                SELECT puntaje, respondidos, ultima_senal, es_retador FROM app_schema.app_partida_jugador
                WHERE partida_id = ?
                """).param(partida).query().singleRow();
        assertThat(((Number) fila.get("puntaje")).intValue()).isZero();
        assertThat(((Number) fila.get("respondidos")).intValue()).isZero();
        // D20: la última señal queda vacía hasta la primera petición del jugador.
        assertThat(fila.get("ultima_senal")).isNull();
        assertThat(fila.get("es_retador")).isEqualTo(true);
    }

    @Test
    void una_partida_tiene_un_retador_y_un_invitado() {
        long partida = crearPartida();

        agregarJugador(partida, crearEstudiante(), true);
        agregarJugador(partida, crearEstudiante(), false);

        assertThat(jdbc.sql("SELECT count(*) FROM app_schema.app_partida_jugador WHERE partida_id = ?")
                .param(partida).query(Integer.class).single()).isEqualTo(2);
    }

    @Test
    void la_ultima_senal_se_puede_actualizar_en_la_fila_del_jugador() {
        long partida = crearPartida();
        long retador = crearEstudiante();
        agregarJugador(partida, retador, true);

        jdbc.sql("UPDATE app_schema.app_partida_jugador SET ultima_senal = now() WHERE partida_id = ? AND estudiante_id = ?")
                .params(partida, retador).update();

        assertThat(jdbc.sql("SELECT ultima_senal FROM app_schema.app_partida_jugador WHERE partida_id = ?")
                .param(partida).query(java.sql.Timestamp.class).single()).isNotNull();
    }

    @Test
    void el_mismo_estudiante_no_puede_ser_los_dos_jugadores_de_una_partida() {
        long partida = crearPartida();
        long estudiante = crearEstudiante();
        agregarJugador(partida, estudiante, true);

        assertThatThrownBy(() -> agregarJugador(partida, estudiante, false))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void una_partida_no_puede_tener_dos_retadores() {
        long partida = crearPartida();
        agregarJugador(partida, crearEstudiante(), true);

        assertThatThrownBy(() -> agregarJugador(partida, crearEstudiante(), true))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void el_mismo_estudiante_puede_estar_en_partidas_distintas() {
        long estudiante = crearEstudiante();

        agregarJugador(crearPartida(), estudiante, true);
        agregarJugador(crearPartida(), estudiante, false);

        assertThat(jdbc.sql("SELECT count(*) FROM app_schema.app_partida_jugador WHERE estudiante_id = ?")
                .param(estudiante).query(Integer.class).single()).isEqualTo(2);
    }

    @Test
    void los_respondidos_no_pueden_ser_negativos() {
        long partida = crearPartida();
        long estudiante = crearEstudiante();
        agregarJugador(partida, estudiante, true);

        assertThatThrownBy(() -> jdbc.sql("UPDATE app_schema.app_partida_jugador SET respondidos = -1 WHERE partida_id = ?")
                .param(partida).update()).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void el_puntaje_no_puede_ser_negativo() {
        long partida = crearPartida();
        agregarJugador(partida, crearEstudiante(), true);

        assertThatThrownBy(() -> jdbc.sql("UPDATE app_schema.app_partida_jugador SET puntaje = -5 WHERE partida_id = ?")
                .param(partida).update()).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void el_jugador_debe_ser_un_estudiante_que_existe() {
        long partida = crearPartida();

        assertThatThrownBy(() -> agregarJugador(partida, -1, true)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void el_jugador_debe_pertenecer_a_una_partida_que_existe() {
        long estudiante = crearEstudiante();

        assertThatThrownBy(() -> agregarJugador(-1, estudiante, true)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void una_partida_con_jugadores_no_se_borra_en_silencio() {
        long partida = crearPartida();
        agregarJugador(partida, crearEstudiante(), true);

        assertThatThrownBy(() -> jdbc.sql("DELETE FROM app_schema.app_partida WHERE id = ?").param(partida).update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
