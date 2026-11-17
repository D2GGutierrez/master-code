package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import com.mastercode.userapi.perfil.ZonaHorariaRepository;

// H6 (ADR-13, D23): PUT /api/yo/zona-horaria, donde la app manda la zona horaria del teléfono. Con ella user-api cuenta
// la racha por el día del estudiante y no por el del servidor.
class ZonaHorariaApiTest extends PruebaDeApi {

    @Autowired
    private ZonaHorariaRepository zonas;

    private ResultActions enviar(String email, String cuerpo) throws Exception {
        return mvc.perform(put("/api/yo/zona-horaria").with(tokenDeCliente(email, "mobile"))
                .contentType(APPLICATION_JSON).content(cuerpo));
    }

    private String guardada(long estudianteId) {
        return jdbc.sql("SELECT zona_horaria FROM app_schema.app_estudiante WHERE id = ?")
                .param(estudianteId).query(String.class).optional().orElse(null);
    }

    @Test
    void guarda_la_zona_del_estudiante_de_la_sesion() throws Exception {
        String email = correoUnico();
        long yo = crearEstudiante(email);

        enviar(email, "{\"zona_horaria\":\"Asia/Tokyo\"}").andExpect(status().isNoContent());

        assertThat(guardada(yo)).isEqualTo("Asia/Tokyo");
        assertThat(zonas.de(yo)).isEqualTo(ZoneId.of("Asia/Tokyo"));
    }

    @Test
    void si_el_estudiante_viaja_la_zona_nueva_reemplaza_a_la_anterior() throws Exception {
        String email = correoUnico();
        long yo = crearEstudiante(email);

        enviar(email, "{\"zona_horaria\":\"America/Lima\"}").andExpect(status().isNoContent());
        enviar(email, "{\"zona_horaria\":\"Europe/Madrid\"}").andExpect(status().isNoContent());

        assertThat(guardada(yo)).isEqualTo("Europe/Madrid");
    }

    @Test
    void repetir_la_misma_zona_no_cambia_nada() throws Exception {
        String email = correoUnico();
        long yo = crearEstudiante(email);

        enviar(email, "{\"zona_horaria\":\"America/Bogota\"}").andExpect(status().isNoContent());
        enviar(email, "{\"zona_horaria\":\"America/Bogota\"}").andExpect(status().isNoContent());

        assertThat(guardada(yo)).isEqualTo("America/Bogota");
    }

    @Test
    void acepta_utc_y_quita_los_espacios_de_los_bordes() throws Exception {
        String email = correoUnico();
        long yo = crearEstudiante(email);

        enviar(email, "{\"zona_horaria\":\"UTC\"}").andExpect(status().isNoContent());
        assertThat(guardada(yo)).isEqualTo("UTC");

        enviar(email, "{\"zona_horaria\":\"  America/Lima \"}").andExpect(status().isNoContent());
        assertThat(guardada(yo)).isEqualTo("America/Lima");
    }

    @Test
    void una_zona_que_no_es_un_nombre_de_region_se_rechaza_y_no_se_guarda() throws Exception {
        String email = correoUnico();
        long yo = crearEstudiante(email);
        enviar(email, "{\"zona_horaria\":\"America/Lima\"}").andExpect(status().isNoContent());

        // Desfases fijos, nombres inventados, vacío, solo espacios y demasiado largo.
        String[] invalidas = { "+05:00", "UTC-5", "GMT+2", "Marte/Olimpo", "lima", "", "   ", "America/" + "x".repeat(60) };
        for (String zona : invalidas) {
            enviar(email, "{\"zona_horaria\":\"" + zona + "\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").exists());
        }

        // La última zona válida sigue guardada.
        assertThat(guardada(yo)).isEqualTo("America/Lima");
    }

    @Test
    void sin_el_campo_o_con_null_responde_400() throws Exception {
        String email = correoUnico();
        long yo = crearEstudiante(email);

        enviar(email, "{}").andExpect(status().isBadRequest());
        enviar(email, "{\"zona_horaria\":null}").andExpect(status().isBadRequest());

        assertThat(guardada(yo)).isNull();
    }

    @Test
    void cada_estudiante_guarda_solo_su_zona() throws Exception {
        String uno = correoUnico();
        String otro = correoUnico();
        long idUno = crearEstudiante(uno);
        long idOtro = crearEstudiante(otro);

        enviar(uno, "{\"zona_horaria\":\"Asia/Tokyo\"}").andExpect(status().isNoContent());

        assertThat(guardada(idUno)).isEqualTo("Asia/Tokyo");
        assertThat(guardada(idOtro)).isNull();
    }

    @Test
    void quien_no_es_estudiante_no_puede_guardarla() throws Exception {
        mvc.perform(put("/api/yo/zona-horaria").with(tokenDe(correoUnico(), "docente"))
                .contentType(APPLICATION_JSON).content("{\"zona_horaria\":\"America/Lima\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void sin_sesion_responde_401() throws Exception {
        mvc.perform(put("/api/yo/zona-horaria").contentType(APPLICATION_JSON).content("{\"zona_horaria\":\"America/Lima\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mientras_la_app_no_la_manda_se_usa_la_de_por_defecto() {
        long yo = crearEstudiante(correoUnico());

        assertThat(guardada(yo)).isNull();
        assertThat(zonas.de(yo)).isEqualTo(ZoneId.of("America/Lima"));
    }

    @Test
    void un_valor_danado_en_la_base_no_rompe_el_dia_del_estudiante() {
        long yo = crearEstudiante(correoUnico());
        jdbc.sql("UPDATE app_schema.app_estudiante SET zona_horaria = 'No/Existe' WHERE id = ?").param(yo).update();

        assertThat(zonas.de(yo)).isEqualTo(ZoneId.of("America/Lima"));
    }

    @Test
    void esValida_distingue_regiones_de_desfases() {
        assertThat(ZonaHorariaRepository.esValida("America/Lima")).isTrue();
        assertThat(ZonaHorariaRepository.esValida("America/Argentina/Buenos_Aires")).isTrue();
        assertThat(ZonaHorariaRepository.esValida("UTC")).isTrue();
        assertThat(ZonaHorariaRepository.esValida("+05:00")).isFalse();
        assertThat(ZonaHorariaRepository.esValida("Z")).isFalse();
        assertThat(ZonaHorariaRepository.esValida(null)).isFalse();
    }
}
