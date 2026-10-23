package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.mastercode.userapi.sandbox.EjecucionResultado;
import com.mastercode.userapi.sandbox.EjecucionSolicitud;
import com.mastercode.userapi.sandbox.Estado;
import com.mastercode.userapi.sandbox.LenguajeNoSoportadoException;
import com.mastercode.userapi.sandbox.SandboxNoDisponibleException;

// HU06-T1 (CA-06.2, CA-06.3): «Ejecutar» devuelve la salida o el error y no crea ningún envío.
class EjecucionApiTest extends PruebaDeApi {

    private ResultActions ejecutar(String email, String cuerpo) throws Exception {
        return mvc.perform(post("/api/ejecuciones")
                .with(tokenDe(email, "estudiante"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    private int enviosEnLaBase() {
        return jdbc.sql("SELECT count(*) FROM app_schema.app_envio").query(Integer.class).single();
    }

    @Test
    void devuelve_la_salida_el_estado_y_el_tiempo_del_sandbox() throws Exception {
        when(sandbox.ejecutar(any())).thenReturn(new EjecucionResultado(Estado.OK, "5\n", "", 12L));

        ejecutar(correoUnico(), "{\"codigo\":\"print(int(input()) + 1)\",\"lenguaje\":\"python\",\"entrada\":\"4\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ok"))
                .andExpect(jsonPath("$.salida").value("5\n"))
                .andExpect(jsonPath("$.error").value(""))
                .andExpect(jsonPath("$.tiempo_ms").value(12));

        ArgumentCaptor<EjecucionSolicitud> enviada = ArgumentCaptor.forClass(EjecucionSolicitud.class);
        verify(sandbox).ejecutar(enviada.capture());
        assertThat(enviada.getValue()).isEqualTo(new EjecucionSolicitud("print(int(input()) + 1)", "python", "4"));
    }

    @Test
    void un_programa_que_falla_es_una_respuesta_200_con_su_error() throws Exception {
        when(sandbox.ejecutar(any()))
                .thenReturn(new EjecucionResultado(Estado.ERROR_EJECUCION, "", "ZeroDivisionError", null));

        ejecutar(correoUnico(), "{\"codigo\":\"1/0\",\"lenguaje\":\"python\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("error_ejecucion"))
                .andExpect(jsonPath("$.error").value("ZeroDivisionError"));
    }

    @Test
    void tiempo_agotado_y_error_de_compilacion_tambien_son_200() throws Exception {
        when(sandbox.ejecutar(any()))
                .thenReturn(new EjecucionResultado(Estado.TIEMPO_AGOTADO, "", "Tiempo límite excedido.", null))
                .thenReturn(new EjecucionResultado(Estado.ERROR_COMPILACION, "", "';' expected", null));

        ejecutar(correoUnico(), "{\"codigo\":\"while True: pass\",\"lenguaje\":\"python\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("tiempo_agotado"));
        ejecutar(correoUnico(), "{\"codigo\":\"class Main {\",\"lenguaje\":\"java\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("error_compilacion"));
    }

    @Test
    void ejecutar_no_crea_ninguna_fila_en_app_envio() throws Exception {
        String email = correoUnico();
        long id = crearEstudiante(email);
        when(sandbox.ejecutar(any())).thenReturn(new EjecucionResultado(Estado.OK, "x", "", 1L));
        int antes = enviosEnLaBase();

        ejecutar(email, "{\"codigo\":\"print('x')\",\"lenguaje\":\"python\"}").andExpect(status().isOk());
        ejecutar(email, "{\"codigo\":\"print('x')\",\"lenguaje\":\"python\"}").andExpect(status().isOk());

        assertThat(enviosEnLaBase()).isEqualTo(antes);
        Integer delEstudiante = jdbc.sql("SELECT count(*) FROM app_schema.app_envio WHERE estudiante_id = ?")
                .param(id)
                .query(Integer.class)
                .single();
        assertThat(delEstudiante).isZero();
    }

    @Test
    void el_lenguaje_se_normaliza_y_la_entrada_ausente_va_vacia() throws Exception {
        when(sandbox.ejecutar(any())).thenReturn(new EjecucionResultado(Estado.OK, "", "", 1L));

        ejecutar(correoUnico(), "{\"codigo\":\"x\",\"lenguaje\":\" Python \"}").andExpect(status().isOk());

        ArgumentCaptor<EjecucionSolicitud> enviada = ArgumentCaptor.forClass(EjecucionSolicitud.class);
        verify(sandbox).ejecutar(enviada.capture());
        assertThat(enviada.getValue()).isEqualTo(new EjecucionSolicitud("x", "python", ""));
    }

    @Test
    void un_lenguaje_no_soportado_da_400_sin_llamar_al_sandbox() throws Exception {
        ejecutar(correoUnico(), "{\"codigo\":\"x\",\"lenguaje\":\"cobol\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Lenguaje no soportado. Usa python, java o javascript."));
        ejecutar(correoUnico(), "{\"codigo\":\"x\"}").andExpect(status().isBadRequest());

        verifyNoInteractions(sandbox);
    }

    @Test
    void un_codigo_vacio_ausente_o_demasiado_largo_da_400_sin_llamar_al_sandbox() throws Exception {
        ejecutar(correoUnico(), "{\"codigo\":\"  \",\"lenguaje\":\"python\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Escribe tu código antes de ejecutarlo."));
        ejecutar(correoUnico(), "{\"lenguaje\":\"python\"}").andExpect(status().isBadRequest());
        ejecutar(correoUnico(), "{\"codigo\":\"" + "x".repeat(50_001) + "\",\"lenguaje\":\"python\"}")
                .andExpect(status().isBadRequest());

        verifyNoInteractions(sandbox);
    }

    @Test
    void una_entrada_demasiado_larga_da_400_sin_llamar_al_sandbox() throws Exception {
        ejecutar(correoUnico(), "{\"codigo\":\"x\",\"lenguaje\":\"python\",\"entrada\":\"" + "y".repeat(10_001) + "\"}")
                .andExpect(status().isBadRequest());

        verifyNoInteractions(sandbox);
    }

    @Test
    void si_el_sandbox_no_esta_disponible_da_503() throws Exception {
        when(sandbox.ejecutar(any())).thenThrow(new SandboxNoDisponibleException("sin conexión", null));

        ejecutar(correoUnico(), "{\"codigo\":\"x\",\"lenguaje\":\"python\"}").andExpect(status().isServiceUnavailable());
    }

    @Test
    void si_el_motor_falla_por_su_cuenta_da_503_y_no_un_error_del_estudiante() throws Exception {
        when(sandbox.ejecutar(any()))
                .thenReturn(new EjecucionResultado(Estado.ERROR_SERVICIO, "", "El motor no respondió.", null));

        ejecutar(correoUnico(), "{\"codigo\":\"x\",\"lenguaje\":\"python\"}").andExpect(status().isServiceUnavailable());
    }

    @Test
    void si_el_motor_no_tiene_el_lenguaje_da_400() throws Exception {
        when(sandbox.ejecutar(any())).thenThrow(new LenguajeNoSoportadoException("java"));

        ejecutar(correoUnico(), "{\"codigo\":\"x\",\"lenguaje\":\"java\"}").andExpect(status().isBadRequest());
    }

    @Test
    void sin_token_da_401_y_con_token_de_docente_403() throws Exception {
        mvc.perform(post("/api/ejecuciones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"x\",\"lenguaje\":\"python\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/ejecuciones")
                        .with(tokenDe(correoUnico(), "docente"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"x\",\"lenguaje\":\"python\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(sandbox);
    }
}
