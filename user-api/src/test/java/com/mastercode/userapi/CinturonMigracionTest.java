package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import com.mastercode.userapi.perfil.EstudianteRepository;

// HU50-T1: la migración V11 crea los niveles de cinturón con los puntos que exige cada uno y app_cinturon, un cinturón
// por estudiante y lenguaje. Las filas que deben fallar van al final de cada prueba: en PostgreSQL, una sentencia fallida
// deja abortada la transacción.
@SpringBootTest
@Transactional
class CinturonMigracionTest {

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private EstudianteRepository estudiantes;

    private long crearEstudiante() {
        String email = "prueba-" + UUID.randomUUID() + "@ejemplo.edu.pe";
        estudiantes.insertarSiNoExiste(email, "ejemplo.edu.pe");
        return estudiantes.findByEmail(email).orElseThrow().getId();
    }

    private void insertar(Long estudianteId, String lenguaje, int nivel, int puntos) {
        jdbc.sql("INSERT INTO app_schema.app_cinturon (estudiante_id, lenguaje, nivel, puntos) VALUES (?, ?, ?, ?)")
                .params(estudianteId, lenguaje, nivel, puntos)
                .update();
    }

    @Test
    void los_siete_niveles_van_de_blanco_a_negro_con_sus_puntos() {
        List<Map<String, Object>> niveles = jdbc.sql(
                "SELECT nivel, nombre, puntos_requeridos FROM app_schema.app_nivel_cinturon ORDER BY nivel")
                .query().listOfRows();

        assertThat(niveles).extracting(fila -> fila.get("nombre")).containsExactly(
                "blanco", "amarillo", "naranja", "verde", "azul", "marron", "negro");
        assertThat(niveles).extracting(fila -> fila.get("puntos_requeridos")).containsExactly(
                0, 100, 250, 500, 900, 1400, 2000);
    }

    @Test
    void cada_nivel_exige_mas_puntos_que_el_anterior_y_el_blanco_no_exige_ninguno() {
        List<Integer> puntos = jdbc.sql("SELECT puntos_requeridos FROM app_schema.app_nivel_cinturon ORDER BY nivel")
                .query(Integer.class).list();

        assertThat(puntos.get(0)).isZero();
        assertThat(puntos).isSorted();
        assertThat(puntos).doesNotHaveDuplicates();
    }

    @Test
    void un_cinturon_nuevo_empieza_en_blanco_con_cero_puntos() {
        long estudiante = crearEstudiante();

        jdbc.sql("INSERT INTO app_schema.app_cinturon (estudiante_id, lenguaje) VALUES (?, 'python')")
                .param(estudiante).update();

        Map<String, Object> fila = jdbc.sql(
                "SELECT nivel, puntos, fecha_actualizacion IS NOT NULL AS con_fecha FROM app_schema.app_cinturon WHERE estudiante_id = ?")
                .param(estudiante).query().singleRow();
        assertThat(((Number) fila.get("nivel")).intValue()).isEqualTo(1);
        assertThat(((Number) fila.get("puntos")).intValue()).isZero();
        assertThat(fila.get("con_fecha")).isEqualTo(true);
    }

    @Test
    void un_estudiante_puede_tener_un_cinturon_por_lenguaje() {
        long estudiante = crearEstudiante();

        insertar(estudiante, "python", 2, 120);
        insertar(estudiante, "java", 1, 10);

        assertThat(jdbc.sql("SELECT count(*) FROM app_schema.app_cinturon WHERE estudiante_id = ?")
                .param(estudiante).query(Integer.class).single()).isEqualTo(2);
    }

    @Test
    void rechaza_dos_cinturones_del_mismo_lenguaje_para_un_estudiante() {
        long estudiante = crearEstudiante();
        insertar(estudiante, "python", 1, 0);

        assertThatThrownBy(() -> insertar(estudiante, "python", 2, 100))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechaza_puntos_negativos() {
        long estudiante = crearEstudiante();

        assertThatThrownBy(() -> insertar(estudiante, "python", 1, -1))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechaza_un_nivel_que_no_existe() {
        long estudiante = crearEstudiante();

        assertThatThrownBy(() -> insertar(estudiante, "python", 8, 0))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechaza_un_cinturon_sin_dueno_o_de_un_estudiante_inexistente() {
        assertThatThrownBy(() -> insertar(null, "python", 1, 0)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechaza_un_estudiante_inexistente() {
        assertThatThrownBy(() -> insertar(999_999_999L, "python", 1, 0))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------- app_puntos_ejercicio (V13, HU50-T2) ----------

    private void apuntarPuntos(Long estudianteId, long problemaId, int puntos) {
        jdbc.sql("INSERT INTO app_schema.app_puntos_ejercicio (estudiante_id, problema_id, lenguaje, puntos) VALUES (?, ?, 'python', ?)")
                .params(estudianteId, problemaId, puntos)
                .update();
    }

    @Test
    void un_ejercicio_da_puntos_una_sola_vez_por_estudiante() {
        long uno = crearEstudiante();
        long otro = crearEstudiante();

        apuntarPuntos(uno, 7, 10);
        // Otro estudiante sí puede ganar los puntos del mismo ejercicio.
        apuntarPuntos(otro, 7, 10);

        assertThatThrownBy(() -> apuntarPuntos(uno, 7, 10)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechaza_puntos_negativos_y_un_estudiante_inexistente_en_el_registro_de_puntos() {
        long estudiante = crearEstudiante();

        assertThatThrownBy(() -> apuntarPuntos(estudiante, 7, -5)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechaza_un_registro_de_puntos_de_un_estudiante_inexistente() {
        assertThatThrownBy(() -> apuntarPuntos(999_999_999L, 7, 10)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
