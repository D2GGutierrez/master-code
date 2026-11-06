package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mastercode.userapi.admin.AdminNoDisponibleException;
import com.mastercode.userapi.admin.EvaluacionDto;
import com.mastercode.userapi.admin.EvaluacionNoExisteException;
import com.mastercode.userapi.admin.ProblemaCalificableDto;
import com.mastercode.userapi.sandbox.EjecucionResultado;
import com.mastercode.userapi.sandbox.Estado;

// HU64-T3 (CA-64.4): el intento se cierra cuando vence el límite de tiempo. El cierre es perezoso (ADR-8 D11): no hay
// tarea programada, cada vez que el estudiante entra o envía código se compara la hora actual con el inicio de SU intento
// más el límite de minutos (o con el fin de la ventana, lo que ocurra primero). El estado de la ventana lo da un mock de
// admin-api; el inicio del intento sí se guarda de verdad en app_intento_evaluacion.
class EvaluacionIntentoApiTest extends PruebaDeApi {

    private static final long CURSO = 10;
    private static final long EVALUACION = 5;
    private static final long PROBLEMA = 7;
    private static final String CODIGO = "a, b = map(int, input().split())\nprint(a + b)";
    private static final int LIMITE = 90;

    // Ventana amplia: el fin no recorta el límite de 90 minutos salvo en la prueba que lo pide.
    private static final OffsetDateTime AHORA = OffsetDateTime.now();
    private static final OffsetDateTime INICIO = AHORA.minusHours(1);
    private static final OffsetDateTime FIN = AHORA.plusHours(5);

    @Autowired
    private ObjectMapper json;

    private record Alumno(String email, long id) {
    }

    private Alumno alumnoInscrito() {
        String email = correoUnico();
        long id = crearEstudiante(email);
        inscribir(id, CURSO);
        return new Alumno(email, id);
    }

    private static EvaluacionDto evaluacion(String estado, OffsetDateTime fin) {
        return new EvaluacionDto(EVALUACION, CURSO, INICIO, fin, LIMITE, estado, List.of(PROBLEMA, 8L));
    }

    // doReturn y no when: una prueba la llama después de dejar el mock lanzando una excepción, y when(...) la dispararía.
    private void evaluacionActiva() {
        doReturn(evaluacion("activa", FIN)).when(admin).evaluacion(EVALUACION);
    }

    // Un intento sembrado: el estudiante entró hace «minutos» minutos.
    private void intentoIniciadoHace(Alumno alumno, long minutos) {
        jdbc.sql("""
                INSERT INTO app_schema.app_intento_evaluacion (estudiante_id, evaluacion_id, fecha_inicio)
                VALUES (?, ?, ?)
                """)
                .params(alumno.id(), EVALUACION, OffsetDateTime.now().minusMinutes(minutos))
                .update();
    }

    private int intentosDe(Alumno alumno) {
        return jdbc.sql("SELECT count(*) FROM app_schema.app_intento_evaluacion WHERE estudiante_id = ?")
                .param(alumno.id()).query(Integer.class).single();
    }

    private int enviosDe(Alumno alumno) {
        return jdbc.sql("SELECT count(*) FROM app_schema.app_envio WHERE estudiante_id = ?")
                .param(alumno.id()).query(Integer.class).single();
    }

    private ResultActions entrar(Alumno alumno) throws Exception {
        return mvc.perform(get("/api/evaluaciones/" + EVALUACION).with(tokenDe(alumno.email(), "estudiante")));
    }

    private ResultActions enviarEnEvaluacion(Alumno alumno, long problemaId) throws Exception {
        return mvc.perform(post("/api/problemas/" + problemaId + "/envios")
                .with(tokenDe(alumno.email(), "estudiante"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("codigo", CODIGO, "evaluacion_id", EVALUACION))));
    }

