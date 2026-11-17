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

// HU54-T3: la migración V15 crea app_nivel_dificultad y agrega el nivel y las rachas a la sesión del Dojo. Las filas que
// deben fallar van al final de cada prueba.
@SpringBootTest
@Transactional
class NivelDificultadMigracionTest {

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private EstudianteRepository estudiantes;

    private long crearEstudiante() {
        String email = "prueba-" + UUID.randomUUID() + "@ejemplo.edu.pe";
        estudiantes.insertarSiNoExiste(email, "ejemplo.edu.pe");
        return estudiantes.findByEmail(email).orElseThrow().getId();
    }

    private void guardarNivel(long estudiante, String lenguaje, String nivel) {
        jdbc.sql("INSERT INTO app_schema.app_nivel_dificultad (estudiante_id, lenguaje, nivel) VALUES (?, ?, ?)")
                .params(estudiante, lenguaje, nivel).update();
    }

    @Test
    void guarda_un_nivel_por_estudiante_y_lenguaje() {
        long estudiante = crearEstudiante();
        guardarNivel(estudiante, "python", "medio");
        guardarNivel(estudiante, "java", "dificil");

        assertThat(jdbc.sql("SELECT nivel FROM app_schema.app_nivel_dificultad WHERE estudiante_id = ? AND lenguaje = 'java'")
                .param(estudiante).query(String.class).single()).isEqualTo("dificil");

        assertThatThrownBy(() -> guardarNivel(estudiante, "python", "facil")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void solo_acepta_los_tres_niveles() {
        long estudiante = crearEstudiante();

        assertThatThrownBy(() -> guardarNivel(estudiante, "python", "experto")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void una_sesion_nueva_parte_en_facil_con_las_rachas_en_cero() {
        long estudiante = crearEstudiante();

        long sesion = jdbc.sql("INSERT INTO app_schema.app_sesion_dojo (estudiante_id, lenguaje) VALUES (?, 'python') RETURNING id")
                .param(estudiante).query(Long.class).single();

        assertThat(jdbc.sql("SELECT nivel FROM app_schema.app_sesion_dojo WHERE id = ?").param(sesion)
                .query(String.class).single()).isEqualTo("facil");
        assertThat(jdbc.sql("SELECT racha_aciertos + racha_errores FROM app_schema.app_sesion_dojo WHERE id = ?")
                .param(sesion).query(Integer.class).single()).isZero();
    }

    @Test
    void la_sesion_rechaza_un_nivel_desconocido_y_rachas_negativas() {
        long estudiante = crearEstudiante();

        assertThatThrownBy(() -> jdbc.sql("INSERT INTO app_schema.app_sesion_dojo (estudiante_id, lenguaje, nivel) VALUES (?, 'python', 'x')")
                .param(estudiante).update()).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void la_sesion_rechaza_rachas_negativas() {
        long estudiante = crearEstudiante();

        assertThatThrownBy(() -> jdbc.sql("INSERT INTO app_schema.app_sesion_dojo (estudiante_id, lenguaje, racha_aciertos) VALUES (?, 'python', -1)")
                .param(estudiante).update()).isInstanceOf(DataIntegrityViolationException.class);
    }
}
