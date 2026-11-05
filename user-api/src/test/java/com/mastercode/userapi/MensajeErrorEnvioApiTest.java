package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mastercode.userapi.admin.ProblemaCalificableDto;
import com.mastercode.userapi.admin.ProblemaDto;
import com.mastercode.userapi.sandbox.EjecucionResultado;
import com.mastercode.userapi.sandbox.Estado;

// HU40-T4 (CA-40.5): el aviso de error de un envío queda guardado con él y se ve en el historial. Se envía por la API
// de verdad (admin-api y el sandbox son mocks) y se mira tanto la fila de app_envio como el historial.
class MensajeErrorEnvioApiTest extends PruebaDeApi {

    private static final long CURSO = 10;
    private static final long PROBLEMA = 7;
    private static final String ENTRADA_OCULTA = "ENTRADA-OCULTA-9931";

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

    private void prepararProblema() {
        when(admin.problemaParaCalificar(PROBLEMA)).thenReturn(new ProblemaCalificableDto(PROBLEMA, CURSO, "python",
                "escritura_codigo", List.of(new ProblemaCalificableDto.Caso(1, "2 3", "5", true),
                        new ProblemaCalificableDto.Caso(2, ENTRADA_OCULTA, "x", false))));
        when(admin.problema(PROBLEMA)).thenReturn(new ProblemaDto(PROBLEMA, "Suma", "Suma dos.", "python", "facil",
                "web", "escritura_codigo", "", List.of(new ProblemaDto.Caso("2 3", "5")), CURSO));
    }

    private ResultActions enviar(Alumno alumno) throws Exception {
        return mvc.perform(post("/api/problemas/" + PROBLEMA + "/envios").with(tokenDe(alumno.email(), "estudiante"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("codigo", "print(1)"))));
    }

    private ResultActions historial(Alumno alumno) throws Exception {
        return mvc.perform(get("/api/problemas/" + PROBLEMA + "/envios").with(tokenDe(alumno.email(), "estudiante")));
    }

    private String guardado(Alumno alumno) {
        return jdbc.sql("SELECT mensaje_error FROM app_schema.app_envio WHERE estudiante_id = ?")
                .param(alumno.id()).query(String.class).optional().orElse(null);
    }

    @Test
    void un_error_de_compilacion_guarda_el_mismo_aviso_que_vio_el_estudiante_y_el_historial_lo_devuelve()
            throws Exception {
        Alumno yo = alumnoInscrito();
        prepararProblema();
        String motor = "Main.java:3: error: ';' expected\n        int x = 1\n                 ^\n1 error";
        when(sandbox.ejecutar(any())).thenReturn(new EjecucionResultado(Estado.ERROR_COMPILACION, "", motor, null));
        String aviso = "Tu código no compiló. Revisa la línea 3. " + motor;

        enviar(yo).andExpect(status().isCreated()).andExpect(jsonPath("$.aviso").value(aviso));

        assertThat(guardado(yo)).isEqualTo(aviso);
        historial(yo).andExpect(status().isOk()).andExpect(jsonPath("$[0].mensaje_error").value(aviso));
    }

    @Test
    void un_tiempo_agotado_guarda_su_mensaje_fijo() throws Exception {
        Alumno yo = alumnoInscrito();
        prepararProblema();
        when(sandbox.ejecutar(any()))
                .thenReturn(new EjecucionResultado(Estado.TIEMPO_AGOTADO, "", "Tiempo límite excedido.", null));

        enviar(yo).andExpect(status().isCreated());

        historial(yo).andExpect(jsonPath("$[0].mensaje_error")
                .value("Tu programa tardó demasiado en uno o más casos y se detuvo."));
    }

    // El mensaje guardado es el del aviso, que no copia el texto de un error de ejecución: un programa que imprime la
    // entrada de un caso oculto por stderr no deja esa entrada guardada ni en el historial.
    @Test
    void un_error_de_ejecucion_guarda_el_aviso_fijo_y_no_el_texto_del_programa() throws Exception {
        Alumno yo = alumnoInscrito();
        prepararProblema();
        when(sandbox.ejecutar(any())).thenReturn(new EjecucionResultado(Estado.ERROR_EJECUCION, "",
                "ValueError: " + ENTRADA_OCULTA, 5L));

        enviar(yo).andExpect(status().isCreated());

        assertThat(guardado(yo)).isEqualTo("Tu programa falló al ejecutarse en uno o más casos.")
                .doesNotContain(ENTRADA_OCULTA);
        historial(yo).andExpect(jsonPath("$[0].mensaje_error")
                .value("Tu programa falló al ejecutarse en uno o más casos."));
    }

    @Test
    void un_envio_sin_error_queda_con_el_mensaje_nulo() throws Exception {
        Alumno yo = alumnoInscrito();
        prepararProblema();
        when(sandbox.ejecutar(any())).thenReturn(new EjecucionResultado(Estado.OK, "7", "", 5L));

        enviar(yo).andExpect(status().isCreated()).andExpect(jsonPath("$.resultado").value("incorrecto"));

        assertThat(guardado(yo)).isNull();
        historial(yo).andExpect(jsonPath("$[0].mensaje_error").value(nullValue()));
    }

    // Un compilador repite la línea del código donde falló; si ahí hay un NUL, PostgreSQL no lo aceptaría en un texto.
    @Test
    void un_caracter_nul_en_el_texto_del_motor_no_rompe_el_envio() throws Exception {
        Alumno yo = alumnoInscrito();
        prepararProblema();
        when(sandbox.ejecutar(any())).thenReturn(new EjecucionResultado(Estado.ERROR_COMPILACION, "",
                "Main.java:2: error: x\0y", null));

        enviar(yo).andExpect(status().isCreated());

        assertThat(guardado(yo)).isEqualTo("Tu código no compiló. Revisa la línea 2. Main.java:2: error: xy");
    }

    @Test
    void cada_envio_guarda_su_propio_mensaje() throws Exception {
        Alumno yo = alumnoInscrito();
        prepararProblema();
        when(sandbox.ejecutar(any()))
                .thenReturn(new EjecucionResultado(Estado.TIEMPO_AGOTADO, "", "", null))
                .thenReturn(new EjecucionResultado(Estado.TIEMPO_AGOTADO, "", "", null))
                .thenReturn(new EjecucionResultado(Estado.OK, "5", "", 5L))
                .thenReturn(new EjecucionResultado(Estado.OK, "", "", 5L));

        enviar(yo);
        enviar(yo);

        // Del más reciente al más antiguo: el segundo envío (sin error) y luego el primero.
        historial(yo)
                .andExpect(jsonPath("$[0].numero_intento").value(2))
                .andExpect(jsonPath("$[0].mensaje_error").value(nullValue()))
                .andExpect(jsonPath("$[1].numero_intento").value(1))
                .andExpect(jsonPath("$[1].mensaje_error")
                        .value("Tu programa tardó demasiado en uno o más casos y se detuvo."));
    }
}
