package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.mastercode.userapi.admin.AdminNoDisponibleException;
import com.mastercode.userapi.admin.EvaluacionDto;
import com.mastercode.userapi.admin.EvaluacionNoExisteException;

// HU64-T2 (CA-64.1 a CA-64.3): GET /api/evaluaciones/{id} solo deja entrar dentro de la ventana. El estado lo calcula
// admin-api (HU64-T1) y aquí es un mock que responde programada, activa o cerrada: lo que se prueba es que user-api
// cierra la puerta antes y después, la abre durante y no revela nada fuera de ella.
class EvaluacionAccesoApiTest extends PruebaDeApi {

    private static final long CURSO = 10;
    private static final long EVALUACION = 5;
    private static final OffsetDateTime INICIO = OffsetDateTime.parse("2026-11-12T14:00:00Z");
    private static final OffsetDateTime FIN = OffsetDateTime.parse("2026-11-12T16:00:00Z");

    private record Alumno(String email, long id) {
    }

    private Alumno alumnoInscrito() {
        String email = correoUnico();
        long id = crearEstudiante(email);
        inscribir(id, CURSO);
        return new Alumno(email, id);
    }

    private static EvaluacionDto evaluacion(String estado, long curso) {
        return new EvaluacionDto(EVALUACION, curso, INICIO, FIN, 90, estado, List.of(7L, 8L));
    }

    private ResultActions entrar(Alumno alumno) throws Exception {
        return mvc.perform(get("/api/evaluaciones/" + EVALUACION).with(tokenDe(alumno.email(), "estudiante")));
    }

    // CA-64.2
    @Test
    void durante_la_ventana_el_estudiante_inscrito_entra_y_recibe_la_ventana_el_limite_y_los_problemas()
            throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.evaluacion(EVALUACION)).thenReturn(evaluacion("activa", CURSO));

        entrar(yo)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(EVALUACION))
                .andExpect(jsonPath("$.curso_id").value(CURSO))
                .andExpect(jsonPath("$.estado").value("activa"))
                .andExpect(jsonPath("$.limite_minutos").value(90))
                .andExpect(jsonPath("$.fecha_inicio").exists())
                .andExpect(jsonPath("$.fecha_fin").exists())
                .andExpect(jsonPath("$.problemas[0]").value(7))
                .andExpect(jsonPath("$.problemas[1]").value(8));
    }

    // CA-64.1
    @Test
    void antes_de_la_ventana_no_entra_y_la_respuesta_trae_las_fechas_pero_ningun_problema() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.evaluacion(EVALUACION)).thenReturn(evaluacion("programada", CURSO));

        MvcResult respuesta = entrar(yo)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("La evaluación todavía no comienza."))
                .andExpect(jsonPath("$.estado").value("programada"))
                .andExpect(jsonPath("$.fecha_inicio").exists())
                .andExpect(jsonPath("$.fecha_fin").exists())
                .andExpect(jsonPath("$.problemas").doesNotExist())
                .andReturn();

        assertThat(respuesta.getResponse().getContentAsString()).doesNotContain("problemas");
    }

    // CA-64.3
    @Test
    void despues_de_la_ventana_no_entra_y_la_respuesta_no_trae_problemas() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.evaluacion(EVALUACION)).thenReturn(evaluacion("cerrada", CURSO));

        entrar(yo)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("La evaluación ya terminó."))
                .andExpect(jsonPath("$.estado").value("cerrada"))
                .andExpect(jsonPath("$.problemas").doesNotExist());
    }

    // Una puerta que solo se abre con «activa»: un estado que user-api no conoce no deja pasar a nadie.
    @Test
    void un_estado_desconocido_no_deja_entrar() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.evaluacion(EVALUACION)).thenReturn(evaluacion("otra-cosa", CURSO));

        entrar(yo).andExpect(status().isForbidden()).andExpect(jsonPath("$.problemas").doesNotExist());
    }

    @Test
    void la_misma_evaluacion_pasa_de_cerrada_a_abierta_cuando_admin_api_la_da_por_activa() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.evaluacion(EVALUACION)).thenReturn(evaluacion("programada", CURSO));
        entrar(yo).andExpect(status().isForbidden());

        when(admin.evaluacion(EVALUACION)).thenReturn(evaluacion("activa", CURSO));
        entrar(yo).andExpect(status().isOk());

        when(admin.evaluacion(EVALUACION)).thenReturn(evaluacion("cerrada", CURSO));
        entrar(yo).andExpect(status().isForbidden());
    }

    // Sin estar inscrito no se sabe ni que existe: da el mismo 404 que una evaluación inexistente, también en la ventana.
    @Test
    void un_estudiante_no_inscrito_en_el_curso_recibe_el_mismo_404_que_si_no_existiera() throws Exception {
        Alumno yo = alumnoInscrito();
        String ajeno = correoUnico();
        crearEstudiante(ajeno);
        when(admin.evaluacion(EVALUACION)).thenReturn(evaluacion("activa", CURSO));

        String sinInscripcion = mvc.perform(get("/api/evaluaciones/" + EVALUACION).with(tokenDe(ajeno, "estudiante")))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();

        when(admin.evaluacion(EVALUACION)).thenThrow(new EvaluacionNoExisteException());
        String inexistente = entrar(yo).andExpect(status().isNotFound()).andReturn().getResponse()
                .getContentAsString();
        assertThat(sinInscripcion).isEqualTo(inexistente);
    }

    @Test
    void una_evaluacion_de_otro_curso_donde_no_esta_inscrito_da_404_aunque_este_activa() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.evaluacion(EVALUACION)).thenReturn(evaluacion("activa", 55));

        entrar(yo).andExpect(status().isNotFound()).andExpect(jsonPath("$.problemas").doesNotExist());
    }

    @Test
    void si_admin_api_no_responde_da_503_y_no_deja_entrar() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.evaluacion(EVALUACION)).thenThrow(new AdminNoDisponibleException("sin conexión", null));

        entrar(yo).andExpect(status().isServiceUnavailable());
    }

    @Test
    void sin_token_da_401_y_con_token_de_docente_403() throws Exception {
        mvc.perform(get("/api/evaluaciones/" + EVALUACION)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/evaluaciones/" + EVALUACION).with(tokenDe(correoUnico(), "docente")))
                .andExpect(status().isForbidden());
    }
}
