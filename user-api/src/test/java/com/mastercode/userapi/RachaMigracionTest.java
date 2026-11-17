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

// HU49-T1: la migración V14 crea app_racha, una racha por estudiante y lenguaje. Las filas que deben fallar van al final
// de cada prueba: en PostgreSQL, una sentencia fallida deja abortada la transacción.
@SpringBootTest
@Transactional
class RachaMigracionTest {

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private EstudianteRepository estudiantes;

    private long crearEstudiante() {
        String email = "prueba-" + UUID.randomUUID() + "@ejemplo.edu.pe";
        estudiantes.insertarSiNoExiste(email, "ejemplo.edu.pe");
        return estudiantes.findByEmail(email).orElseThrow().getId();
    }

    private void insertar(long estudianteId, String lenguaje, int actual, int maxima) {
        jdbc.sql("""
                INSERT INTO app_schema.app_racha (estudiante_id, lenguaje, racha_actual, racha_maxima, fecha_ultima_actividad)
                VALUES (?, ?, ?, ?, DATE '2026-10-01')
                """)
                .params(estudianteId, lenguaje, actual, maxima)
                .update();
    }

    @Test
    void una_racha_nueva_empieza_en_cero_y_sin_actividad() {
        long estudiante = crearEstudiante();

        jdbc.sql("INSERT INTO app_schema.app_racha (estudiante_id, lenguaje) VALUES (?, 'python')")
                .param(estudiante).update();

        Map<String, Object> fila = jdbc.sql("""
                SELECT racha_actual, racha_maxima, fecha_ultima_actividad FROM app_schema.app_racha
                WHERE estudiante_id = ?
                """).param(estudiante).query().singleRow();
        assertThat(((Number) fila.get("racha_actual")).intValue()).isZero();
        assertThat(((Number) fila.get("racha_maxima")).intValue()).isZero();
        assertThat(fila.get("fecha_ultima_actividad")).isNull();
    }

    @Test
    void guarda_la_racha_actual_la_maxima_y_la_fecha_del_ultimo_dia() {
        long estudiante = crearEstudiante();

        insertar(estudiante, "python", 3, 7);

        Map<String, Object> fila = jdbc.sql("""
                SELECT racha_actual, racha_maxima, fecha_ultima_actividad::text AS dia FROM app_schema.app_racha
                WHERE estudiante_id = ?
                """).param(estudiante).query().singleRow();
        assertThat(((Number) fila.get("racha_actual")).intValue()).isEqualTo(3);
        assertThat(((Number) fila.get("racha_maxima")).intValue()).isEqualTo(7);
        assertThat(fila.get("dia")).isEqualTo("2026-10-01");
    }

    @Test
    void un_estudiante_puede_tener_una_racha_por_lenguaje_pero_no_dos_del_mismo() {
        long estudiante = crearEstudiante();
        insertar(estudiante, "python", 1, 1);
        insertar(estudiante, "java", 2, 2);

        assertThatThrownBy(() -> insertar(estudiante, "python", 5, 5)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void la_maxima_no_puede_ser_menor_que_la_actual() {
        long estudiante = crearEstudiante();

        assertThatThrownBy(() -> insertar(estudiante, "python", 4, 3)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void una_racha_negativa_se_rechaza() {
        long estudiante = crearEstudiante();

        assertThatThrownBy(() -> insertar(estudiante, "python", -1, 0)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void no_se_puede_crear_la_racha_de_un_estudiante_que_no_existe() {
        assertThatThrownBy(() -> insertar(-1, "python", 0, 0)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
