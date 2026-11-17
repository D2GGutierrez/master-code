package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.mastercode.userapi.admin.ProblemaDto;

// HU45-T3 (CA-45.5, CA-24.4): tres intentos por ejercicio del celular, los que quedan a la vista, cada intento como un
// envío más (plataforma movil) y los reenvíos sin contar dos veces (HU48-T3). admin-api es un mock.
class IntentosMovilApiTest extends EjerciciosMovilesBase {

    private static final String UUID_A = "11111111-1111-1111-1111-111111111111";
    private static final String UUID_B = "22222222-2222-2222-2222-222222222222";
    private static final String UUID_C = "33333333-3333-3333-3333-333333333333";

    @Test
    void cada_respuesta_queda_como_un_envio_del_celular_con_su_numero_de_intento() throws Exception {
        String yo = alumno();
        publicar(prediccion(7, "facil", "Bucles", "ok"));

        responder(yo, 7, "no").andExpect(status().isOk());
        responder(yo, 7, "ok").andExpect(status().isOk());

        List<Map<String, Object>> filas = jdbc.sql("""
                SELECT numero_intento, plataforma, resultado, puntaje, codigo FROM app_schema.app_envio
                WHERE estudiante_id = ? AND problema_id = 7 ORDER BY numero_intento
                """).param(idDe(yo)).query().listOfRows();
        assertThat(filas).hasSize(2);
        assertThat(filas.get(0)).containsEntry("plataforma", "movil").containsEntry("resultado", "incorrecto")
                .containsEntry("codigo", "no");
        assertThat(((Number) filas.get(0).get("numero_intento")).intValue()).isEqualTo(1);
        assertThat(filas.get(1)).containsEntry("resultado", "correcto").containsEntry("codigo", "ok");
        assertThat(((Number) filas.get(1).get("numero_intento")).intValue()).isEqualTo(2);
    }

    @Test
    void en_opcion_multiple_se_guarda_la_alternativa_elegida() throws Exception {
        String yo = alumno();
        publicar(multiple(8, "facil", "Listas"));

        enviar(yo, 8, null, 801L, null, null).andExpect(status().isOk());

        assertThat(jdbc.sql("SELECT codigo FROM app_schema.app_envio WHERE estudiante_id = ?").param(idDe(yo))
                .query(String.class).single()).isEqualTo("opcion:801");
    }

    @Test
    void los_intentos_restantes_bajan_con_cada_respuesta_y_el_cuarto_se_rechaza() throws Exception {
        String yo = alumno();
        publicar(prediccion(7, "facil", "Bucles", "ok"));

        responder(yo, 7, "no").andExpect(status().isOk()).andExpect(jsonPath("$.intentos_restantes").value(2));
        responder(yo, 7, "no").andExpect(status().isOk()).andExpect(jsonPath("$.intentos_restantes").value(1));
        responder(yo, 7, "ok").andExpect(status().isOk()).andExpect(jsonPath("$.intentos_restantes").value(0));

        responder(yo, 7, "ok").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya usaste los 3 intentos de este ejercicio."));
        assertThat(intentosGuardados(idDe(yo), 7)).isEqualTo(3);
    }

    @Test
    void los_intentos_son_de_cada_estudiante_y_de_cada_ejercicio() throws Exception {
        String yo = alumno();
        String otro = alumno();
        publicar(prediccion(7, "facil", "Bucles", "ok"), prediccion(9, "facil", "Bucles", "ok"));
        for (int i = 0; i < 3; i++) {
            responder(yo, 7, "no").andExpect(status().isOk());
        }

        responder(otro, 7, "no").andExpect(status().isOk()).andExpect(jsonPath("$.intentos_restantes").value(2));
        responder(yo, 9, "no").andExpect(status().isOk()).andExpect(jsonPath("$.intentos_restantes").value(2));
    }

    @Test
    void una_respuesta_invalida_no_gasta_un_intento() throws Exception {
        String yo = alumno();
        publicar(prediccion(7, "facil", "Bucles", "ok"));

        responder(yo, 7, "").andExpect(status().isBadRequest());

        assertThat(intentosGuardados(idDe(yo), 7)).isZero();
        responder(yo, 7, "no").andExpect(status().isOk()).andExpect(jsonPath("$.intentos_restantes").value(2));
    }

