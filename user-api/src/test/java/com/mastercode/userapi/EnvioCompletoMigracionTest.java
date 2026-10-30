package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import com.mastercode.userapi.perfil.EstudianteRepository;

// HU36-T1: la migración V5 completa app_envio con el código, el puntaje y el número de intento, y deja que resultado
// sea también «parcial». @Transactional: cada prueba deshace lo que insertó. Las filas que deben fallar van al final
// de cada prueba: en PostgreSQL, una sentencia fallida deja abortada la transacción.
@SpringBootTest
@Transactional
class EnvioCompletoMigracionTest {

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private EstudianteRepository estudiantes;

    private long crearEstudiante() {
        String email = "prueba-" + UUID.randomUUID() + "@ejemplo.edu.pe";
        estudiantes.insertarSiNoExiste(email, "ejemplo.edu.pe");
        return estudiantes.findByEmail(email).orElseThrow().getId();
    }

    private void insertar(long estudianteId, long problemaId, Integer intento, String resultado, BigDecimal puntaje,
            String codigo) {
        jdbc.sql("""
                INSERT INTO app_schema.app_envio
                    (estudiante_id, problema_id, plataforma, resultado, puntaje, numero_intento, codigo)
                VALUES (?, ?, 'web', ?, ?, ?, ?)
                """)
                .params(estudianteId, problemaId, resultado, puntaje, intento, codigo)
                .update();
    }

    @Test
    void acepta_correcto_parcial_e_incorrecto() {
        long estudiante = crearEstudiante();

        insertar(estudiante, 7, 1, "correcto", new BigDecimal("100.00"), "a");
        insertar(estudiante, 7, 2, "parcial", new BigDecimal("66.67"), "b");
        insertar(estudiante, 7, 3, "incorrecto", new BigDecimal("0.00"), "c");

        Integer guardados = jdbc.sql("SELECT count(*) FROM app_schema.app_envio WHERE estudiante_id = ?")
                .param(estudiante).query(Integer.class).single();
        assertThat(guardados).isEqualTo(3);
    }

    @Test
    void rechaza_cualquier_otro_resultado() {
        long estudiante = crearEstudiante();

        assertThatThrownBy(() -> insertar(estudiante, 7, 1, "Parcial", new BigDecimal("50"), "a"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void el_puntaje_va_de_0_a_100() {
        long estudiante = crearEstudiante();
        insertar(estudiante, 7, 1, "incorrecto", new BigDecimal("0"), "a");
        insertar(estudiante, 7, 2, "correcto", new BigDecimal("100"), "b");

        assertThatThrownBy(() -> insertar(estudiante, 7, 3, "correcto", new BigDecimal("100.01"), "c"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void un_puntaje_negativo_se_rechaza() {
        long estudiante = crearEstudiante();

        assertThatThrownBy(() -> insertar(estudiante, 7, 1, "incorrecto", new BigDecimal("-1"), "a"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void guarda_el_codigo_completo_aunque_sea_largo() {
        long estudiante = crearEstudiante();
        String codigo = "print('x')\n".repeat(5000);

        insertar(estudiante, 7, 1, "correcto", new BigDecimal("100"), codigo);

        String guardado = jdbc.sql("SELECT codigo FROM app_schema.app_envio WHERE estudiante_id = ?")
                .param(estudiante).query(String.class).single();
        assertThat(guardado).isEqualTo(codigo);
    }

    @Test
    void el_mismo_numero_se_permite_en_otro_problema_y_en_otro_estudiante() {
        long uno = crearEstudiante();
        long otro = crearEstudiante();

        insertar(uno, 7, 1, "correcto", new BigDecimal("100"), "a");
        insertar(uno, 8, 1, "correcto", new BigDecimal("100"), "a");
        insertar(otro, 7, 1, "correcto", new BigDecimal("100"), "a");

        Integer guardados = jdbc.sql("SELECT count(*) FROM app_schema.app_envio WHERE estudiante_id IN (?, ?)")
                .params(uno, otro).query(Integer.class).single();
        assertThat(guardados).isEqualTo(3);
    }

    @Test
    void las_filas_sin_numero_no_chocan_entre_si() {
        long estudiante = crearEstudiante();

        // Las anteriores a V5 y las sembradas sin calificar no tienen número: el índice único no las junta.
        insertar(estudiante, 7, null, null, null, null);
        insertar(estudiante, 7, null, null, null, null);

        Integer guardados = jdbc.sql("SELECT count(*) FROM app_schema.app_envio WHERE estudiante_id = ?")
                .param(estudiante).query(Integer.class).single();
        assertThat(guardados).isEqualTo(2);
    }

    @Test
    void dos_envios_del_mismo_estudiante_al_mismo_problema_no_comparten_numero() {
        long estudiante = crearEstudiante();
        insertar(estudiante, 7, 1, "incorrecto", new BigDecimal("0"), "a");

        assertThatThrownBy(() -> insertar(estudiante, 7, 1, "correcto", new BigDecimal("100"), "b"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
