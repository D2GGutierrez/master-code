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

// HU52-T1: la migración V10 crea app_dispositivo, con un token único y dueño obligatorio. Las filas que deben fallar
// van al final de cada prueba: en PostgreSQL, una sentencia fallida deja abortada la transacción.
@SpringBootTest
@Transactional
class DispositivoMigracionTest {

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private EstudianteRepository estudiantes;

    private long crearEstudiante() {
        String email = "prueba-" + UUID.randomUUID() + "@ejemplo.edu.pe";
        estudiantes.insertarSiNoExiste(email, "ejemplo.edu.pe");
        return estudiantes.findByEmail(email).orElseThrow().getId();
    }

    private void insertar(Long estudianteId, String token) {
        jdbc.sql("INSERT INTO app_schema.app_dispositivo (estudiante_id, token) VALUES (?, ?)")
                .params(estudianteId, token)
                .update();
    }

    @Test
    void guarda_el_dispositivo_con_la_fecha_de_registro_por_defecto() {
        long estudiante = crearEstudiante();

        insertar(estudiante, "token-migracion-1");

        Boolean conFecha = jdbc.sql("SELECT fecha_registro IS NOT NULL FROM app_schema.app_dispositivo WHERE token = ?")
                .param("token-migracion-1").query(Boolean.class).single();
        assertThat(conFecha).isTrue();
    }

    @Test
    void permite_varios_dispositivos_por_estudiante() {
        long estudiante = crearEstudiante();

        insertar(estudiante, "token-migracion-2a");
        insertar(estudiante, "token-migracion-2b");

        assertThat(jdbc.sql("SELECT count(*) FROM app_schema.app_dispositivo WHERE estudiante_id = ?")
                .param(estudiante).query(Integer.class).single()).isEqualTo(2);
    }

    @Test
    void rechaza_el_mismo_token_dos_veces() {
        long uno = crearEstudiante();
        long otro = crearEstudiante();
        insertar(uno, "token-migracion-3");

        assertThatThrownBy(() -> insertar(otro, "token-migracion-3"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechaza_un_dispositivo_sin_dueno() {
        assertThatThrownBy(() -> insertar(null, "token-migracion-4"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechaza_un_estudiante_inexistente() {
        assertThatThrownBy(() -> insertar(999_999_999L, "token-migracion-5"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