    @Test
    void el_detalle_del_ejercicio_dice_cuantos_intentos_quedan_y_los_de_codigo_no_tienen_limite() throws Exception {
        String yo = alumno();
        publicar(prediccion(7, "facil", "Bucles", "ok"));
        when(admin.problema(50)).thenReturn(new ProblemaDto(50, "Suma", "Suma dos.", "python", "facil", "web",
                "escritura_codigo", "", List.of(), CURSO));
        responder(yo, 7, "no").andExpect(status().isOk());

        mvc.perform(get("/api/problemas/7").with(tokenDeCliente(yo, "mobile")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.intentos_restantes").value(2));
        mvc.perform(get("/api/problemas/50").with(tokenDeCliente(yo, "mobile")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.intentos_restantes").doesNotExist());
    }

    @Test
    void el_dojo_trae_los_intentos_restantes_y_ya_no_ofrece_un_ejercicio_con_los_intentos_agotados() throws Exception {
        String yo = alumno();
        publicar(prediccion(7, "facil", "Bucles", "ok"));
        for (int i = 0; i < 3; i++) {
            responder(yo, 7, "no").andExpect(status().isOk());
        }
        pedirSiguiente(yo, sesion(yo)).andExpect(status().isNoContent());

        String otro = alumno();
        responder(otro, 7, "no").andExpect(status().isOk());
        pedirSiguiente(otro, sesion(otro)).andExpect(status().isOk()).andExpect(jsonPath("$.intentos_restantes").value(2));
    }

    @Test
    void dentro_de_una_sesion_la_respuesta_tambien_cuenta_como_intento() throws Exception {
        String yo = alumno();
        publicar(prediccion(7, "facil", "Bucles", "ok"));
        long sesion = sesion(yo);
        long problema = idDelSiguiente(yo, sesion);

        enviar(yo, problema, "no", null, sesion, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.intentos_restantes").value(2));

        assertThat(intentosGuardados(idDe(yo), problema)).isEqualTo(1);
    }

    @Test
    void el_intento_del_celular_aparece_en_el_avance_que_ve_la_web() throws Exception {
        String yo = alumno();
        publicar(prediccion(7, "facil", "Bucles", "ok"), prediccion(9, "facil", "Bucles", "ok"));

        responder(yo, 7, "ok").andExpect(status().isOk());
        responder(yo, 9, "no").andExpect(status().isOk());

        mvc.perform(get("/api/cursos/10/avance").with(tokenDe(yo, "estudiante")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resueltos").value(1))
                .andExpect(jsonPath("$.intentados").value(1));
    }

    // ---------- reenvíos tras perder la señal (HU48-T3, CA-48.5) ----------

    @Test
    void un_reenvio_con_el_mismo_intento_id_se_registra_una_sola_vez() throws Exception {
        String yo = alumno();
        publicar(prediccion(7, "facil", "Bucles", "ok"));

        enviar(yo, 7, "no", null, null, UUID_A).andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("incorrecto")).andExpect(jsonPath("$.intentos_restantes").value(2));
        enviar(yo, 7, "no", null, null, UUID_A).andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("incorrecto")).andExpect(jsonPath("$.intentos_restantes").value(2));

        assertThat(intentosGuardados(idDe(yo), 7)).isEqualTo(1);
    }

    @Test
    void dos_intentos_distintos_con_claves_distintas_si_cuentan_los_dos() throws Exception {
        String yo = alumno();
        publicar(prediccion(7, "facil", "Bucles", "ok"));

        enviar(yo, 7, "no", null, null, UUID_A).andExpect(status().isOk());
        enviar(yo, 7, "no", null, null, UUID_B).andExpect(status().isOk());

        assertThat(intentosGuardados(idDe(yo), 7)).isEqualTo(2);
    }

    @Test
    void un_reenvio_se_acepta_aunque_ya_no_queden_intentos_y_devuelve_lo_que_se_perdio() throws Exception {
        String yo = alumno();
        publicar(prediccion(7, "facil", "Bucles", "ok"));
        enviar(yo, 7, "no", null, null, UUID_A).andExpect(status().isOk());
        enviar(yo, 7, "no", null, null, UUID_B).andExpect(status().isOk());
        enviar(yo, 7, "no", null, null, UUID_C).andExpect(status().isOk());

        enviar(yo, 7, "no", null, null, UUID_C).andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("incorrecto"))
                .andExpect(jsonPath("$.respuesta_correcta").value("ok"))
                .andExpect(jsonPath("$.intentos_restantes").value(0));
        enviar(yo, 7, "no", null, null, "44444444-4444-4444-4444-444444444444").andExpect(status().isConflict());
        assertThat(intentosGuardados(idDe(yo), 7)).isEqualTo(3);
    }

    @Test
    void un_reenvio_dentro_de_una_sesion_ya_terminada_devuelve_el_mismo_resultado_sin_contar_otra_vez() throws Exception {
        String yo = alumno();
        publicar(prediccion(7, "facil", "Bucles", "ok"));
        long sesion = sesion(yo);
        long problema = idDelSiguiente(yo, sesion);
        enviar(yo, problema, "ok", null, sesion, UUID_A).andExpect(status().isOk());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/dojo/sesiones/" + sesion + "/terminar").with(tokenDeCliente(yo, "mobile")))
                .andExpect(status().isOk());

        enviar(yo, problema, "ok", null, sesion, UUID_A).andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("correcto"));

        assertThat(intentosGuardados(idDe(yo), problema)).isEqualTo(1);
        assertThat(jdbc.sql("SELECT racha_aciertos FROM app_schema.app_sesion_dojo WHERE id = ?").param(sesion)
                .query(Integer.class).single()).isEqualTo(1);
    }

    @Test
    void un_reenvio_no_suma_los_puntos_dos_veces() throws Exception {
        String yo = alumno();
        publicar(prediccion(7, "facil", "Bucles", "ok"));

        enviar(yo, 7, "ok", null, null, UUID_A).andExpect(status().isOk());
        enviar(yo, 7, "ok", null, null, UUID_A).andExpect(status().isOk());

        assertThat(jdbc.sql("SELECT puntos FROM app_schema.app_cinturon WHERE estudiante_id = ?").param(idDe(yo))
                .query(Integer.class).single()).isEqualTo(10);
    }

    @Test
    void un_intento_id_demasiado_largo_es_un_400() throws Exception {
        String yo = alumno();
        publicar(prediccion(7, "facil", "Bucles", "ok"));

        enviar(yo, 7, "ok", null, null, "x".repeat(41)).andExpect(status().isBadRequest());

        assertThat(intentosGuardados(idDe(yo), 7)).isZero();
    }
}