    private ResultActions enviarDePractica(Alumno alumno) throws Exception {
        return mvc.perform(post("/api/problemas/" + PROBLEMA + "/envios")
                .with(tokenDe(alumno.email(), "estudiante"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("codigo", CODIGO))));
    }

    private void problemaListoParaCalificar() {
        when(admin.problemaParaCalificar(PROBLEMA)).thenReturn(new ProblemaCalificableDto(PROBLEMA, CURSO, "python",
                "escritura_codigo", List.of(new ProblemaCalificableDto.Caso(1, "2 3", "5", true))));
        when(sandbox.ejecutar(any())).thenReturn(new EjecucionResultado(Estado.OK, "5", "", 5L));
    }

    private JsonNode cuerpo(ResultActions respuesta) throws Exception {
        return json.readTree(respuesta.andReturn().getResponse().getContentAsString());
    }

    // ---------- entrar ----------

    @Test
    void la_primera_entrada_guarda_el_inicio_del_intento_y_su_limite_es_inicio_mas_los_minutos() throws Exception {
        Alumno yo = alumnoInscrito();
        evaluacionActiva();

        JsonNode cuerpo = cuerpo(entrar(yo).andExpect(status().isOk()));

        assertThat(intentosDe(yo)).isEqualTo(1);
        OffsetDateTime inicio = OffsetDateTime.parse(cuerpo.get("fecha_inicio_intento").asText());
        OffsetDateTime limite = OffsetDateTime.parse(cuerpo.get("fecha_limite").asText());
        assertThat(Duration.between(inicio, limite)).isEqualTo(Duration.ofMinutes(LIMITE));
        assertThat(Duration.between(inicio, OffsetDateTime.now()).abs()).isLessThan(Duration.ofMinutes(1));
    }

    @Test
    void volver_a_entrar_no_reinicia_el_reloj() throws Exception {
        Alumno yo = alumnoInscrito();
        evaluacionActiva();
        intentoIniciadoHace(yo, 10);

        JsonNode primera = cuerpo(entrar(yo).andExpect(status().isOk()));
        JsonNode segunda = cuerpo(entrar(yo).andExpect(status().isOk()));

        assertThat(intentosDe(yo)).isEqualTo(1);
        OffsetDateTime inicio = OffsetDateTime.parse(primera.get("fecha_inicio_intento").asText());
        assertThat(Duration.between(inicio, OffsetDateTime.now()).toMinutes()).isBetween(9L, 11L);
        assertThat(segunda.get("fecha_inicio_intento").asText()).isEqualTo(primera.get("fecha_inicio_intento").asText());
        assertThat(segunda.get("fecha_limite").asText()).isEqualTo(primera.get("fecha_limite").asText());
    }

    // CA-64.4
    @Test
    void con_el_limite_vencido_dentro_de_la_ventana_el_intento_esta_cerrado_y_no_se_entregan_problemas()
            throws Exception {
        Alumno yo = alumnoInscrito();
        evaluacionActiva();
        intentoIniciadoHace(yo, 120);

        ResultActions respuesta = entrar(yo)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Se acabó el tiempo de tu evaluación."))
                .andExpect(jsonPath("$.estado").value("tiempo_agotado"))
                .andExpect(jsonPath("$.fecha_inicio_intento").exists())
                .andExpect(jsonPath("$.fecha_limite").exists())
                .andExpect(jsonPath("$.problemas").doesNotExist());

        assertThat(respuesta.andReturn().getResponse().getContentAsString()).doesNotContain("problemas");
        // Cerrar no borra nada ni reinicia el reloj: sigue el mismo intento.
        assertThat(intentosDe(yo)).isEqualTo(1);
    }

    @Test
    void con_tiempo_de_sobra_el_intento_sigue_abierto() throws Exception {
        Alumno yo = alumnoInscrito();
        evaluacionActiva();
        intentoIniciadoHace(yo, 10);

        entrar(yo).andExpect(status().isOk()).andExpect(jsonPath("$.problemas[0]").value(PROBLEMA));
    }

