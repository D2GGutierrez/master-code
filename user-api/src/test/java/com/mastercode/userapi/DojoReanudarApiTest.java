package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.mastercode.userapi.admin.AdminNoDisponibleException;
import com.mastercode.userapi.admin.ProblemaNoVisibleException;

// HU55-T2 (CA-55.2): GET /api/dojo/sesiones/{id}, lo que la app usa para ofrecer «continuar donde quedó». Devuelve cómo va
// la sesión y el ejercicio que se dejó sin responder, sin entregar ni apuntar nada. admin-api es un mock.
class DojoReanudarApiTest extends EjerciciosMovilesBase {

    private ResultActions estado(String email, long sesion) throws Exception {
        return mvc.perform(get("/api/dojo/sesiones/" + sesion).with(tokenDeCliente(email, "mobile")));
    }

    private ResultActions terminar(String email, long sesion) throws Exception {
        return mvc.perform(post("/api/dojo/sesiones/" + sesion + "/terminar").with(tokenDeCliente(email, "mobile")));
    }

    private int entregas(long sesion) {
        return jdbc.sql("SELECT count(*) FROM app_schema.app_sesion_dojo_ejercicio WHERE sesion_id = ?")
                .param(sesion).query(Integer.class).single();
    }

    @Test
    void una_sesion_recien_abierta_no_tiene_ejercicio_que_retomar() throws Exception {
        publicar(prediccion(1, "facil", "Bucles", "ok"));
        String yo = alumno();
        long sesion = sesion(yo);

        estado(yo, sesion).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sesion))
                .andExpect(jsonPath("$.lenguaje").value("python"))
                .andExpect(jsonPath("$.nivel").value("facil"))
                .andExpect(jsonPath("$.abierta").value(true))
                .andExpect(jsonPath("$.aciertos").value(0))
                .andExpect(jsonPath("$.respondidos").value(0))
                .andExpect(jsonPath("$.ejercicio_actual").value((Object) null));
    }

    // CA-55.2: el ejercicio que se dejó a medias es el que se retoma, y es el mismo cada vez que se pide.
    @Test
    void el_ejercicio_entregado_y_sin_responder_es_el_que_se_retoma() throws Exception {
        publicar(prediccion(1, "facil", "Bucles", "ok"), prediccion(2, "facil", "Listas", "ok"));
        String yo = alumno();
        long sesion = sesion(yo);
        long entregado = idDelSiguiente(yo, sesion);

        estado(yo, sesion).andExpect(status().isOk())
                .andExpect(jsonPath("$.ejercicio_actual.id").value(entregado))
                .andExpect(jsonPath("$.ejercicio_actual.tipo_ejercicio").value("prediccion_salida"))
                .andExpect(jsonPath("$.ejercicio_actual.intentos_restantes").value(3));
        estado(yo, sesion).andExpect(jsonPath("$.ejercicio_actual.id").value(entregado));
    }

    @Test
    void pedir_el_estado_no_entrega_ni_apunta_ningun_ejercicio() throws Exception {
        publicar(prediccion(1, "facil", "Bucles", "ok"), prediccion(2, "facil", "Listas", "ok"));
        String yo = alumno();
        long sesion = sesion(yo);
        idDelSiguiente(yo, sesion);

        estado(yo, sesion).andExpect(status().isOk());
        estado(yo, sesion).andExpect(status().isOk());

        assertThat(entregas(sesion)).isEqualTo(1);
    }

    @Test
    void despues_de_retomar_el_siguiente_es_otro_ejercicio_y_no_el_que_ya_se_dejo() throws Exception {
        publicar(prediccion(1, "facil", "Bucles", "ok"), prediccion(2, "facil", "Listas", "ok"));
        String yo = alumno();
        long sesion = sesion(yo);
        long primero = idDelSiguiente(yo, sesion);
        estado(yo, sesion).andExpect(jsonPath("$.ejercicio_actual.id").value(primero));

        responder(yo, primero, "ok").andExpect(status().isOk());
        long segundo = idDelSiguiente(yo, sesion);

        assertThat(segundo).isNotEqualTo(primero);
    }

    @Test
    void con_el_ejercicio_ya_respondido_no_hay_nada_que_retomar_y_los_contadores_son_los_del_servidor() throws Exception {
        publicar(prediccion(1, "facil", "Bucles", "ok"), prediccion(2, "facil", "Listas", "ok"));
        String yo = alumno();
        long sesion = sesion(yo);
        long primero = idDelSiguiente(yo, sesion);
        enviar(yo, primero, "ok", null, sesion, null).andExpect(status().isOk());

        estado(yo, sesion).andExpect(status().isOk())
                .andExpect(jsonPath("$.aciertos").value(1))
                .andExpect(jsonPath("$.respondidos").value(1))
                .andExpect(jsonPath("$.ejercicio_actual").value((Object) null));
    }

    @Test
    void un_ejercicio_fallado_cuenta_como_respondido_y_no_como_acierto() throws Exception {
        publicar(prediccion(1, "facil", "Bucles", "ok"));
        String yo = alumno();
        long sesion = sesion(yo);
        long primero = idDelSiguiente(yo, sesion);
        enviar(yo, primero, "mal", null, sesion, null).andExpect(status().isOk());

        estado(yo, sesion).andExpect(jsonPath("$.aciertos").value(0))
                .andExpect(jsonPath("$.respondidos").value(1))
                .andExpect(jsonPath("$.ejercicio_actual").value((Object) null));
    }

    @Test
    void si_se_entregaron_dos_sin_responder_se_retoma_el_ultimo() throws Exception {
        publicar(prediccion(1, "facil", "Bucles", "ok"), prediccion(2, "facil", "Listas", "ok"));
        String yo = alumno();
        long sesion = sesion(yo);
        idDelSiguiente(yo, sesion);
        long segundo = idDelSiguiente(yo, sesion);

        estado(yo, sesion).andExpect(jsonPath("$.ejercicio_actual.id").value(segundo));
    }

    @Test
    void en_opcion_multiple_el_ejercicio_retomado_no_marca_la_alternativa_correcta_ni_trae_la_respuesta() throws Exception {
        publicar(multiple(3, "facil", "Bucles"));
        String yo = alumno();
        long sesion = sesion(yo);
        idDelSiguiente(yo, sesion);

        estado(yo, sesion).andExpect(status().isOk())
                .andExpect(jsonPath("$.ejercicio_actual.opciones.length()").value(3))
                .andExpect(content().string(not(containsString("es_correcta"))))
                .andExpect(content().string(not(containsString("respuesta_esperada"))));
    }

    @Test
    void una_sesion_terminada_sale_cerrada_sin_ejercicio_y_con_sus_cuentas() throws Exception {
        publicar(prediccion(1, "facil", "Bucles", "ok"), prediccion(2, "facil", "Listas", "ok"));
        String yo = alumno();
        long sesion = sesion(yo);
        long primero = idDelSiguiente(yo, sesion);
        enviar(yo, primero, "ok", null, sesion, null).andExpect(status().isOk());
        idDelSiguiente(yo, sesion);
        terminar(yo, sesion).andExpect(status().isOk());

        estado(yo, sesion).andExpect(status().isOk())
                .andExpect(jsonPath("$.abierta").value(false))
                .andExpect(jsonPath("$.aciertos").value(1))
                .andExpect(jsonPath("$.respondidos").value(1))
                .andExpect(jsonPath("$.ejercicio_actual").value((Object) null));
    }

    @Test
    void si_el_ejercicio_pendiente_ya_no_es_visible_no_se_retoma_y_la_app_pide_el_siguiente() throws Exception {
        publicar(prediccion(1, "facil", "Bucles", "ok"));
        String yo = alumno();
        long sesion = sesion(yo);
        idDelSiguiente(yo, sesion);
        when(admin.problema(1)).thenThrow(new ProblemaNoVisibleException());

        estado(yo, sesion).andExpect(status().isOk())
                .andExpect(jsonPath("$.abierta").value(true))
                .andExpect(jsonPath("$.ejercicio_actual").value((Object) null));
    }

    @Test
    void si_admin_api_no_responde_da_503_y_no_cambia_la_sesion() throws Exception {
        publicar(prediccion(1, "facil", "Bucles", "ok"));
        String yo = alumno();
        long sesion = sesion(yo);
        idDelSiguiente(yo, sesion);
        when(admin.problema(1)).thenThrow(new AdminNoDisponibleException("sin conexión", null));

        estado(yo, sesion).andExpect(status().isServiceUnavailable());
        assertThat(entregas(sesion)).isEqualTo(1);
    }

    @Test
    void la_sesion_de_otro_estudiante_o_una_que_no_existe_dan_404() throws Exception {
        publicar(prediccion(1, "facil", "Bucles", "ok"));
        long sesionAjena = sesion(alumno());

        estado(alumno(), sesionAjena).andExpect(status().isNotFound());
        estado(alumno(), 999_999_999L).andExpect(status().isNotFound());
    }

    @Test
    void sin_sesion_responde_401_y_un_docente_recibe_403() throws Exception {
        mvc.perform(get("/api/dojo/sesiones/1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/dojo/sesiones/1").with(tokenDe(correoUnico(), "docente"))).andExpect(status().isForbidden());
    }
}
