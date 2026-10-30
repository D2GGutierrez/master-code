package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mastercode.userapi.admin.AdminNoDisponibleException;
import com.mastercode.userapi.admin.ProblemaCalificableDto;
import com.mastercode.userapi.admin.ProblemaNoVisibleException;
import com.mastercode.userapi.admin.ProblemaResumenDto;
import com.mastercode.userapi.sandbox.EjecucionResultado;
import com.mastercode.userapi.sandbox.EjecucionSolicitud;
import com.mastercode.userapi.sandbox.Estado;
import com.mastercode.userapi.sandbox.LenguajeNoSoportadoException;
import com.mastercode.userapi.sandbox.SandboxNoDisponibleException;

// HU36-T2 y HU38-T2: el envío oficial. Se ejecuta contra todos los casos, se califica, se guarda con su número de
// intento y se devuelve el detalle. admin-api y el sandbox son mocks: el sandbox responde según la entrada de cada
// caso, así cada prueba decide qué casos pasan.
class EnviosApiTest extends PruebaDeApi {

    private static final long CURSO = 10;
    private static final long PROBLEMA = 7;
    private static final String CODIGO = "a, b = map(int, input().split())\nprint(a + b)";

    // Centinelas: textos que solo pueden aparecer en la respuesta si un caso oculto viajó.
    private static final String ENTRADA_OCULTA = "ENTRADA-OCULTA-4821";
    private static final String SALIDA_OCULTA = "SALIDA-OCULTA-9137";

    private static final ProblemaCalificableDto.Caso PUBLICO = new ProblemaCalificableDto.Caso("2 3", "5", true);
    private static final ProblemaCalificableDto.Caso OCULTO = new ProblemaCalificableDto.Caso(ENTRADA_OCULTA,
            SALIDA_OCULTA, false);
    private static final ProblemaCalificableDto.Caso OTRO_OCULTO = new ProblemaCalificableDto.Caso("10 20", "30", false);
    private static final ProblemaCalificableDto.Caso TERCER_OCULTO = new ProblemaCalificableDto.Caso("1 1", "2", false);

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

    private static ProblemaCalificableDto problema(long id, long curso, String tipo,
            ProblemaCalificableDto.Caso... casos) {
        return new ProblemaCalificableDto(id, curso, "python", tipo, List.of(casos));
    }

    private void prepararProblema(ProblemaCalificableDto.Caso... casos) {
        when(admin.problemaParaCalificar(PROBLEMA)).thenReturn(problema(PROBLEMA, CURSO, "escritura_codigo", casos));
    }

    // El programa del estudiante «imprime» lo que diga el mapa para cada entrada; sin entrada en el mapa, nada.
    private void sandboxImprime(Map<String, String> salidaPorEntrada) {
        // doAnswer y no when: un test lo llama dos veces, y when(...) ejecutaría la respuesta anterior con un argumento nulo.
        doAnswer(llamada -> {
            EjecucionSolicitud solicitud = llamada.getArgument(0);
            return new EjecucionResultado(Estado.OK, salidaPorEntrada.getOrDefault(solicitud.entrada(), ""), "", 5L);
        }).when(sandbox).ejecutar(any());
    }

    private ResultActions enviar(String email, long problemaId, String codigo) throws Exception {
        return mvc.perform(post("/api/problemas/" + problemaId + "/envios")
                .with(tokenDe(email, "estudiante"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("codigo", codigo))));
    }

    private ResultActions enviar(Alumno alumno) throws Exception {
        return enviar(alumno.email(), PROBLEMA, CODIGO);
    }

    private List<Map<String, Object>> guardados(long estudianteId, long problemaId) {
        return jdbc.sql("""
                SELECT resultado, puntaje, numero_intento, plataforma, codigo
                FROM app_schema.app_envio
                WHERE estudiante_id = ? AND problema_id = ?
                ORDER BY numero_intento
                """)
                .params(estudianteId, problemaId)
                .query()
                .listOfRows();
    }

