package com.mastercode.userapi;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.mastercode.userapi.admin.AdminNoDisponibleException;
import com.mastercode.userapi.admin.CursoNoExisteException;
import com.mastercode.userapi.admin.ProblemaResumenDto;

// HU43-T1: GET /api/cursos/{id}/avance. Los envíos se siembran en app_envio; admin-api es un mock que entrega los
// problemas que el estudiante ve en el curso 10.
class AvanceDelCursoApiTest extends PruebaDeApi {

    private static final OffsetDateTime FECHA = OffsetDateTime.parse("2026-10-28T15:00:00Z");
    private static final ProblemaResumenDto SUMA = problema(7, "web");
    private static final ProblemaResumenDto PREDICE = problema(8, "movil");
    private static final ProblemaResumenDto ORDENA = problema(9, "ambas");

    private static ProblemaResumenDto problema(long id, String plataforma) {
        return new ProblemaResumenDto(id, "Problema " + id, "python", "facil", plataforma, "escritura_codigo", FECHA);
    }

    private record Alumno(String email, long id) {
    }

    private Alumno alumnoInscrito() {
        String email = correoUnico();
        long id = crearEstudiante(email);
        inscribir(id, 10);
        return new Alumno(email, id);
    }

    private ResultActions avance(String email) throws Exception {
        return mvc.perform(get("/api/cursos/10/avance").with(tokenDe(email, "estudiante")));
    }

