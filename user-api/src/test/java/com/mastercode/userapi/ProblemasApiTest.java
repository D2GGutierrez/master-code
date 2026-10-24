package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.mastercode.userapi.admin.AdminNoDisponibleException;
import com.mastercode.userapi.admin.CursoNoExisteException;
import com.mastercode.userapi.admin.ProblemaDto;
import com.mastercode.userapi.admin.ProblemaNoVisibleException;
import com.mastercode.userapi.admin.ProblemaResumenDto;

// HU29-T1 (listado de problemas de un curso, solo si el estudiante está inscrito), HU31-T1/T2 (solo aprobados,
// también al abrir uno por id) y HU32-T1 (detalle sin casos ocultos). Qué problemas son visibles lo decide
// admin-api (su API interna parte de visibles_para_estudiante y tiene sus propias pruebas): aquí se comprueba
// quién puede preguntar y que a un estudiante solo llega lo que admin-api entrega como visible.
class ProblemasApiTest extends PruebaDeApi {

    private static final ProblemaResumenDto P7 = new ProblemaResumenDto(7, "Suma de dos", "python", "facil", "web",
            "escritura_codigo", OffsetDateTime.parse("2026-10-28T15:00:00Z"));
    private static final ProblemaResumenDto P8 = new ProblemaResumenDto(8, "¿Qué imprime?", "python", "medio", "movil",
            "prediccion_salida", OffsetDateTime.parse("2026-10-28T16:00:00Z"));

    private static final ProblemaDto DETALLE_P7 = new ProblemaDto(7, "Suma de dos", "Lee dos enteros e imprime su suma.",
            "python", "facil", "web", "escritura_codigo",
            List.of(new ProblemaDto.Caso("2 3", "5")), 10);

    // ---------- listado: GET /api/cursos/{cursoId}/problemas ----------

