package com.mastercode.userapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.mastercode.userapi.sandbox.EjecucionResultado;
import com.mastercode.userapi.sandbox.EjecucionSolicitud;
import com.mastercode.userapi.sandbox.Estado;
import com.mastercode.userapi.sandbox.LenguajeNoSoportadoException;
import com.mastercode.userapi.sandbox.SandboxCliente;
import com.mastercode.userapi.sandbox.SandboxNoDisponibleException;

// HU07-T3: el cliente envía código, lenguaje y entrada y recupera salida, errores y tiempos.
// Sin ai-service real: MockRestServiceServer responde en su lugar.
class SandboxClienteTest {

    private static final EjecucionSolicitud SOLICITUD =
            new EjecucionSolicitud("print(int(input()) + 1)", "python", "4");

    private MockRestServiceServer servidor;
    private SandboxCliente cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ai-service:8002");
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new SandboxCliente(builder.build());
    }

    @Test
    void envia_codigo_lenguaje_y_entrada_y_recupera_salida_y_tiempo() {
        servidor.expect(requestTo("http://ai-service:8002/execute"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(jsonPath("$.codigo").value("print(int(input()) + 1)"))
                .andExpect(jsonPath("$.lenguaje").value("python"))
                .andExpect(jsonPath("$.entrada").value("4"))
                .andRespond(withSuccess(
                        "{\"estado\":\"ok\",\"salida\":\"5\\n\",\"error\":\"\",\"tiempo_ms\":12}",
                        MediaType.APPLICATION_JSON));

        EjecucionResultado resultado = cliente.ejecutar(SOLICITUD);

        assertEquals(Estado.OK, resultado.estado());
        assertEquals("5\n", resultado.salida());
        assertEquals(12L, resultado.tiempoMs());
        servidor.verify();
    }

    @Test
    void un_error_del_programa_llega_como_resultado_y_no_como_excepcion() {
        servidor.expect(requestTo("http://ai-service:8002/execute"))
                .andRespond(withSuccess(
                        "{\"estado\":\"error_ejecucion\",\"salida\":\"\",\"error\":\"ZeroDivisionError\"}",
                        MediaType.APPLICATION_JSON));

        EjecucionResultado resultado = cliente.ejecutar(SOLICITUD);

        assertEquals(Estado.ERROR_EJECUCION, resultado.estado());
        assertEquals("ZeroDivisionError", resultado.error());
        assertNull(resultado.tiempoMs());
    }

    @Test
    void los_demas_estados_se_traducen() {
        String[][] casos = {
                {"error_compilacion", "ERROR_COMPILACION"},
                {"tiempo_agotado", "TIEMPO_AGOTADO"},
                {"error_servicio", "ERROR_SERVICIO"},
        };
        for (String[] caso : casos) {
            servidor.reset();
            servidor.expect(requestTo("http://ai-service:8002/execute"))
                    .andRespond(withSuccess("{\"estado\":\"" + caso[0] + "\"}", MediaType.APPLICATION_JSON));

            assertEquals(Estado.valueOf(caso[1]), cliente.ejecutar(SOLICITUD).estado());
        }
    }

    @Test
    void un_lenguaje_no_soportado_lanza_su_excepcion() {
        servidor.expect(requestTo("http://ai-service:8002/execute"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThrows(LenguajeNoSoportadoException.class, () -> cliente.ejecutar(SOLICITUD));
    }

    @Test
    void si_ai_service_falla_lanza_sandbox_no_disponible() {
        servidor.expect(requestTo("http://ai-service:8002/execute")).andRespond(withServerError());

        assertThrows(SandboxNoDisponibleException.class, () -> cliente.ejecutar(SOLICITUD));
    }

    @Test
    void si_no_hay_conexion_lanza_sandbox_no_disponible() {
        servidor.expect(requestTo("http://ai-service:8002/execute"))
                .andRespond(withException(new IOException("sin conexión")));

        assertThrows(SandboxNoDisponibleException.class, () -> cliente.ejecutar(SOLICITUD));
    }

    @Test
    void una_respuesta_con_estado_desconocido_o_vacia_lanza_sandbox_no_disponible() {
        servidor.expect(requestTo("http://ai-service:8002/execute"))
                .andRespond(withSuccess("{\"estado\":\"raro\"}", MediaType.APPLICATION_JSON));
        assertThrows(SandboxNoDisponibleException.class, () -> cliente.ejecutar(SOLICITUD));

        servidor.reset();
        servidor.expect(requestTo("http://ai-service:8002/execute"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertThrows(SandboxNoDisponibleException.class, () -> cliente.ejecutar(SOLICITUD));
    }
}
