package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.mastercode.userapi.admin.AdminNoDisponibleException;
import com.mastercode.userapi.admin.ProblemaDto;
import com.mastercode.userapi.admin.ProblemaNoVisibleException;
import com.mastercode.userapi.admin.RespuestaDelProblemaDto;

// H2 (con HU45-T1 y HU46-T2): POST /api/problemas/{id}/respuesta, la ruta donde el celular envía su respuesta. admin-api
// es un mock que dice qué problema es visible y cuál es la respuesta esperada.
class RespuestaMovilApiTest extends PruebaDeApi {

    private static final long CURSO = 10;
    private static final long PREDICCION = 7;
    private static final long OPCION = 8;
    private static final long COMPLETAR = 9;
    private static final String SALIDA_SECRETA = "SALIDA-SECRETA-4471";
    private static final String ALTERNATIVA_SECRETA = "ALTERNATIVA-SECRETA-9902";

    private static ProblemaDto problema(long id, String tipo, long curso) {
        return new ProblemaDto(id, "Ejercicio", "¿Qué imprime?", "python", "facil", "movil", tipo, "", List.of(), curso);
    }

    private static RespuestaDelProblemaDto prediccion() {
        return new RespuestaDelProblemaDto(PREDICCION, CURSO, "prediccion_salida", SALIDA_SECRETA, List.of());
    }

    private static RespuestaDelProblemaDto opcion() {
        return new RespuestaDelProblemaDto(OPCION, CURSO, "opcion_multiple", "", List.of(
                new RespuestaDelProblemaDto.Opcion(81, "Una", false),
                new RespuestaDelProblemaDto.Opcion(82, ALTERNATIVA_SECRETA, true),
                new RespuestaDelProblemaDto.Opcion(83, "Tres", false)));
    }

    private static RespuestaDelProblemaDto completar() {
        return new RespuestaDelProblemaDto(COMPLETAR, CURSO, "completar_espacio", "len", List.of());
    }

    private String alumnoInscrito() {
        String email = correoUnico();
        inscribir(crearEstudiante(email), CURSO);
        return email;
    }

    private void prepararPrediccion() {
        when(admin.problema(PREDICCION)).thenReturn(problema(PREDICCION, "prediccion_salida", CURSO));
        when(admin.respuestaDelProblema(PREDICCION)).thenReturn(prediccion());
    }

    private void prepararCompletar() {
        when(admin.problema(COMPLETAR)).thenReturn(problema(COMPLETAR, "completar_espacio", CURSO));
        when(admin.respuestaDelProblema(COMPLETAR)).thenReturn(completar());
    }

    private void prepararOpcion() {
        when(admin.problema(OPCION)).thenReturn(problema(OPCION, "opcion_multiple", CURSO));
        when(admin.respuestaDelProblema(OPCION)).thenReturn(opcion());
    }

    private ResultActions responder(String email, long problemaId, String cuerpo) throws Exception {
        return mvc.perform(post("/api/problemas/" + problemaId + "/respuesta")
                .with(tokenDeCliente(email, "mobile")).contentType(APPLICATION_JSON).content(cuerpo));
    }

    @Test
    void una_prediccion_acertada_es_correcta_y_no_trae_la_respuesta() throws Exception {
        String yo = alumnoInscrito();
        prepararPrediccion();

        responder(yo, PREDICCION, "{\"respuesta\":\"" + SALIDA_SECRETA + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("correcto"))
                .andExpect(jsonPath("$.respuesta_correcta").doesNotExist());
    }

