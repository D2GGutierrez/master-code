package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import com.mastercode.userapi.perfil.EstudianteRepository;

// HU64-T3: la migración V9 crea app_intento_evaluacion, con un solo intento por estudiante y evaluación.
// @Transactional: cada prueba deshace lo que insertó. Las filas que deben fallar van al final de cada prueba: en
// PostgreSQL, una sentencia fallida deja abortada la transacción.
@SpringBootTest
@Transactional
class IntentoEvaluacionMigracionTest {

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private EstudianteRepository estudiantes;

    private long crearEstudiante() {
        String email = "prueba-" + UUID.randomUUID() + "@ejemplo.edu.pe";
        estudiantes.insertarSiNoExiste(email, "ejemplo.edu.pe");
        return estudiantes.findByEmail(email).orElseThrow().getId();
    }

    private void insertar(long estudianteId, long evaluacionId) {
        jdbc.sql("INSERT INTO app_schema.app_intento_evaluacion (estudiante_id, evaluacion_id) VALUES (?, ?)")
                .params(estudianteId, evaluacionId)
                .update();
    }

    @Test
    void guarda_el_intento_con_la_fecha_de_inicio_por_defecto() {
        long estudiante = crearEstudiante();

        insertar(estudiante, 5);

        Boolean conFecha = jdbc.sql(
                "SELECT fecha_inicio IS NOT NULL FROM app_schema.app_intento_evaluacion WHERE estudiante_id = ?")
                .param(estudiante).query(Boolean.class).single();
        assertThat(conFecha).isTrue();
    }

    @Test
    void permite_un_intento_por_evaluacion_para_el_mismo_estudiante_y_la_misma_evaluacion_a_otros() {
        long uno = crearEstudiante();
        long otro = crearEstudiante();

        insertar(uno, 5);
        insertar(uno, 6);
        insertar(otro, 5);

        Integer total = jdbc.sql("SELECT count(*) FROM app_schema.app_intento_evaluacion WHERE estudiante_id IN (?, ?)")
                .params(uno, otro).query(Integer.class).single();
        assertThat(total).isEqualTo(3);
    }

    @Test
    void rechaza_un_segundo_intento_del_mismo_estudiante_en_la_misma_evaluacion() {
        long estudiante = crearEstudiante();
        insertar(estudiante, 5);

        assertThatThrownBy(() -> insertar(estudiante, 5)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechaza_un_intento_de_un_estudiante_que_no_existe() {
        assertThatThrownBy(() -> insertar(999_999_999L, 5)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
