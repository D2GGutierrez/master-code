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

// HU30-T2: la migración V4 agrega app_envio.resultado (vacío, correcto o incorrecto) y un índice por estudiante.
// @Transactional: cada prueba deshace lo que insertó.
@SpringBootTest
@Transactional
class EnvioResultadoMigracionTest {

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private EstudianteRepository estudiantes;

    private long crearEstudiante() {
        String email = "prueba-" + UUID.randomUUID() + "@ejemplo.edu.pe";
        estudiantes.insertarSiNoExiste(email, "ejemplo.edu.pe");
        return estudiantes.findByEmail(email).orElseThrow().getId();
    }

    private void enviarConResultado(long estudianteId, String resultado) {
        jdbc.sql("INSERT INTO app_schema.app_envio (estudiante_id, problema_id, plataforma, resultado) VALUES (?, 7, 'web', ?)")
                .params(estudianteId, resultado)
                .update();
    }

    @Test
    void un_envio_sin_resultado_se_sigue_guardando() {
        long estudiante = crearEstudiante();

        // Los envíos anteriores a la columna y los que aún no se califican no tienen resultado.
        jdbc.sql("INSERT INTO app_schema.app_envio (estudiante_id, problema_id, plataforma) VALUES (?, 7, 'web')")
                .param(estudiante)
                .update();

        Integer sinResultado = jdbc.sql("SELECT count(*) FROM app_schema.app_envio WHERE estudiante_id = ? AND resultado IS NULL")
                .param(estudiante)
                .query(Integer.class)
                .single();
        assertThat(sinResultado).isEqualTo(1);
    }

    @Test
    void acepta_correcto_e_incorrecto() {
        long estudiante = crearEstudiante();

        enviarConResultado(estudiante, "correcto");
        enviarConResultado(estudiante, "incorrecto");

        Integer guardados = jdbc.sql("SELECT count(*) FROM app_schema.app_envio WHERE estudiante_id = ? AND resultado IS NOT NULL")
                .param(estudiante)
                .query(Integer.class)
                .single();
        assertThat(guardados).isEqualTo(2);
    }

    @Test
    void rechaza_cualquier_otro_resultado() {
        long estudiante = crearEstudiante();

        // Un error de escritura («Correcto») dejaría un problema resuelto sin contarse nunca.
        assertThatThrownBy(() -> enviarConResultado(estudiante, "Correcto")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existe_el_indice_por_estudiante_y_problema() {
        Integer indices = jdbc.sql("""
                SELECT count(*) FROM pg_indexes
                WHERE schemaname = 'app_schema' AND tablename = 'app_envio'
                  AND indexname = 'ix_app_envio_estudiante_problema'
                """)
                .query(Integer.class)
                .single();

        assertThat(indices).isEqualTo(1);
    }
}
