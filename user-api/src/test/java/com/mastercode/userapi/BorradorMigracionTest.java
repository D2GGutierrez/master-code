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

// HU35-T1: la migración V6 crea app_borrador, con un solo borrador por estudiante y problema. @Transactional: cada
// prueba deshace lo que insertó. Las filas que deben fallar van al final de cada prueba: en PostgreSQL, una sentencia
// fallida deja abortada la transacción.
@SpringBootTest
@Transactional
class BorradorMigracionTest {

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private EstudianteRepository estudiantes;

    private long crearEstudiante() {
        String email = "prueba-" + UUID.randomUUID() + "@ejemplo.edu.pe";
        estudiantes.insertarSiNoExiste(email, "ejemplo.edu.pe");
        return estudiantes.findByEmail(email).orElseThrow().getId();
    }

    private void insertar(long estudianteId, long problemaId, String codigo) {
        jdbc.sql("INSERT INTO app_schema.app_borrador (estudiante_id, problema_id, codigo) VALUES (?, ?, ?)")
                .params(estudianteId, problemaId, codigo)
                .update();
    }

    private int contar(long estudianteId) {
        return jdbc.sql("SELECT count(*) FROM app_schema.app_borrador WHERE estudiante_id = ?")
                .param(estudianteId).query(Integer.class).single();
    }

    @Test
    void guarda_un_borrador_con_fecha_de_actualizacion() {
        long estudiante = crearEstudiante();

        insertar(estudiante, 7, "print(1)");

        Boolean conFecha = jdbc.sql(
                "SELECT fecha_actualizacion IS NOT NULL FROM app_schema.app_borrador WHERE estudiante_id = ?")
                .param(estudiante).query(Boolean.class).single();
        assertThat(conFecha).isTrue();
    }

    @Test
    void permite_un_borrador_por_problema_para_el_mismo_estudiante() {
        long estudiante = crearEstudiante();

        insertar(estudiante, 7, "a");
        insertar(estudiante, 8, "b");

        assertThat(contar(estudiante)).isEqualTo(2);
    }

    @Test
    void permite_el_mismo_problema_a_estudiantes_distintos() {
        long uno = crearEstudiante();
        long otro = crearEstudiante();

        insertar(uno, 7, "a");
        insertar(otro, 7, "b");

        assertThat(contar(uno)).isEqualTo(1);
        assertThat(contar(otro)).isEqualTo(1);
    }

    @Test
    void rechaza_un_segundo_borrador_del_mismo_estudiante_y_problema() {
        long estudiante = crearEstudiante();
        insertar(estudiante, 7, "a");

        assertThatThrownBy(() -> insertar(estudiante, 7, "b"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechaza_un_borrador_de_un_estudiante_que_no_existe() {
        assertThatThrownBy(() -> insertar(Long.MAX_VALUE, 7, "a"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void el_codigo_es_obligatorio() {
        long estudiante = crearEstudiante();

        assertThatThrownBy(() -> insertar(estudiante, 7, null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void guarda_el_codigo_completo_aunque_sea_largo() {
        long estudiante = crearEstudiante();
        String codigo = "print('x')\n".repeat(5000);

        insertar(estudiante, 7, codigo);

        String guardado = jdbc.sql("SELECT codigo FROM app_schema.app_borrador WHERE estudiante_id = ?")
                .param(estudiante).query(String.class).single();
        assertThat(guardado).isEqualTo(codigo);
    }
}