    @Test
    void justo_antes_y_justo_despues_del_limite_cambia_la_respuesta() throws Exception {
        Alumno antes = alumnoInscrito();
        Alumno despues = alumnoInscrito();
        evaluacionActiva();
        intentoIniciadoHace(antes, LIMITE - 1);
        intentoIniciadoHace(despues, LIMITE + 1);

        entrar(antes).andExpect(status().isOk());
        entrar(despues).andExpect(status().isForbidden()).andExpect(jsonPath("$.estado").value("tiempo_agotado"));
    }

    @Test
    void el_fin_de_la_ventana_recorta_el_limite_de_quien_entra_tarde() throws Exception {
        Alumno yo = alumnoInscrito();
        OffsetDateTime fin = OffsetDateTime.now().plusMinutes(20);
        when(admin.evaluacion(EVALUACION)).thenReturn(evaluacion("activa", fin));

        JsonNode cuerpo = cuerpo(entrar(yo).andExpect(status().isOk()));

        // Con 90 minutos de límite pero solo 20 de ventana, el intento se cierra cuando se cierra la ventana.
        assertThat(OffsetDateTime.parse(cuerpo.get("fecha_limite").asText()).toInstant()).isEqualTo(fin.toInstant());
    }

    @Test
    void el_limite_es_de_cada_estudiante_el_retraso_de_uno_no_cierra_al_otro() throws Exception {
        Alumno madrugador = alumnoInscrito();
        Alumno tarde = alumnoInscrito();
        evaluacionActiva();
        intentoIniciadoHace(madrugador, 100);

        entrar(madrugador).andExpect(status().isForbidden());
        entrar(tarde).andExpect(status().isOk());
        assertThat(intentosDe(tarde)).isEqualTo(1);
    }

