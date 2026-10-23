package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.mastercode.userapi.admin.CursoDto;

// HU26-T4 (CA-026.3 a CA-026.5): completa InscripcionApiTest (HU26-T3), que ya prueba el código válido,
// el errado, el vencido, el 409 al repetir, sin token, docente y la normalización; el UNIQUE en la base lo
// prueba InscripcionMigracionTest. Aquí va lo que faltaba, por el endpoint: que la regla es por
// estudiante + curso, que repetir no toca la primera inscripción y que 401 y 403 no dejan filas.
class InscripcionCodigoInvitacionTest extends PruebaDeApi {

    private ResultActions pedirInscripcion(String email, String rol, String codigo) throws Exception {
        return mvc.perform(post("/api/inscripciones")
                .with(tokenDe(email, rol))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"" + codigo + "\"}"));
    }

    // Id alto y al azar: los conteos por curso no se mezclan con datos de la base de desarrollo.
    private static CursoDto cursoNuevo(String nombre) {
        long id = ThreadLocalRandom.current().nextLong(1_000_000_000L, Long.MAX_VALUE);
        return new CursoDto(id, nombre, "python", "ejemplo.edu.pe");
    }

    private int inscripcionesDelCurso(long cursoId) {
        return jdbc.sql("SELECT count(*) FROM app_schema.app_inscripcion WHERE curso_id = ?")
                .param(cursoId)
                .query(Integer.class)
                .single();
    }

    private OffsetDateTime fechaDeInscripcion(long estudianteId, long cursoId) {
        return jdbc.sql("SELECT fecha FROM app_schema.app_inscripcion WHERE estudiante_id = ? AND curso_id = ?")
                .params(estudianteId, cursoId)
                .query(OffsetDateTime.class)
                .single();
    }

    @Test
    void repetir_la_inscripcion_no_cambia_la_primera() throws Exception {
        String email = correoUnico();
        long id = crearEstudiante(email);
        CursoDto curso = cursoNuevo("Algoritmos I");
        when(admin.cursoPorCodigo("ABCD2345")).thenReturn(curso);
        pedirInscripcion(email, "estudiante", "ABCD2345").andExpect(status().isCreated());
        OffsetDateTime primera = fechaDeInscripcion(id, curso.id());

        pedirInscripcion(email, "estudiante", "ABCD2345").andExpect(status().isConflict());

        assertThat(inscripcionesDe(id)).isEqualTo(1);
        assertThat(fechaDeInscripcion(id, curso.id())).isEqualTo(primera);
    }

    @Test
    void otro_estudiante_puede_inscribirse_en_el_mismo_curso() throws Exception {
        String primero = correoUnico();
        String segundo = correoUnico();
        crearEstudiante(primero);
        crearEstudiante(segundo);
        CursoDto curso = cursoNuevo("Algoritmos I");
        when(admin.cursoPorCodigo("ABCD2345")).thenReturn(curso);

        pedirInscripcion(primero, "estudiante", "ABCD2345").andExpect(status().isCreated());
        pedirInscripcion(segundo, "estudiante", "ABCD2345").andExpect(status().isCreated());

        assertThat(inscripcionesDelCurso(curso.id())).isEqualTo(2);
    }

    @Test
    void el_mismo_estudiante_puede_inscribirse_en_otro_curso() throws Exception {
        String email = correoUnico();
        long id = crearEstudiante(email);
        when(admin.cursoPorCodigo("ABCD2345")).thenReturn(cursoNuevo("Algoritmos I"));
        when(admin.cursoPorCodigo("EFGH6789")).thenReturn(cursoNuevo("Estructuras"));

        pedirInscripcion(email, "estudiante", "ABCD2345").andExpect(status().isCreated());
        pedirInscripcion(email, "estudiante", "EFGH6789").andExpect(status().isCreated());

        assertThat(inscripcionesDe(id)).isEqualTo(2);
    }

    @Test
    void sin_token_o_con_token_de_docente_no_se_crea_ninguna_inscripcion() throws Exception {
        CursoDto curso = cursoNuevo("Algoritmos I");
        when(admin.cursoPorCodigo("ABCD2345")).thenReturn(curso);

        mvc.perform(post("/api/inscripciones").contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"ABCD2345\"}"))
                .andExpect(status().isUnauthorized());
        pedirInscripcion(correoUnico(), "docente", "ABCD2345").andExpect(status().isForbidden());

        assertThat(inscripcionesDelCurso(curso.id())).isZero();
    }
}