    @Test
    void un_estudiante_inscrito_ve_los_problemas_del_curso() throws Exception {
        String email = correoUnico();
        inscribir(crearEstudiante(email), 10);
        when(admin.problemasDelCurso(10, null)).thenReturn(List.of(P7, P8));

        mvc.perform(get("/api/cursos/10/problemas").with(tokenDe(email, "estudiante")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].titulo").value("Suma de dos"))
                .andExpect(jsonPath("$[0].lenguaje").value("python"))
                // CA-29.2 y CA-29.3: cada problema trae su dificultad y su tipo de ejercicio.
                .andExpect(jsonPath("$[0].dificultad").value("facil"))
                .andExpect(jsonPath("$[0].tipo_ejercicio").value("escritura_codigo"))
                .andExpect(jsonPath("$[0].plataforma").value("web"))
                .andExpect(jsonPath("$[0].fecha_publicacion").value(startsWith("2026-10-28T15:00")))
                .andExpect(jsonPath("$[1].id").value(8))
                .andExpect(jsonPath("$[1].tipo_ejercicio").value("prediccion_salida"))
                // El listado no lleva el enunciado ni casos: eso se pide al abrir el problema.
                .andExpect(jsonPath("$[0].enunciado").doesNotExist())
                .andExpect(jsonPath("$[0].casos_prueba").doesNotExist());

        verify(admin).problemasDelCurso(10, null);
    }

    @Test
    void un_estudiante_no_inscrito_recibe_403_sin_que_se_llame_a_admin_api() throws Exception {
        String email = correoUnico();
        crearEstudiante(email);

        mvc.perform(get("/api/cursos/10/problemas").with(tokenDe(email, "estudiante")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("No estás inscrito en este curso."));

        verifyNoInteractions(admin);
    }

    @Test
    void estar_inscrito_en_otro_curso_no_da_acceso() throws Exception {
        String email = correoUnico();
        inscribir(crearEstudiante(email), 11);

        mvc.perform(get("/api/cursos/10/problemas").with(tokenDe(email, "estudiante")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(admin);
    }

    @Test
    void la_inscripcion_de_otro_estudiante_no_da_acceso() throws Exception {
        String email = correoUnico();
        crearEstudiante(email);
        inscribir(crearEstudiante(correoUnico()), 10);

        mvc.perform(get("/api/cursos/10/problemas").with(tokenDe(email, "estudiante")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(admin);
    }

    @Test
    void el_filtro_de_plataforma_se_pasa_a_admin_api() throws Exception {
        String email = correoUnico();
        inscribir(crearEstudiante(email), 10);
        when(admin.problemasDelCurso(10, "movil")).thenReturn(List.of(P8));

        mvc.perform(get("/api/cursos/10/problemas?plataforma=movil").with(tokenDe(email, "estudiante")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(8));

        verify(admin).problemasDelCurso(10, "movil");
    }

    @Test
    void un_filtro_de_plataforma_vacio_equivale_a_no_filtrar() throws Exception {
        String email = correoUnico();
        inscribir(crearEstudiante(email), 10);
        when(admin.problemasDelCurso(10, null)).thenReturn(List.of(P7));

        mvc.perform(get("/api/cursos/10/problemas?plataforma=").with(tokenDe(email, "estudiante")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(admin).problemasDelCurso(10, null);
    }

    @Test
    void una_plataforma_que_no_existe_da_400_sin_llamar_a_admin_api() throws Exception {
        String email = correoUnico();
        inscribir(crearEstudiante(email), 10);

        mvc.perform(get("/api/cursos/10/problemas?plataforma=ambas").with(tokenDe(email, "estudiante")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("plataforma debe ser web o movil."));

        verifyNoInteractions(admin);
    }

    @Test
    void un_curso_sin_problemas_aprobados_da_lista_vacia() throws Exception {
        String email = correoUnico();
        inscribir(crearEstudiante(email), 10);
        when(admin.problemasDelCurso(10, null)).thenReturn(List.of());

        mvc.perform(get("/api/cursos/10/problemas").with(tokenDe(email, "estudiante")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void si_admin_api_ya_no_tiene_el_curso_da_404() throws Exception {
        String email = correoUnico();
        inscribir(crearEstudiante(email), 10);
        when(admin.problemasDelCurso(10, null)).thenThrow(new CursoNoExisteException());

        mvc.perform(get("/api/cursos/10/problemas").with(tokenDe(email, "estudiante")))
                .andExpect(status().isNotFound());
    }

    @Test
    void si_admin_api_no_responde_el_listado_da_503() throws Exception {
        String email = correoUnico();
        inscribir(crearEstudiante(email), 10);
        when(admin.problemasDelCurso(10, null)).thenThrow(new AdminNoDisponibleException("sin conexión", null));

        mvc.perform(get("/api/cursos/10/problemas").with(tokenDe(email, "estudiante")))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void el_listado_sin_token_da_401_y_con_token_de_docente_403() throws Exception {
        mvc.perform(get("/api/cursos/10/problemas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/cursos/10/problemas").with(tokenDe(correoUnico(), "docente")))
                .andExpect(status().isForbidden());
    }

    // ---------- detalle: GET /api/problemas/{problemaId} ----------

    @Test
    void un_estudiante_inscrito_abre_un_problema_con_sus_casos_de_ejemplo() throws Exception {
        String email = correoUnico();
        inscribir(crearEstudiante(email), 10);
        when(admin.problema(7)).thenReturn(DETALLE_P7);

        mvc.perform(get("/api/problemas/7").with(tokenDe(email, "estudiante")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.curso_id").value(10))
                .andExpect(jsonPath("$.titulo").value("Suma de dos"))
                .andExpect(jsonPath("$.enunciado").value("Lee dos enteros e imprime su suma."))
                .andExpect(jsonPath("$.lenguaje").value("python"))
                .andExpect(jsonPath("$.dificultad").value("facil"))
                .andExpect(jsonPath("$.plataforma").value("web"))
                .andExpect(jsonPath("$.tipo_ejercicio").value("escritura_codigo"))
                .andExpect(jsonPath("$.casos_prueba", hasSize(1)))
                .andExpect(jsonPath("$.casos_prueba[0].entrada").value("2 3"))
                .andExpect(jsonPath("$.casos_prueba[0].salida_esperada").value("5"));
    }

    @Test
    void el_detalle_trae_solo_los_campos_previstos() throws Exception {
        String email = correoUnico();
        inscribir(crearEstudiante(email), 10);
        when(admin.problema(7)).thenReturn(DETALLE_P7);

        // Nueve campos exactos: lo que no se nombra aquí no sale hacia el estudiante.
        mvc.perform(get("/api/problemas/7").with(tokenDe(email, "estudiante")))
                .andExpect(jsonPath("$.*", hasSize(9)))
                .andExpect(jsonPath("$.casos_prueba[0].*", hasSize(2)))
                .andExpect(jsonPath("$.respuesta_esperada").doesNotExist())
                .andExpect(jsonPath("$.opciones").doesNotExist())
                .andExpect(jsonPath("$.aprobado_por_docente").doesNotExist());
    }

    @Test
    void un_problema_sin_casos_en_la_respuesta_de_admin_api_se_entrega_con_lista_vacia() throws Exception {
        String email = correoUnico();
        inscribir(crearEstudiante(email), 10);
        when(admin.problema(8)).thenReturn(new ProblemaDto(8, "¿Qué imprime?", "Piensa la salida.", "python", "medio",
                "movil", "prediccion_salida", null, 10));

        mvc.perform(get("/api/problemas/8").with(tokenDe(email, "estudiante")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.casos_prueba", hasSize(0)));
    }

    // HU31-T2 (CA-31.2): admin-api responde 404 a un problema sin aprobar, descartado o inexistente.
    @Test
    void un_problema_que_admin_api_no_entrega_como_visible_da_404() throws Exception {
        String email = correoUnico();
        inscribir(crearEstudiante(email), 10);
        when(admin.problema(9)).thenThrow(new ProblemaNoVisibleException());

        mvc.perform(get("/api/problemas/9").with(tokenDe(email, "estudiante")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Problema no encontrado."));
    }

    @Test
    void un_problema_de_un_curso_sin_inscripcion_responde_igual_que_uno_inexistente() throws Exception {
        String email = correoUnico();
        // Inscrito, pero en otro curso que el del problema 7.
        inscribir(crearEstudiante(email), 11);
        when(admin.problema(7)).thenReturn(DETALLE_P7);
        when(admin.problema(999)).thenThrow(new ProblemaNoVisibleException());

        var sinInscripcion = mvc.perform(get("/api/problemas/7").with(tokenDe(email, "estudiante")))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();
        var inexistente = mvc.perform(get("/api/problemas/999").with(tokenDe(email, "estudiante")))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        // Mismo cuerpo: adivinar ids no revela cuáles existen ni de qué curso son.
        assertThat(sinInscripcion).isEqualTo(inexistente);
    }

    @Test
    void la_inscripcion_de_otro_estudiante_no_abre_el_problema() throws Exception {
        String email = correoUnico();
        crearEstudiante(email);
        inscribir(crearEstudiante(correoUnico()), 10);
        when(admin.problema(7)).thenReturn(DETALLE_P7);

        mvc.perform(get("/api/problemas/7").with(tokenDe(email, "estudiante")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.enunciado").doesNotExist());
    }

    @Test
    void si_admin_api_no_responde_el_detalle_da_503() throws Exception {
        String email = correoUnico();
        inscribir(crearEstudiante(email), 10);
        when(admin.problema(7)).thenThrow(new AdminNoDisponibleException("sin conexión", null));

        mvc.perform(get("/api/problemas/7").with(tokenDe(email, "estudiante")))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void el_detalle_sin_token_da_401_y_con_token_de_docente_403() throws Exception {
        mvc.perform(get("/api/problemas/7")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/problemas/7").with(tokenDe(correoUnico(), "docente")))
                .andExpect(status().isForbidden());
    }
}