    @Test
    void fuera_de_la_ventana_o_sin_inscripcion_no_se_empieza_ningun_reloj() throws Exception {
        Alumno yo = alumnoInscrito();
        String ajeno = correoUnico();
        long ajenoId = crearEstudiante(ajeno);

        when(admin.evaluacion(EVALUACION)).thenReturn(evaluacion("programada", FIN));
        entrar(yo).andExpect(status().isForbidden());
        when(admin.evaluacion(EVALUACION)).thenReturn(evaluacion("cerrada", FIN));
        entrar(yo).andExpect(status().isForbidden());
        when(admin.evaluacion(EVALUACION)).thenReturn(evaluacion("activa", FIN));
        mvc.perform(get("/api/evaluaciones/" + EVALUACION).with(tokenDe(ajeno, "estudiante")))
                .andExpect(status().isNotFound());
        when(admin.evaluacion(EVALUACION)).thenThrow(new EvaluacionNoExisteException());
        entrar(yo).andExpect(status().isNotFound());

        assertThat(intentosDe(yo)).isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM app_schema.app_intento_evaluacion WHERE estudiante_id = ?")
                .param(ajenoId).query(Integer.class).single()).isZero();
    }

    // ---------- enviar código dentro de la evaluación ----------

    @Test
    void enviar_con_tiempo_de_sobra_se_califica_y_se_guarda() throws Exception {
        Alumno yo = alumnoInscrito();
        evaluacionActiva();
        problemaListoParaCalificar();
        intentoIniciadoHace(yo, 10);

        enviarEnEvaluacion(yo, PROBLEMA)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultado").value("correcto"));

        assertThat(enviosDe(yo)).isEqualTo(1);
    }

    @Test
    void enviar_por_primera_vez_dentro_de_la_evaluacion_tambien_empieza_el_reloj() throws Exception {
        Alumno yo = alumnoInscrito();
        evaluacionActiva();
        problemaListoParaCalificar();

        enviarEnEvaluacion(yo, PROBLEMA).andExpect(status().isCreated());

        assertThat(intentosDe(yo)).isEqualTo(1);
    }

    // CA-64.4: pasado el límite no se ejecuta nada ni se guarda ningún envío.
    @Test
    void enviar_con_el_limite_vencido_da_403_sin_ejecutar_ni_guardar_nada() throws Exception {
        Alumno yo = alumnoInscrito();
        evaluacionActiva();
        problemaListoParaCalificar();
        intentoIniciadoHace(yo, 120);

        enviarEnEvaluacion(yo, PROBLEMA)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value("tiempo_agotado"))
                .andExpect(jsonPath("$.detail").value("Se acabó el tiempo de tu evaluación."));

        verifyNoInteractions(sandbox);
        verify(admin, never()).problemaParaCalificar(anyLong());
        assertThat(enviosDe(yo)).isZero();
    }

    @Test
    void enviar_a_una_evaluacion_programada_o_cerrada_da_403_y_no_guarda_nada() throws Exception {
        Alumno yo = alumnoInscrito();
        problemaListoParaCalificar();

        when(admin.evaluacion(EVALUACION)).thenReturn(evaluacion("programada", FIN));
        enviarEnEvaluacion(yo, PROBLEMA).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value("programada"));
        when(admin.evaluacion(EVALUACION)).thenReturn(evaluacion("cerrada", FIN));
        enviarEnEvaluacion(yo, PROBLEMA).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value("cerrada"));

        verifyNoInteractions(sandbox);
        assertThat(enviosDe(yo)).isZero();
        assertThat(intentosDe(yo)).isZero();
    }

    @Test
    void enviar_un_problema_que_no_es_de_la_evaluacion_da_404_y_no_ejecuta_nada() throws Exception {
        Alumno yo = alumnoInscrito();
        evaluacionActiva();
        problemaListoParaCalificar();

        enviarEnEvaluacion(yo, 99).andExpect(status().isNotFound());

        verifyNoInteractions(sandbox);
        assertThat(enviosDe(yo)).isZero();
    }

    @Test
    void enviar_a_una_evaluacion_inexistente_o_de_un_curso_ajeno_da_404() throws Exception {
        Alumno yo = alumnoInscrito();
        String ajeno = correoUnico();
        crearEstudiante(ajeno);
        problemaListoParaCalificar();

        when(admin.evaluacion(EVALUACION)).thenThrow(new EvaluacionNoExisteException());
        enviarEnEvaluacion(yo, PROBLEMA).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Evaluación no encontrada."));

        evaluacionActiva();
        mvc.perform(post("/api/problemas/" + PROBLEMA + "/envios")
                .with(tokenDe(ajeno, "estudiante"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("codigo", CODIGO, "evaluacion_id", EVALUACION))))
                .andExpect(status().isNotFound());

        verifyNoInteractions(sandbox);
    }

    @Test
    void si_admin_api_no_responde_no_se_envia_dentro_de_la_evaluacion() throws Exception {
        Alumno yo = alumnoInscrito();
        problemaListoParaCalificar();
        when(admin.evaluacion(EVALUACION)).thenThrow(new AdminNoDisponibleException("sin conexión", null));

        enviarEnEvaluacion(yo, PROBLEMA).andExpect(status().isServiceUnavailable());

        verifyNoInteractions(sandbox);
        assertThat(enviosDe(yo)).isZero();
    }

    // El envío de práctica (sin evaluacion_id) no se entera de ninguna evaluación: nada cambia para él.
    @Test
    void un_envio_sin_evaluacion_no_consulta_ni_depende_del_reloj_de_ninguna_evaluacion() throws Exception {
        Alumno yo = alumnoInscrito();
        evaluacionActiva();
        problemaListoParaCalificar();
        intentoIniciadoHace(yo, 120);

        enviarDePractica(yo).andExpect(status().isCreated());

        verify(admin, never()).evaluacion(anyLong());
        assertThat(enviosDe(yo)).isEqualTo(1);
    }
}
