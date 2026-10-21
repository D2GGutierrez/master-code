package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import com.mastercode.userapi.perfil.EstudianteRepository;

// HU26 T2: la migración V3 crea app_inscripcion con UNIQUE (estudiante, curso) y sin FK a admin_schema.
// @Transactional: cada prueba deshace lo que insertó.
@SpringBootTest
@Transactional
class InscripcionMigracionTest {

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private EstudianteRepository estudiantes;

    private long crearEstudiante() {
        String email = "prueba-" + UUID.randomUUID() + "@ejemplo.edu.pe";
        estudiantes.insertarSiNoExiste(email, "ejemplo.edu.pe");
        return estudiantes.findByEmail(email).orElseThrow().getId();
    }

    private void inscribir(long estudianteId, long cursoId) {
        jdbc.sql("INSERT INTO app_schema.app_inscripcion (estudiante_id, curso_id) VALUES (?, ?)")
                .params(estudianteId, cursoId)
                .update();
    }

    @Test
    void una_inscripcion_se_guarda_con_fecha() {
        long estudiante = crearEstudiante();

        inscribir(estudiante, 10);

        Integer conFecha = jdbc.sql("SELECT count(*) FROM app_schema.app_inscripcion WHERE estudiante_id = ? AND fecha IS NOT NULL")
                .param(estudiante)
                .query(Integer.class)
                .single();
        assertThat(conFecha).isEqualTo(1);
    }

    @Test
    void la_misma_inscripcion_dos_veces_se_rechaza() {
        long estudiante = crearEstudiante();
        inscribir(estudiante, 10);

        assertThatThrownBy(() -> inscribir(estudiante, 10)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void el_mismo_estudiante_puede_inscribirse_a_otro_curso() {
        long estudiante = crearEstudiante();

        inscribir(estudiante, 10);
        inscribir(estudiante, 11);

        // Sin excepción: la restricción es por la pareja, no por el estudiante.
    }

    @Test
    void curso_id_no_tiene_clave_foranea_y_solo_hay_una_hacia_app_estudiante() {
        List<String> destinos = jdbc.sql("""
                        SELECT ccu.table_schema || '.' || ccu.table_name
                        FROM information_schema.table_constraints tc
                        JOIN information_schema.constraint_column_usage ccu ON ccu.constraint_name = tc.constraint_name
                        WHERE tc.table_schema = 'app_schema' AND tc.table_name = 'app_inscripcion'
                          AND tc.constraint_type = 'FOREIGN KEY'
                        """)
                .query(String.class)
                .list();

        assertThat(destinos).containsExactly("app_schema.app_estudiante");
    }
}
