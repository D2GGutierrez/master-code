package com.mastercode.userapi;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

// HU52-T1 (D16): PUT /api/dispositivo, donde la app registra el token de notificaciones de ese teléfono.
class DispositivoApiTest extends PruebaDeApi {

    private ResultActions registrar(String email, String cuerpo) throws Exception {
        return mvc.perform(put("/api/dispositivo").with(tokenDeCliente(email, "mobile"))
                .contentType(APPLICATION_JSON).content(cuerpo));
    }

    private int filasDelToken(String token) {
        return jdbc.sql("SELECT count(*) FROM app_schema.app_dispositivo WHERE token = ?")
                .param(token).query(Integer.class).single();
    }

    private long duenoDelToken(String token) {
        return jdbc.sql("SELECT estudiante_id FROM app_schema.app_dispositivo WHERE token = ?")
                .param(token).query(Long.class).single();
    }

    @Test
    void guarda_el_token_a_nombre_del_estudiante_de_la_sesion() throws Exception {
        String email = correoUnico();
        long yo = crearEstudiante(email);

        registrar(email, "{\"token\":\"token-fcm-1\"}").andExpect(status().isNoContent());

        assertThat(filasDelToken("token-fcm-1")).isEqualTo(1);
        assertThat(duenoDelToken("token-fcm-1")).isEqualTo(yo);
    }

    @Test
    void repetir_el_mismo_token_no_duplica_la_fila() throws Exception {
        String email = correoUnico();
        crearEstudiante(email);

        registrar(email, "{\"token\":\"token-fcm-2\"}").andExpect(status().isNoContent());
        registrar(email, "{\"token\":\"token-fcm-2\"}").andExpect(status().isNoContent());

        assertThat(filasDelToken("token-fcm-2")).isEqualTo(1);
    }

    @Test
    void si_otro_estudiante_inicia_sesion_en_el_mismo_telefono_el_token_cambia_de_dueno() throws Exception {
        String primero = correoUnico();
        String segundo = correoUnico();
        crearEstudiante(primero);
        long idSegundo = crearEstudiante(segundo);

        registrar(primero, "{\"token\":\"token-fcm-3\"}").andExpect(status().isNoContent());
        registrar(segundo, "{\"token\":\"token-fcm-3\"}").andExpect(status().isNoContent());

        assertThat(filasDelToken("token-fcm-3")).isEqualTo(1);
        assertThat(duenoDelToken("token-fcm-3")).isEqualTo(idSegundo);
    }

    @Test
    void un_estudiante_puede_tener_varios_telefonos() throws Exception {
        String email = correoUnico();
        long yo = crearEstudiante(email);

        registrar(email, "{\"token\":\"token-fcm-4a\"}").andExpect(status().isNoContent());
        registrar(email, "{\"token\":\"token-fcm-4b\"}").andExpect(status().isNoContent());

        assertThat(jdbc.sql("SELECT count(*) FROM app_schema.app_dispositivo WHERE estudiante_id = ?")
                .param(yo).query(Integer.class).single()).isEqualTo(2);
    }

    @Test
    void el_estudiante_sale_de_la_sesion_aunque_el_cuerpo_traiga_otro_dato() throws Exception {
        String email = correoUnico();
        long yo = crearEstudiante(email);
        long otro = crearEstudiante(correoUnico());

        registrar(email, "{\"token\":\"token-fcm-5\",\"estudiante_id\":" + otro + "}").andExpect(status().isNoContent());

        assertThat(duenoDelToken("token-fcm-5")).isEqualTo(yo);
    }

    @Test
    void quita_los_espacios_alrededor_del_token() throws Exception {
        String email = correoUnico();
        crearEstudiante(email);

        registrar(email, "{\"token\":\"  token-fcm-6  \"}").andExpect(status().isNoContent());

        assertThat(filasDelToken("token-fcm-6")).isEqualTo(1);
    }

    @Test
    void sin_token_o_con_token_en_blanco_es_un_400_con_mensaje() throws Exception {
        String email = correoUnico();
        crearEstudiante(email);

        registrar(email, "{}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Falta el token del dispositivo."));
        registrar(email, "{\"token\":\"   \"}").andExpect(status().isBadRequest());
    }

    @Test
    void un_token_demasiado_largo_es_un_400_y_no_se_guarda() throws Exception {
        String email = correoUnico();
        crearEstudiante(email);
        String largo = "t".repeat(1_025);

        registrar(email, "{\"token\":\"" + largo + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString(largo))));

        assertThat(filasDelToken(largo)).isZero();
    }

    @Test
    void sin_token_de_sesion_responde_401() throws Exception {
        mvc.perform(put("/api/dispositivo").contentType(APPLICATION_JSON).content("{\"token\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }
}
