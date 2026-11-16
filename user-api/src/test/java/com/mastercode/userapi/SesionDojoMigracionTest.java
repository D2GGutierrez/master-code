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

// HU53-T1: la migración V12 crea app_sesion_dojo y los ejercicios que se entregan en cada sesión, sin repetir ninguno.
// Las filas que deben fallar van al final de cada prueba: en PostgreSQL, una sentencia fallida deja abortada la
// transacción.
@SpringBootTest
@Transactional
class SesionDojoMigracionTest {

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private EstudianteRepository estudiantes;

    private long crearEstudiante() {
        String email = "prueba-" + UUID.randomUUID() + "@ejemplo.edu.pe";
        estudiantes.insertarSiNoExiste(email, "ejemplo.edu.pe");
        return estudiantes.findByEmail(email).orElseThrow().getId();
    }

    private long crearSesion(long estudianteId) {
        return jdbc.sql("INSERT INTO app_schema.app_sesion_dojo (estudiante_id, lenguaje) VALUES (?, 'python') RETURNING id")
                .param(estudianteId).query(Long.class).single();
    }

    private void entregar(long sesionId, long problemaId, int orden) {
        jdbc.sql("INSERT INTO app_schema.app_sesion_dojo_ejercicio (sesion_id, problema_id, orden) VALUES (?, ?, ?)")
                .params(sesionId, problemaId, orden)
                .update();
    }

    @Test
    void una_sesion_nueva_queda_abierta_con_su_fecha_de_inicio() {
        long sesion = crearSesion(crearEstudiante());

        Map<String, Object> fila = jdbc.sql(
                "SELECT fecha_inicio IS NOT NULL AS con_inicio, fecha_fin IS NULL AS abierta FROM app_schema.app_sesion_dojo WHERE id = ?")
                .param(sesion).query().singleRow();
        assertThat(fila.get("con_inicio")).isEqualTo(true);
        assertThat(fila.get("abierta")).isEqualTo(true);
    }

    @Test
    void un_ejercicio_entregado_empieza_sin_respuesta() {
        long sesion = crearSesion(crearEstudiante());

        entregar(sesion, 7, 1);

        Map<String, Object> fila = jdbc.sql(
                "SELECT acerto IS NULL AS sin_respuesta, fecha_entrega IS NOT NULL AS con_fecha FROM app_schema.app_sesion_dojo_ejercicio WHERE sesion_id = ?")
                .param(sesion).query().singleRow();
        assertThat(fila.get("sin_respuesta")).isEqualTo(true);
        assertThat(fila.get("con_fecha")).isEqualTo(true);
    }

    @Test
    void el_mismo_ejercicio_puede_estar_en_otra_sesion() {
        long estudiante = crearEstudiante();

        entregar(crearSesion(estudiante), 7, 1);
        entregar(crearSesion(estudiante), 7, 1);

        assertThat(jdbc.sql("SELECT count(*) FROM app_schema.app_sesion_dojo_ejercicio WHERE problema_id = 7 AND sesion_id IN (SELECT id FROM app_schema.app_sesion_dojo WHERE estudiante_id = ?)")
                .param(estudiante).query(Integer.class).single()).isEqualTo(2);
    }

    @Test
    void rechaza_entregar_dos_veces_el_mismo_ejercicio_en_una_sesion() {
        long sesion = crearSesion(crearEstudiante());
        entregar(sesion, 7, 1);

        assertThatThrownBy(() -> entregar(sesion, 7, 2)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechaza_dos_ejercicios_con_el_mismo_orden_en_una_sesion() {
        long sesion = crearSesion(crearEstudiante());
        entregar(sesion, 7, 1);

        assertThatThrownBy(() -> entregar(sesion, 8, 1)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechaza_un_orden_menor_que_uno() {
        long sesion = crearSesion(crearEstudiante());

        assertThatThrownBy(() -> entregar(sesion, 7, 0)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechaza_un_ejercicio_de_una_sesion_inexistente() {
        assertThatThrownBy(() -> entregar(999_999_999L, 7, 1)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechaza_una_sesion_de_un_estudiante_inexistente() {
        assertThatThrownBy(() -> crearSesion(999_999_999L)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