    @Test
    void una_prediccion_con_espacios_y_mayusculas_de_mas_tambien_acierta() throws Exception {
        String yo = alumnoInscrito();
        prepararPrediccion();

        responder(yo, PREDICCION, "{\"respuesta\":\"   " + SALIDA_SECRETA.toLowerCase() + "  \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("correcto"));
    }

    @Test
    void una_prediccion_fallada_es_incorrecta_y_trae_la_respuesta_correcta() throws Exception {
        String yo = alumnoInscrito();
        prepararPrediccion();

        responder(yo, PREDICCION, "{\"respuesta\":\"otra cosa\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("incorrecto"))
                .andExpect(jsonPath("$.respuesta_correcta").value(SALIDA_SECRETA));
    }

    // HU50-T2: un acierto suma puntos al cinturón del lenguaje del ejercicio, solo la primera vez.
    private int puntosDelCinturon(String email, String lenguaje) {
        return jdbc.sql("""
                SELECT coalesce(sum(c.puntos), 0) FROM app_schema.app_cinturon c
                JOIN app_schema.app_estudiante e ON e.id = c.estudiante_id
                WHERE e.email = ? AND c.lenguaje = ?
                """)
                .params(email, lenguaje).query(Integer.class).single();
    }

    @Test
    void un_acierto_suma_puntos_al_cinturon_pero_repetirlo_no_y_un_fallo_tampoco() throws Exception {
        String yo = alumnoInscrito();
        prepararPrediccion();
        prepararOpcion();

        responder(yo, PREDICCION, "{\"respuesta\":\"otra cosa\"}").andExpect(status().isOk());
        assertThat(puntosDelCinturon(yo, "python")).isZero();

        responder(yo, PREDICCION, "{\"respuesta\":\"" + SALIDA_SECRETA + "\"}").andExpect(status().isOk());
        assertThat(puntosDelCinturon(yo, "python")).isEqualTo(10);

        responder(yo, PREDICCION, "{\"respuesta\":\"" + SALIDA_SECRETA + "\"}").andExpect(status().isOk());
        assertThat(puntosDelCinturon(yo, "python")).isEqualTo(10);

        responder(yo, OPCION, "{\"opcion_id\":82}").andExpect(status().isOk());
        assertThat(puntosDelCinturon(yo, "python")).isEqualTo(20);
    }

    @Test
    void una_respuesta_invalida_no_suma_puntos() throws Exception {
        String yo = alumnoInscrito();
        prepararPrediccion();

        responder(yo, PREDICCION, "{}").andExpect(status().isBadRequest());

        assertThat(puntosDelCinturon(yo, "python")).isZero();
    }

    // HU47-T2: el espacio por completar se corrige con la misma normalización que la predicción de salida.
    @Test
    void un_espacio_completado_con_espacios_y_mayusculas_de_mas_acierta() throws Exception {
        String yo = alumnoInscrito();
        prepararCompletar();

        responder(yo, COMPLETAR, "{\"respuesta\":\"  LEN \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("correcto"))
                .andExpect(jsonPath("$.respuesta_correcta").doesNotExist());
    }

    @Test
    void un_espacio_mal_completado_es_incorrecto_y_trae_la_respuesta_correcta() throws Exception {
        String yo = alumnoInscrito();
        prepararCompletar();

        responder(yo, COMPLETAR, "{\"respuesta\":\"length\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("incorrecto"))
                .andExpect(jsonPath("$.respuesta_correcta").value("len"));
    }

    @Test
    void un_espacio_en_blanco_es_un_400() throws Exception {
        String yo = alumnoInscrito();
        prepararCompletar();

        responder(yo, COMPLETAR, "{\"respuesta\":\"   \"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Escribe tu respuesta antes de enviarla."));
    }

    @Test
    void una_alternativa_correcta_es_correcta_y_no_trae_la_respuesta() throws Exception {
        String yo = alumnoInscrito();
        prepararOpcion();

        responder(yo, OPCION, "{\"opcion_id\":82}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("correcto"))
                .andExpect(jsonPath("$.respuesta_correcta").doesNotExist());
    }

    @Test
    void una_alternativa_equivocada_es_incorrecta_y_trae_el_texto_de_la_correcta() throws Exception {
        String yo = alumnoInscrito();
        prepararOpcion();

        responder(yo, OPCION, "{\"opcion_id\":81}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("incorrecto"))
                .andExpect(jsonPath("$.respuesta_correcta").value(ALTERNATIVA_SECRETA));
    }

    @Test
    void una_alternativa_que_no_es_de_ese_ejercicio_es_un_400() throws Exception {
        String yo = alumnoInscrito();
        prepararOpcion();

        responder(yo, OPCION, "{\"opcion_id\":999}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Esa alternativa no es de este ejercicio."));
    }

    @Test
    void sin_respuesta_o_sin_alternativa_es_un_400_con_mensaje() throws Exception {
        String yo = alumnoInscrito();
        prepararPrediccion();
        prepararOpcion();

        responder(yo, PREDICCION, "{}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Escribe tu respuesta antes de enviarla."));
        responder(yo, PREDICCION, "{\"respuesta\":\"  \"}").andExpect(status().isBadRequest());
        responder(yo, OPCION, "{}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Elige una alternativa antes de enviarla."));
    }

    @Test
    void un_ejercicio_de_codigo_no_se_responde_aqui() throws Exception {
        String yo = alumnoInscrito();
        when(admin.problema(5)).thenReturn(problema(5, "escritura_codigo", CURSO));
        when(admin.respuestaDelProblema(5)).thenReturn(
                new RespuestaDelProblemaDto(5, CURSO, "escritura_codigo", "", List.of()));

        responder(yo, 5, "{\"respuesta\":\"print(1)\"}").andExpect(status().isBadRequest());
    }

    @Test
    void quien_no_esta_inscrito_en_el_curso_recibe_404_y_no_se_pide_la_respuesta_esperada() throws Exception {
        String ajeno = correoUnico();
        crearEstudiante(ajeno);
        prepararPrediccion();

        responder(ajeno, PREDICCION, "{\"respuesta\":\"x\"}")
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString(SALIDA_SECRETA))));

        verify(admin, never()).respuestaDelProblema(PREDICCION);
    }

    @Test
    void un_problema_no_visible_da_404() throws Exception {
        String yo = alumnoInscrito();
        when(admin.problema(PREDICCION)).thenThrow(new ProblemaNoVisibleException());

        responder(yo, PREDICCION, "{\"respuesta\":\"x\"}").andExpect(status().isNotFound());
    }

    @Test
    void un_problema_que_deja_de_ser_visible_entre_las_dos_consultas_tambien_da_404() throws Exception {
        String yo = alumnoInscrito();
        when(admin.problema(PREDICCION)).thenReturn(problema(PREDICCION, "prediccion_salida", CURSO));
        when(admin.respuestaDelProblema(PREDICCION)).thenThrow(new ProblemaNoVisibleException());

        responder(yo, PREDICCION, "{\"respuesta\":\"x\"}").andExpect(status().isNotFound());
    }

    @Test
    void con_admin_api_caido_responde_503_sin_filtrar_nada() throws Exception {
        String yo = alumnoInscrito();
        when(admin.problema(PREDICCION)).thenReturn(problema(PREDICCION, "prediccion_salida", CURSO));
        when(admin.respuestaDelProblema(PREDICCION)).thenThrow(new AdminNoDisponibleException("caído", null));

        responder(yo, PREDICCION, "{\"respuesta\":\"x\"}")
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(not(containsString(SALIDA_SECRETA))));
    }

    @Test
    void sin_token_de_sesion_responde_401() throws Exception {
        mvc.perform(post("/api/problemas/" + PREDICCION + "/respuesta").contentType(APPLICATION_JSON)
                .content("{\"respuesta\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }
}