    // Por estudiante: la base de desarrollo puede tener envíos de otras corridas, y una prueba solo mira los suyos.
    private int enviosDe(long estudianteId) {
        return jdbc.sql("SELECT count(*) FROM app_schema.app_envio WHERE estudiante_id = ?")
                .param(estudianteId)
                .query(Integer.class)
                .single();
    }

    private static String texto(ResultActions respuesta) throws Exception {
        return respuesta.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    // ---------- calificación (HU38) ----------

    @Test
    void con_todos_los_casos_correctos_el_resultado_es_correcto_con_100_y_queda_guardado() throws Exception {
        Alumno yo = alumnoInscrito();
        prepararProblema(PUBLICO, OTRO_OCULTO);
        sandboxImprime(Map.of("2 3", "5", "10 20", "30"));

        enviar(yo)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero_intento").value(1))
                .andExpect(jsonPath("$.resultado").value("correcto"))
                .andExpect(jsonPath("$.puntaje").value(100.0))
                .andExpect(jsonPath("$.casos_superados").value(2))
                .andExpect(jsonPath("$.casos_total").value(2))
                .andExpect(jsonPath("$.aviso").value(nullValue()));

        List<Map<String, Object>> filas = guardados(yo.id(), PROBLEMA);
        assertThat(filas).hasSize(1);
        assertThat(filas.get(0).get("resultado")).isEqualTo("correcto");
        assertThat((BigDecimal) filas.get(0).get("puntaje")).isEqualByComparingTo("100");
        assertThat(filas.get(0).get("numero_intento")).isEqualTo(1);
        assertThat(filas.get(0).get("plataforma")).isEqualTo("web");
        // El código enviado queda guardado tal cual (HU42 lo mostrará).
        assertThat(filas.get(0).get("codigo")).isEqualTo(CODIGO);
    }

    @Test
    void con_algunos_casos_el_resultado_es_parcial_y_el_puntaje_proporcional() throws Exception {
        Alumno yo = alumnoInscrito();
        prepararProblema(PUBLICO, OCULTO, OTRO_OCULTO, TERCER_OCULTO);
        // Pasan 3 de 4: el caso «ENTRADA-OCULTA» imprime algo distinto de lo esperado.
        sandboxImprime(Map.of("2 3", "5", ENTRADA_OCULTA, "otra cosa", "10 20", "30", "1 1", "2"));

        enviar(yo)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultado").value("parcial"))
                .andExpect(jsonPath("$.puntaje").value(75.0))
                .andExpect(jsonPath("$.casos_superados").value(3))
                .andExpect(jsonPath("$.casos_total").value(4));

        List<Map<String, Object>> filas = guardados(yo.id(), PROBLEMA);
        assertThat(filas.get(0).get("resultado")).isEqualTo("parcial");
        assertThat((BigDecimal) filas.get(0).get("puntaje")).isEqualByComparingTo("75");
    }

    @Test
    void sin_ningun_caso_superado_el_resultado_es_incorrecto_con_0() throws Exception {
        Alumno yo = alumnoInscrito();
        prepararProblema(PUBLICO, OTRO_OCULTO);
        sandboxImprime(Map.of("2 3", "6", "10 20", "31"));

        enviar(yo)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultado").value("incorrecto"))
                .andExpect(jsonPath("$.puntaje").value(0.0))
                .andExpect(jsonPath("$.casos_superados").value(0));

        assertThat(guardados(yo.id(), PROBLEMA).get(0).get("resultado")).isEqualTo("incorrecto");
    }

    @Test
    void los_espacios_y_saltos_del_final_no_hacen_fallar_un_caso_pero_una_diferencia_real_si() throws Exception {
        Alumno yo = alumnoInscrito();
        prepararProblema(PUBLICO, OTRO_OCULTO);
        sandboxImprime(Map.of("2 3", "5  \n\n", "10 20", "300"));

        enviar(yo)
                .andExpect(jsonPath("$.casos[0].paso").value(true))
                .andExpect(jsonPath("$.casos[1].paso").value(false))
                .andExpect(jsonPath("$.resultado").value("parcial"));
    }

    // ---------- número de intento (HU36, CA-36.3) ----------

    @Test
    void el_numero_de_intento_sube_con_cada_envio_del_mismo_estudiante_en_el_mismo_problema() throws Exception {
        Alumno yo = alumnoInscrito();
        prepararProblema(PUBLICO);
        sandboxImprime(Map.of("2 3", "5"));

        enviar(yo).andExpect(jsonPath("$.numero_intento").value(1));
        enviar(yo).andExpect(jsonPath("$.numero_intento").value(2));
        enviar(yo).andExpect(jsonPath("$.numero_intento").value(3));

        assertThat(guardados(yo.id(), PROBLEMA)).extracting(fila -> fila.get("numero_intento")).containsExactly(1, 2, 3);
    }

    @Test
    void cada_problema_y_cada_estudiante_llevan_su_propia_cuenta() throws Exception {
        Alumno yo = alumnoInscrito();
        Alumno otro = alumnoInscrito();
        prepararProblema(PUBLICO);
        when(admin.problemaParaCalificar(8)).thenReturn(problema(8, CURSO, "escritura_codigo", PUBLICO));
        sandboxImprime(Map.of("2 3", "5"));

        enviar(yo).andExpect(jsonPath("$.numero_intento").value(1));
        enviar(yo).andExpect(jsonPath("$.numero_intento").value(2));
        // Otro problema: vuelve a 1.
        enviar(yo.email(), 8, CODIGO).andExpect(jsonPath("$.numero_intento").value(1));
        // Otro estudiante en el mismo problema: también empieza en 1.
        enviar(otro).andExpect(jsonPath("$.numero_intento").value(1));
    }

    // ---------- casos ocultos (HU39) ----------

    @Test
    void de_un_caso_oculto_solo_viaja_si_paso_nunca_su_entrada_ni_su_salida() throws Exception {
        Alumno yo = alumnoInscrito();
        prepararProblema(PUBLICO, OCULTO);
        // Un programa que imprime lo que lee: su salida contiene la entrada oculta, y aun así no debe salir.
        when(sandbox.ejecutar(any())).thenAnswer(llamada -> {
            EjecucionSolicitud solicitud = llamada.getArgument(0);
            String salida = "2 3".equals(solicitud.entrada()) ? "5" : "ECO " + solicitud.entrada();
            return new EjecucionResultado(Estado.OK, salida, "", 5L);
        });

        ResultActions respuesta = enviar(yo)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.casos", hasSize(2)))
                .andExpect(jsonPath("$.casos[0].publico").value(true))
                .andExpect(jsonPath("$.casos[0].entrada").value("2 3"))
                .andExpect(jsonPath("$.casos[0].salida_esperada").value("5"))
                .andExpect(jsonPath("$.casos[0].salida_obtenida").value("5"))
                .andExpect(jsonPath("$.casos[1].publico").value(false))
                .andExpect(jsonPath("$.casos[1].paso").value(false))
                .andExpect(jsonPath("$.casos[1].entrada").doesNotExist())
                .andExpect(jsonPath("$.casos[1].salida_esperada").doesNotExist())
                .andExpect(jsonPath("$.casos[1].salida_obtenida").doesNotExist())
                .andExpect(jsonPath("$.casos[1].error").doesNotExist())
                .andExpect(jsonPath("$.casos[1].estado").doesNotExist());

        String cuerpo = texto(respuesta);
        assertThat(cuerpo).doesNotContain(ENTRADA_OCULTA).doesNotContain(SALIDA_OCULTA);
    }

    @Test
    void un_error_del_programa_en_un_caso_oculto_no_filtra_su_entrada_por_el_aviso() throws Exception {
        Alumno yo = alumnoInscrito();
        prepararProblema(PUBLICO, OCULTO);
        when(sandbox.ejecutar(any())).thenAnswer(llamada -> {
            EjecucionSolicitud solicitud = llamada.getArgument(0);
            if ("2 3".equals(solicitud.entrada())) {
                return new EjecucionResultado(Estado.OK, "5", "", 5L);
            }
            // El traceback imprime el dato con el que falló el programa.
            return new EjecucionResultado(Estado.ERROR_EJECUCION, "", "ValueError: " + solicitud.entrada(), 5L);
        });

        ResultActions respuesta = enviar(yo)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.aviso").value("Tu programa falló al ejecutarse en uno o más casos."));

        assertThat(texto(respuesta)).doesNotContain(ENTRADA_OCULTA);
    }

    // ---------- errores del código del estudiante (HU40, en lo mínimo) ----------

    @Test
    void un_error_de_compilacion_da_un_aviso_con_el_mensaje_del_motor() throws Exception {
        Alumno yo = alumnoInscrito();
        prepararProblema(PUBLICO, OTRO_OCULTO);
        when(sandbox.ejecutar(any()))
                .thenReturn(new EjecucionResultado(Estado.ERROR_COMPILACION, "", "SyntaxError en la línea 2", null));

        enviar(yo)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultado").value("incorrecto"))
                .andExpect(jsonPath("$.aviso").value("Tu código no compiló. SyntaxError en la línea 2"))
                .andExpect(jsonPath("$.casos[0].estado").value("error_compilacion"))
                .andExpect(jsonPath("$.casos[0].error").value("SyntaxError en la línea 2"));
    }

    @Test
    void un_tiempo_agotado_da_un_aviso_y_cuenta_el_caso_como_fallado() throws Exception {
        Alumno yo = alumnoInscrito();
        prepararProblema(PUBLICO);
        when(sandbox.ejecutar(any()))
                .thenReturn(new EjecucionResultado(Estado.TIEMPO_AGOTADO, "", "Tiempo límite excedido.", null));

        enviar(yo)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultado").value("incorrecto"))
                .andExpect(jsonPath("$.aviso").value("Tu programa tardó demasiado en uno o más casos y se detuvo."))
                .andExpect(jsonPath("$.casos[0].paso").value(false))
                .andExpect(jsonPath("$.casos[0].estado").value("tiempo_agotado"));
    }

    @Test
    void una_salida_enorme_se_recorta_en_la_respuesta_pero_el_caso_se_califica_completo() throws Exception {
        Alumno yo = alumnoInscrito();
        prepararProblema(PUBLICO);
        String enorme = "x".repeat(50_000);
        when(sandbox.ejecutar(any())).thenReturn(new EjecucionResultado(Estado.OK, enorme, "", 5L));

        ResultActions respuesta = enviar(yo).andExpect(status().isCreated()).andExpect(jsonPath("$.casos[0].paso").value(false));

        assertThat(texto(respuesta).length()).isLessThan(5_000);
    }

    // ---------- fallos del servicio: el intento no se cuenta ----------

    @Test
    void si_el_motor_falla_responde_503_y_no_guarda_ningun_envio() throws Exception {
        Alumno yo = alumnoInscrito();
        prepararProblema(PUBLICO, OTRO_OCULTO);
        // El primer caso corre bien y el segundo falla el motor: no se guarda un envío a medias.
        when(sandbox.ejecutar(any()))
                .thenReturn(new EjecucionResultado(Estado.OK, "5", "", 5L))
                .thenReturn(new EjecucionResultado(Estado.ERROR_SERVICIO, "", "motor caído", null));

        enviar(yo).andExpect(status().isServiceUnavailable());

        assertThat(enviosDe(yo.id())).isZero();
    }

    @Test
    void si_ai_service_no_responde_es_503_y_no_guarda_nada() throws Exception {
        Alumno yo = alumnoInscrito();
        prepararProblema(PUBLICO);
        when(sandbox.ejecutar(any())).thenThrow(new SandboxNoDisponibleException("sin conexión", null));

        enviar(yo)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value(
                        "El servicio de ejecución no está disponible en este momento. Tu envío no se contó: intenta de nuevo en un momento."));

        assertThat(enviosDe(yo.id())).isZero();
    }