    @Test
    void cuenta_resueltos_intentados_y_pendientes() throws Exception {
        Alumno yo = alumnoInscrito();
        registrarEnvio(yo.id(), 7, "web", "correcto");
        registrarEnvio(yo.id(), 8, "movil", "incorrecto");
        when(admin.problemasDelCurso(10, null)).thenReturn(List.of(SUMA, PREDICE, ORDENA));

        avance(yo.email())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.curso_id").value(10))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.resueltos").value(1))
                .andExpect(jsonPath("$.intentados").value(1))
                .andExpect(jsonPath("$.pendientes").value(1));
    }

    @Test
    void un_envio_correcto_basta_aunque_haya_otros_que_fallaron() throws Exception {
        Alumno yo = alumnoInscrito();
        registrarEnvio(yo.id(), 7, "web", "incorrecto");
        registrarEnvio(yo.id(), 7, "web", "correcto");
        registrarEnvio(yo.id(), 7, "web", "parcial");
        when(admin.problemasDelCurso(10, null)).thenReturn(List.of(SUMA));

        avance(yo.email())
                .andExpect(jsonPath("$.resueltos").value(1))
                .andExpect(jsonPath("$.intentados").value(0));
    }

    @Test
    void un_envio_parcial_o_sin_calificar_cuenta_como_intentado() throws Exception {
        Alumno yo = alumnoInscrito();
        registrarEnvio(yo.id(), 7, "web", "parcial");
        registrarEnvio(yo.id(), 9, "web", null);
        when(admin.problemasDelCurso(10, null)).thenReturn(List.of(SUMA, ORDENA));

        avance(yo.email())
                .andExpect(jsonPath("$.resueltos").value(0))
                .andExpect(jsonPath("$.intentados").value(2))
                .andExpect(jsonPath("$.pendientes").value(0));
    }

    @Test
    void los_envios_de_web_y_de_movil_cuentan_en_la_misma_cuenta() throws Exception {
        Alumno yo = alumnoInscrito();
        registrarEnvio(yo.id(), 7, "web", "correcto");
        registrarEnvio(yo.id(), 8, "movil", "correcto");
        when(admin.problemasDelCurso(10, null)).thenReturn(List.of(SUMA, PREDICE));

        avance(yo.email()).andExpect(jsonPath("$.resueltos").value(2));
    }

    @Test
    void un_curso_sin_problemas_da_todo_en_cero() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.problemasDelCurso(10, null)).thenReturn(List.of());

        avance(yo.email())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.resueltos").value(0))
                .andExpect(jsonPath("$.intentados").value(0))
                .andExpect(jsonPath("$.pendientes").value(0));
    }

    @Test
    void un_curso_terminado_da_todo_resuelto_y_ninguno_pendiente() throws Exception {
        Alumno yo = alumnoInscrito();
        registrarEnvio(yo.id(), 7, "web", "correcto");
        registrarEnvio(yo.id(), 8, "movil", "correcto");
        registrarEnvio(yo.id(), 9, "web", "correcto");
        when(admin.problemasDelCurso(10, null)).thenReturn(List.of(SUMA, PREDICE, ORDENA));

        avance(yo.email())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.resueltos").value(3))
                .andExpect(jsonPath("$.intentados").value(0))
                .andExpect(jsonPath("$.pendientes").value(0));
    }

    // HU43-T4: solo cuentan los problemas que el estudiante ve en el curso; un envío de otro problema no suma.
    @Test
    void los_envios_de_un_problema_que_no_esta_en_la_lista_no_cuentan() throws Exception {
        Alumno yo = alumnoInscrito();
        registrarEnvio(yo.id(), 99, "web", "correcto");
        registrarEnvio(yo.id(), 7, "web", "incorrecto");
        when(admin.problemasDelCurso(10, null)).thenReturn(List.of(SUMA, ORDENA));

        avance(yo.email())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.resueltos").value(0))
                .andExpect(jsonPath("$.intentados").value(1))
                .andExpect(jsonPath("$.pendientes").value(1));
    }

    @Test
    void los_envios_de_otro_estudiante_no_cambian_mi_avance() throws Exception {
        Alumno yo = alumnoInscrito();
        Alumno otro = alumnoInscrito();
        registrarEnvio(otro.id(), 7, "web", "correcto");
        when(admin.problemasDelCurso(10, null)).thenReturn(List.of(SUMA));

        avance(yo.email())
                .andExpect(jsonPath("$.resueltos").value(0))
                .andExpect(jsonPath("$.pendientes").value(1));
    }

    @Test
    void la_plataforma_se_le_pasa_a_admin_api_como_en_el_listado() throws Exception {
        Alumno yo = alumnoInscrito();
        registrarEnvio(yo.id(), 7, "web", "correcto");
        when(admin.problemasDelCurso(10, "web")).thenReturn(List.of(SUMA, ORDENA));

        mvc.perform(get("/api/cursos/10/avance?plataforma=web").with(tokenDe(yo.email(), "estudiante")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.resueltos").value(1));

        verify(admin).problemasDelCurso(10, "web");
    }

    @Test
    void una_plataforma_que_no_existe_da_400_sin_llamar_a_admin_api() throws Exception {
        Alumno yo = alumnoInscrito();

        mvc.perform(get("/api/cursos/10/avance?plataforma=ios").with(tokenDe(yo.email(), "estudiante")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(admin);
    }

    @Test
    void sin_inscripcion_da_403_sin_llamar_a_admin_api() throws Exception {
        String email = correoUnico();
        crearEstudiante(email);

        avance(email).andExpect(status().isForbidden());

        verifyNoInteractions(admin);
    }

    @Test
    void un_curso_que_ya_no_existe_da_404() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.problemasDelCurso(10, null)).thenThrow(new CursoNoExisteException());

        avance(yo.email()).andExpect(status().isNotFound());
    }

    @Test
    void si_admin_api_no_responde_da_503() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.problemasDelCurso(10, null)).thenThrow(new AdminNoDisponibleException("sin conexión", null));

        avance(yo.email()).andExpect(status().isServiceUnavailable());
    }

    @Test
    void sin_token_da_401_y_con_token_de_docente_403() throws Exception {
        mvc.perform(get("/api/cursos/10/avance")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/cursos/10/avance").with(tokenDe(correoUnico(), "docente")))
                .andExpect(status().isForbidden());
    }
}
