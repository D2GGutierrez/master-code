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

// HU37-T1: la migración V7 crea app_resultado_caso_prueba, una fila por caso evaluado en un envío. @Transactional: cada
// prueba deshace lo que insertó. Las filas que deben fallar van al final de cada prueba: en PostgreSQL, una sentencia
// fallida deja abortada la transacción.
@SpringBootTest
@Transactional
class ResultadoCasoMigracionTest {

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private EstudianteRepository estudiantes;

    private long crearEnvio() {
        String email = "prueba-" + UUID.randomUUID() + "@ejemplo.edu.pe";
        estudiantes.insertarSiNoExiste(email, "ejemplo.edu.pe");
        long estudiante = estudiantes.findByEmail(email).orElseThrow().getId();
        return jdbc.sql("""
                INSERT INTO app_schema.app_envio (estudiante_id, problema_id, plataforma)
                VALUES (?, 7, 'web')
                RETURNING id
                """)
                .param(estudiante)
                .query(Long.class)
                .single();
    }

    private void insertar(long envioId, long casoId, Boolean paso, String salida) {
        jdbc.sql("""
                INSERT INTO app_schema.app_resultado_caso_prueba (envio_id, caso_prueba_id, paso, salida_obtenida)
                VALUES (?, ?, ?, ?)
                """)
                .params(envioId, casoId, paso, salida)
                .update();
    }

    private int contar(long envioId) {
        return jdbc.sql("SELECT count(*) FROM app_schema.app_resultado_caso_prueba WHERE envio_id = ?")
                .param(envioId).query(Integer.class).single();
    }

    @Test
    void guarda_una_fila_por_caso_de_un_envio() {
        long envio = crearEnvio();

        insertar(envio, 31, true, "5");
        insertar(envio, 32, false, "otra cosa");

        assertThat(contar(envio)).isEqualTo(2);
    }

    @Test
    void sin_salida_obtenida_queda_vacia() {
        long envio = crearEnvio();

        jdbc.sql("INSERT INTO app_schema.app_resultado_caso_prueba (envio_id, caso_prueba_id, paso) VALUES (?, 31, false)")
                .param(envio).update();

        String salida = jdbc.sql("SELECT salida_obtenida FROM app_schema.app_resultado_caso_prueba WHERE envio_id = ?")
                .param(envio).query(String.class).single();
        assertThat(salida).isEmpty();
    }

    @Test
    void guarda_la_salida_completa_aunque_sea_larga() {
        long envio = crearEnvio();
        String salida = "x".repeat(50_000);

        insertar(envio, 31, true, salida);

        String guardada = jdbc.sql("SELECT salida_obtenida FROM app_schema.app_resultado_caso_prueba WHERE envio_id = ?")
                .param(envio).query(String.class).single();
        assertThat(guardada).isEqualTo(salida);
    }

    @Test
    void el_mismo_caso_se_permite_en_otro_envio() {
        long uno = crearEnvio();
        long otro = crearEnvio();

        insertar(uno, 31, true, "5");
        insertar(otro, 31, false, "");

        assertThat(contar(uno)).isEqualTo(1);
        assertThat(contar(otro)).isEqualTo(1);
    }

    @Test
    void si_paso_es_obligatorio() {
        long envio = crearEnvio();

        assertThatThrownBy(() -> insertar(envio, 31, null, "5"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechaza_un_resultado_de_un_envio_que_no_existe() {
        assertThatThrownBy(() -> insertar(Long.MAX_VALUE, 31, true, "5"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechaza_el_mismo_caso_dos_veces_en_un_envio() {
        long envio = crearEnvio();
        insertar(envio, 31, true, "5");

        assertThatThrownBy(() -> insertar(envio, 31, false, "otra"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
