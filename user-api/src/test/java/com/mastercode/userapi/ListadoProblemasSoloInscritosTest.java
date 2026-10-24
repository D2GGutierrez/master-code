package com.mastercode.userapi;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;

import com.mastercode.userapi.admin.ProblemaResumenDto;

// HU29-T4 (CA-029.1): solo un estudiante inscrito ve los problemas del curso. Completa a ProblemasApiTest
// (HU29-T1), que ya prueba el 200 del inscrito, el 403 sin llamar a admin-api del no inscrito, del inscrito
// en otro curso y cuando el inscrito es otro estudiante. Aquí va lo que faltaba: que la comprobación es por
// curso en los dos sentidos, que sin token o con token de docente tampoco se llega a admin-api, y que el
// mismo estudiante pasa de 403 a 200 al inscribirse.
class ListadoProblemasSoloInscritosTest extends PruebaDeApi {

    private static final ProblemaResumenDto PROBLEMA = new ProblemaResumenDto(7, "Suma de dos", "python", "facil",
            "web", "escritura_codigo", OffsetDateTime.parse("2026-10-28T15:00:00Z"));

    // Id alto y al azar: no choca con inscripciones de la base de desarrollo.
    private static long cursoAlAzar() {
        return ThreadLocalRandom.current().nextLong(1_000_000_000L, Long.MAX_VALUE);
    }

    @Test
    void la_inscripcion_en_un_curso_da_acceso_a_ese_y_no_a_otro() throws Exception {
        String email = correoUnico();
        long cursoX = cursoAlAzar();
        long cursoY = cursoX - 1;
        inscribir(crearEstudiante(email), cursoX);
        when(admin.problemasDelCurso(cursoX, null)).thenReturn(List.of(PROBLEMA));

        mvc.perform(get("/api/cursos/{id}/problemas", cursoY).with(tokenDe(email, "estudiante")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("No estás inscrito en este curso."));
        mvc.perform(get("/api/cursos/{id}/problemas", cursoX).with(tokenDe(email, "estudiante")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(7));

        // Solo se preguntó por X: el 403 de Y no llegó a admin-api.
        verify(admin).problemasDelCurso(cursoX, null);
        verifyNoMoreInteractions(admin);
    }

    @Test
    void sin_token_o_con_token_de_docente_no_se_llega_a_admin_api() throws Exception {
        long curso = cursoAlAzar();

        mvc.perform(get("/api/cursos/{id}/problemas", curso)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/cursos/{id}/problemas", curso).with(tokenDe(correoUnico(), "docente")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(admin);
    }

    @Test
    void el_mismo_estudiante_pasa_de_403_a_200_al_inscribirse() throws Exception {
        String email = correoUnico();
        long estudiante = crearEstudiante(email);
        long curso = cursoAlAzar();
        when(admin.problemasDelCurso(curso, null)).thenReturn(List.of(PROBLEMA));
        mvc.perform(get("/api/cursos/{id}/problemas", curso).with(tokenDe(email, "estudiante")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$[0]").doesNotExist());

        inscribir(estudiante, curso);

        mvc.perform(get("/api/cursos/{id}/problemas", curso).with(tokenDe(email, "estudiante")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo").value("Suma de dos"));
        verify(admin).problemasDelCurso(curso, null);
    }
}