    @Test
    void un_lenguaje_que_el_motor_no_soporta_es_400_y_no_guarda_nada() throws Exception {
        Alumno yo = alumnoInscrito();
        prepararProblema(PUBLICO);
        when(sandbox.ejecutar(any())).thenThrow(new LenguajeNoSoportadoException("python"));

        enviar(yo).andExpect(status().isBadRequest());

        assertThat(enviosDe(yo.id())).isZero();
    }

    @Test
    void con_admin_api_caido_es_503_y_no_ejecuta_nada() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.problemaParaCalificar(PROBLEMA)).thenThrow(new AdminNoDisponibleException("caído", null));

        enviar(yo).andExpect(status().isServiceUnavailable());

        verifyNoInteractions(sandbox);
        assertThat(enviosDe(yo.id())).isZero();
    }

    // ---------- quién puede enviar qué ----------

    @Test
    void un_problema_que_no_es_visible_da_404_y_no_ejecuta_nada() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.problemaParaCalificar(PROBLEMA)).thenThrow(new ProblemaNoVisibleException());

        enviar(yo)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Problema no encontrado."));

        verifyNoInteractions(sandbox);
        assertThat(enviosDe(yo.id())).isZero();
    }

    @Test
    void sin_inscripcion_en_su_curso_da_el_mismo_404_y_no_ejecuta_nada() throws Exception {
        String email = correoUnico();
        long id = crearEstudiante(email);
        prepararProblema(PUBLICO);

        enviar(email, PROBLEMA, CODIGO)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Problema no encontrado."));

        verifyNoInteractions(sandbox);
        assertThat(enviosDe(id)).isZero();
    }

    @Test
    void estar_inscrito_en_otro_curso_no_da_acceso() throws Exception {
        String email = correoUnico();
        long id = crearEstudiante(email);
        inscribir(id, 11);
        prepararProblema(PUBLICO);

        enviar(email, PROBLEMA, CODIGO).andExpect(status().isNotFound());

        verifyNoInteractions(sandbox);
    }

    @Test
    void el_estudiante_sale_del_token_aunque_el_cuerpo_traiga_otro_id() throws Exception {
        Alumno yo = alumnoInscrito();
        Alumno otro = alumnoInscrito();
        prepararProblema(PUBLICO);
        sandboxImprime(Map.of("2 3", "5"));

        mvc.perform(post("/api/problemas/" + PROBLEMA + "/envios")
                .with(tokenDe(yo.email(), "estudiante"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"print(5)\",\"estudiante_id\":" + otro.id() + ",\"plataforma\":\"movil\"}"))
                .andExpect(status().isCreated());

        assertThat(guardados(yo.id(), PROBLEMA)).hasSize(1);
        assertThat(guardados(otro.id(), PROBLEMA)).isEmpty();
        // La plataforma tampoco sale del cuerpo.
        assertThat(guardados(yo.id(), PROBLEMA).get(0).get("plataforma")).isEqualTo("web");
    }

    @Test
    void un_ejercicio_que_no_es_de_escribir_codigo_no_se_envia_al_sandbox() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.problemaParaCalificar(PROBLEMA)).thenReturn(problema(PROBLEMA, CURSO, "opcion_multiple"));

        enviar(yo).andExpect(status().isBadRequest());

        verifyNoInteractions(sandbox);
        assertThat(enviosDe(yo.id())).isZero();
    }

    @Test
    void un_problema_sin_casos_de_prueba_es_409() throws Exception {
        Alumno yo = alumnoInscrito();
        prepararProblema();

        enviar(yo).andExpect(status().isConflict());

        verifyNoInteractions(sandbox);
        assertThat(enviosDe(yo.id())).isZero();
    }

    @Test
    void sin_codigo_o_con_un_codigo_demasiado_largo_es_400_sin_consultar_a_admin_api() throws Exception {
        Alumno yo = alumnoInscrito();

        enviar(yo.email(), PROBLEMA, "   ").andExpect(status().isBadRequest());
        enviar(yo.email(), PROBLEMA, "x".repeat(50_001)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/problemas/" + PROBLEMA + "/envios")
                .with(tokenDe(yo.email(), "estudiante"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(admin);
        verifyNoInteractions(sandbox);
    }

    @Test
    void un_docente_o_una_peticion_sin_token_no_pueden_enviar() throws Exception {
        mvc.perform(post("/api/problemas/" + PROBLEMA + "/envios")
                .with(tokenDe(correoUnico(), "docente"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"print(5)\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/problemas/" + PROBLEMA + "/envios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"print(5)\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(sandbox);
    }

    // ---------- lo que recibe el sandbox ----------

    @Test
    void el_sandbox_recibe_el_codigo_el_lenguaje_del_problema_y_la_entrada_de_cada_caso() throws Exception {
        Alumno yo = alumnoInscrito();
        prepararProblema(PUBLICO, OTRO_OCULTO);
        sandboxImprime(Map.of("2 3", "5", "10 20", "30"));

        // El lenguaje es el del problema (python); el estudiante no lo elige.
        enviar(yo).andExpect(status().isCreated());

        ArgumentCaptor<EjecucionSolicitud> enviadas = ArgumentCaptor.forClass(EjecucionSolicitud.class);
        verify(sandbox, times(2)).ejecutar(enviadas.capture());
        assertThat(enviadas.getAllValues()).containsExactly(
                new EjecucionSolicitud(CODIGO, "python", "2 3"),
                new EjecucionSolicitud(CODIGO, "python", "10 20"));
    }

    @Test
    void un_caso_sin_entrada_se_ejecuta_con_la_entrada_vacia() throws Exception {
        Alumno yo = alumnoInscrito();
        prepararProblema(new ProblemaCalificableDto.Caso(null, "hola", true));
        sandboxImprime(Map.of("", "hola"));

        enviar(yo).andExpect(jsonPath("$.resultado").value("correcto"));
    }

    // ---------- plataforma (HU16, CA-16.2) ----------

    @Test
    void el_envio_se_guarda_con_la_plataforma_del_cliente_del_token() throws Exception {
        Alumno web = alumnoInscrito();
        Alumno movil = alumnoInscrito();
        prepararProblema(PUBLICO);
        sandboxImprime(Map.of("2 3", "5"));

        mvc.perform(post("/api/problemas/" + PROBLEMA + "/envios")
                .with(tokenDeCliente(web.email(), "user-web"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"print(5)\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/problemas/" + PROBLEMA + "/envios")
                .with(tokenDeCliente(movil.email(), "mobile"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"print(5)\"}"))
                .andExpect(status().isCreated());

        assertThat(guardados(web.id(), PROBLEMA).get(0).get("plataforma")).isEqualTo("web");
        assertThat(guardados(movil.id(), PROBLEMA).get(0).get("plataforma")).isEqualTo("movil");
    }

    // ---------- el estado del problema (HU30, D6) ----------

    @Test
    void el_listado_refleja_el_envio_parcial_como_intentado_y_el_correcto_como_resuelto() throws Exception {
        Alumno yo = alumnoInscrito();
        prepararProblema(PUBLICO, OTRO_OCULTO);
        when(admin.problemasDelCurso(CURSO, null)).thenReturn(List.of(new ProblemaResumenDto(PROBLEMA, "Suma",
                "python", "facil", "web", "escritura_codigo", OffsetDateTime.parse("2026-10-28T15:00:00Z"))));
        String listado = "/api/cursos/" + CURSO + "/problemas";

        mvc.perform(get(listado).with(tokenDe(yo.email(), "estudiante")))
                .andExpect(jsonPath("$[0].estado").value("sin_abrir"));

        // Pasa un caso de dos: parcial, o sea intentado.
        sandboxImprime(Map.of("2 3", "5", "10 20", "otra cosa"));
        enviar(yo).andExpect(jsonPath("$.resultado").value("parcial"));
        mvc.perform(get(listado).with(tokenDe(yo.email(), "estudiante")))
                .andExpect(jsonPath("$[0].estado").value("intentado"));

        // Ahora pasa los dos: correcto, o sea resuelto.
        sandboxImprime(Map.of("2 3", "5", "10 20", "30"));
        enviar(yo).andExpect(jsonPath("$.resultado").value("correcto"));
        mvc.perform(get(listado).with(tokenDe(yo.email(), "estudiante")))
                .andExpect(jsonPath("$[0].estado").value("resuelto"));
        // Filtrando por estado también aparece.
        mvc.perform(get(listado + "?estado=resuelto").with(tokenDe(yo.email(), "estudiante")))
                .andExpect(jsonPath("$", hasSize(1)));
    }
}
