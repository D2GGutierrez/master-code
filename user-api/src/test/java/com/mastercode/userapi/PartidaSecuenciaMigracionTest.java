package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import com.mastercode.userapi.perfil.EstudianteRepository;

// HU58-T1, H9 y HU52-T2 (ADR-15 D26): la migración V19 crea app_partida_ejercicio, app_partida_respuesta y
// app_aviso_ajuste. Las filas que deben fallar van al final de cada prueba: en PostgreSQL una sentencia fallida deja
// abortada la transacción.
@SpringBootTest
@Transactional
class PartidaSecuenciaMigracionTest {

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
        return jdbc.sql("INSERT INTO app_schema.app_partida (curso_id) VALUES (10) RETURNING id").query(Long.class).single();
    }

    private void ejercicio(long partida, int turno, long problema) {
        jdbc.sql("INSERT INTO app_schema.app_partida_ejercicio (partida_id, turno, problema_id) VALUES (?, ?, ?)")
                .params(partida, turno, problema).update();
    }

    private void respuesta(long partida, long estudiante, int turno, boolean correcta) {
        jdbc.sql("INSERT INTO app_schema.app_partida_respuesta (partida_id, estudiante_id, turno, correcta) VALUES (?, ?, ?, ?)")
                .params(partida, estudiante, turno, correcta).update();
    }

    // ---------- la secuencia ----------

    @Test
    void la_secuencia_guarda_los_ejercicios_en_su_orden() {
        long partida = crearPartida();

        ejercicio(partida, 1, 500);
        ejercicio(partida, 2, 300);
        ejercicio(partida, 3, 400);

        assertThat(jdbc.sql("SELECT problema_id FROM app_schema.app_partida_ejercicio WHERE partida_id = ? ORDER BY turno")
                .param(partida).query(Long.class).list()).containsExactly(500L, 300L, 400L);
    }

    @Test
    void un_turno_no_se_repite_en_la_misma_partida() {
        long partida = crearPartida();
        ejercicio(partida, 1, 500);

        assertThatThrownBy(() -> ejercicio(partida, 1, 501)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void un_ejercicio_no_se_repite_en_la_misma_partida() {
        long partida = crearPartida();
        ejercicio(partida, 1, 500);

        assertThatThrownBy(() -> ejercicio(partida, 2, 500)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void el_mismo_ejercicio_puede_estar_en_partidas_distintas() {
        ejercicio(crearPartida(), 1, 500);
        ejercicio(crearPartida(), 1, 500);
    }

    @Test
    void el_turno_empieza_en_uno() {
        long partida = crearPartida();

        assertThatThrownBy(() -> ejercicio(partida, 0, 500)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void el_ejercicio_debe_ser_de_una_partida_que_existe() {
        assertThatThrownBy(() -> ejercicio(-1, 1, 500)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void una_partida_con_secuencia_no_se_borra_en_silencio() {
        long partida = crearPartida();
        ejercicio(partida, 1, 500);

        assertThatThrownBy(() -> jdbc.sql("DELETE FROM app_schema.app_partida WHERE id = ?").param(partida).update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------- las respuestas ----------

    @Test
    void una_respuesta_guarda_quien_respondio_el_turno_y_si_acerto() {
        long partida = crearPartida();
        long estudiante = crearEstudiante();
        ejercicio(partida, 1, 500);

        respuesta(partida, estudiante, 1, true);

        Map<String, Object> fila = jdbc.sql("SELECT * FROM app_schema.app_partida_respuesta WHERE partida_id = ?")
                .param(partida).query().singleRow();
        assertThat(fila.get("estudiante_id")).isEqualTo(estudiante);
        assertThat(fila.get("correcta")).isEqualTo(true);
        assertThat(fila.get("fecha")).isNotNull();
    }

    @Test
    void los_dos_jugadores_responden_el_mismo_turno() {
        long partida = crearPartida();
        ejercicio(partida, 1, 500);

        respuesta(partida, crearEstudiante(), 1, true);
        respuesta(partida, crearEstudiante(), 1, false);
    }

    @Test
    void un_jugador_no_responde_dos_veces_el_mismo_turno() {
        long partida = crearPartida();
        long estudiante = crearEstudiante();
        ejercicio(partida, 1, 500);
        respuesta(partida, estudiante, 1, true);

        assertThatThrownBy(() -> respuesta(partida, estudiante, 1, false)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void no_se_responde_un_turno_que_no_esta_en_la_secuencia() {
        long partida = crearPartida();
        long estudiante = crearEstudiante();
        ejercicio(partida, 1, 500);

        assertThatThrownBy(() -> respuesta(partida, estudiante, 2, true)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void el_que_responde_debe_ser_un_estudiante_que_existe() {
        long partida = crearPartida();
        ejercicio(partida, 1, 500);

        assertThatThrownBy(() -> respuesta(partida, -1, 1, true)).isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------- los ajustes de avisos ----------

    @Test
    void un_ajuste_nuevo_trae_los_valores_por_defecto_de_la_app() {
        long estudiante = crearEstudiante();

        jdbc.sql("INSERT INTO app_schema.app_aviso_ajuste (estudiante_id) VALUES (?)").param(estudiante).update();

        Map<String, Object> fila = jdbc.sql("SELECT activados, hora, ultimo_aviso FROM app_schema.app_aviso_ajuste WHERE estudiante_id = ?")
                .param(estudiante).query().singleRow();
        assertThat(fila.get("activados")).isEqualTo(true);
        assertThat(((java.sql.Time) fila.get("hora")).toLocalTime()).isEqualTo(LocalTime.of(20, 0));
        assertThat(fila.get("ultimo_aviso")).isNull();
    }

    @Test
    void cada_estudiante_tiene_un_solo_ajuste() {
        long estudiante = crearEstudiante();
        jdbc.sql("INSERT INTO app_schema.app_aviso_ajuste (estudiante_id) VALUES (?)").param(estudiante).update();

        assertThatThrownBy(() -> jdbc.sql("INSERT INTO app_schema.app_aviso_ajuste (estudiante_id) VALUES (?)").param(estudiante)
                .update()).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void el_ajuste_debe_ser_de_un_estudiante_que_existe() {
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO app_schema.app_aviso_ajuste (estudiante_id) VALUES (-1)").update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
