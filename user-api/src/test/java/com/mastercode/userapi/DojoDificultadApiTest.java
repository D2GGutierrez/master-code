package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.mastercode.userapi.admin.ProblemaDto;
import com.mastercode.userapi.admin.ProblemaResumenDto;
import com.mastercode.userapi.admin.RespuestaDelProblemaDto;

// HU54 y HU56: la dificultad del Dojo (la regla de docs/regla-dificultad.md aplicada a una sesión), el nivel inicial y el
// guardado al terminar, y el resumen de la sesión. admin-api es un mock; la respuesta correcta de todos es «ok».
class DojoDificultadApiTest extends PruebaDeApi {

    private static final long CURSO = 10;

    // Un ejercicio de predicción de salida del celular, de un nivel y un tema.
    private record Ejercicio(long id, String dificultad, String tema) {
    }

    private static Ejercicio e(long id, String dificultad, String tema) {
        return new Ejercicio(id, dificultad, tema);
    }

    private void publicar(Ejercicio... ejercicios) {
        List<ProblemaResumenDto> resumenes = new ArrayList<>();
        for (Ejercicio x : ejercicios) {
            resumenes.add(new ProblemaResumenDto(x.id(), "Ejercicio " + x.id(), "python", x.dificultad(), "movil",
                    "prediccion_salida", null, x.tema()));
            when(admin.problema(x.id())).thenReturn(new ProblemaDto(x.id(), "Ejercicio " + x.id(), "¿Qué imprime?", "python",
                    x.dificultad(), "movil", "prediccion_salida", "", List.of(), CURSO, List.of(), x.tema()));
            when(admin.respuestaDelProblema(x.id())).thenReturn(
                    new RespuestaDelProblemaDto(x.id(), CURSO, "prediccion_salida", "ok", List.of()));
        }
        when(admin.problemasDelCurso(CURSO, "movil")).thenReturn(resumenes);
    }

    private String alumno() {
        String email = correoUnico();
        inscribir(crearEstudiante(email), CURSO);
        return email;
    }

    private long idEstudiante(String email) {
        return jdbc.sql("SELECT id FROM app_schema.app_estudiante WHERE email = ?").param(email).query(Long.class).single();
    }

    private ResultActions iniciar(String email) throws Exception {
        return mvc.perform(post("/api/dojo/sesiones").with(tokenDeCliente(email, "mobile"))
                .contentType(APPLICATION_JSON).content("{\"lenguaje\":\"python\"}"));
    }

    private long sesion(String email) throws Exception {
        String texto = iniciar(email).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return Long.parseLong(texto.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    // Pide el siguiente ejercicio y devuelve su id.
    private long siguiente(String email, long sesion) throws Exception {
        String texto = mvc.perform(post("/api/dojo/sesiones/" + sesion + "/siguiente").with(tokenDeCliente(email, "mobile")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return Long.parseLong(texto.replaceAll(".*?\"id\":(\\d+).*", "$1"));
    }

    private ResultActions responder(String email, long problema, String respuesta, Long sesion) throws Exception {
        String cuerpo = "{\"respuesta\":\"" + respuesta + "\"" + (sesion == null ? "" : ",\"sesion_id\":" + sesion) + "}";
        return mvc.perform(post("/api/problemas/" + problema + "/respuesta").with(tokenDeCliente(email, "mobile"))
                .contentType(APPLICATION_JSON).content(cuerpo));
    }

    private ResultActions terminar(String email, long sesion) throws Exception {
        return mvc.perform(post("/api/dojo/sesiones/" + sesion + "/terminar").with(tokenDeCliente(email, "mobile")));
    }

    private String nivelDe(long sesion) {
        return jdbc.sql("SELECT nivel FROM app_schema.app_sesion_dojo WHERE id = ?").param(sesion).query(String.class).single();
    }

    private void guardarNivel(String email, String nivel) {
        jdbc.sql("INSERT INTO app_schema.app_nivel_dificultad (estudiante_id, lenguaje, nivel) VALUES (?, 'python', ?)")
                .params(idEstudiante(email), nivel).update();
    }

    // ---------- el nivel inicial (HU54-T4) ----------

    @Test
    void un_estudiante_nuevo_empieza_en_facil() throws Exception {
        String yo = alumno();

        iniciar(yo).andExpect(status().isCreated()).andExpect(jsonPath("$.nivel").value("facil"));
    }

    @Test
    void quien_ya_tiene_un_nivel_guardado_empieza_en_ese_nivel() throws Exception {
        String yo = alumno();
        guardarNivel(yo, "dificil");

        long sesion = sesion(yo);

        assertThat(nivelDe(sesion)).isEqualTo("dificil");
    }

    // ---------- la regla sobre la sesión y la elección del siguiente (HU54-T2) ----------

    @Test
    void dos_aciertos_seguidos_suben_el_nivel_y_el_siguiente_ejercicio_es_de_ese_nivel() throws Exception {
        String yo = alumno();
        publicar(e(1, "facil", "Bucles"), e(2, "facil", "Listas"), e(3, "medio", "Bucles"), e(4, "medio", "Listas"));
        long sesion = sesion(yo);

        responder(yo, siguiente(yo, sesion), "ok", sesion).andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("correcto"));
        assertThat(nivelDe(sesion)).isEqualTo("facil");
        responder(yo, siguiente(yo, sesion), "ok", sesion).andExpect(status().isOk());

        assertThat(nivelDe(sesion)).isEqualTo("medio");
        assertThat(siguiente(yo, sesion)).isIn(3L, 4L);
    }

    @Test
    void dos_errores_seguidos_bajan_el_nivel_y_el_siguiente_ejercicio_es_de_ese_nivel() throws Exception {
        String yo = alumno();
        guardarNivel(yo, "medio");
        publicar(e(1, "facil", "Bucles"), e(3, "medio", "Bucles"), e(4, "medio", "Listas"), e(5, "dificil", "Bucles"));
        long sesion = sesion(yo);

        responder(yo, siguiente(yo, sesion), "no", sesion).andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("incorrecto"));
        responder(yo, siguiente(yo, sesion), "no", sesion).andExpect(status().isOk());

        assertThat(nivelDe(sesion)).isEqualTo("facil");
        assertThat(siguiente(yo, sesion)).isEqualTo(1L);
    }

    @Test
    void un_acierto_entre_dos_errores_no_cambia_el_nivel() throws Exception {
        String yo = alumno();
        guardarNivel(yo, "medio");
        publicar(e(3, "medio", "Bucles"), e(4, "medio", "Listas"), e(6, "medio", "Cadenas"));
        long sesion = sesion(yo);

        responder(yo, siguiente(yo, sesion), "no", sesion);
        responder(yo, siguiente(yo, sesion), "ok", sesion);
        responder(yo, siguiente(yo, sesion), "no", sesion);

        assertThat(nivelDe(sesion)).isEqualTo("medio");
        assertThat(jdbc.sql("SELECT racha_errores FROM app_schema.app_sesion_dojo WHERE id = ?").param(sesion)
                .query(Integer.class).single()).isEqualTo(1);
    }

    @Test
    void sin_ejercicios_del_nivel_se_toma_el_mas_cercano_y_a_igual_distancia_el_mas_facil() throws Exception {
        String yo = alumno();
        guardarNivel(yo, "medio");
        publicar(e(1, "facil", "Bucles"), e(5, "dificil", "Bucles"));
        long sesion = sesion(yo);

        assertThat(siguiente(yo, sesion)).isEqualTo(1L);
        // El nivel de la sesión no cambia por la sustitución: lo cambia solo la regla.
        assertThat(nivelDe(sesion)).isEqualTo("medio");
        assertThat(siguiente(yo, sesion)).isEqualTo(5L);
        mvc.perform(post("/api/dojo/sesiones/" + sesion + "/siguiente").with(tokenDeCliente(yo, "mobile")))
                .andExpect(status().isNoContent());
    }

    @Test
    void un_ejercicio_de_dificultad_desconocida_no_se_ofrece() throws Exception {
        String yo = alumno();
        publicar(e(1, "imposible", "Bucles"));

        mvc.perform(post("/api/dojo/sesiones/" + sesion(yo) + "/siguiente").with(tokenDeCliente(yo, "mobile")))
                .andExpect(status().isNoContent());
    }

    @Test
    void responder_sin_sesion_sigue_funcionando_y_no_mueve_ningun_nivel() throws Exception {
        String yo = alumno();
        publicar(e(1, "facil", "Bucles"));
        long sesion = sesion(yo);

        responder(yo, 1, "ok", null).andExpect(status().isOk()).andExpect(jsonPath("$.resultado").value("correcto"));

        assertThat(jdbc.sql("SELECT racha_aciertos FROM app_schema.app_sesion_dojo WHERE id = ?").param(sesion)
                .query(Integer.class).single()).isZero();
    }

    @Test
    void una_respuesta_invalida_no_cuenta_ni_toca_las_rachas() throws Exception {
        String yo = alumno();
        publicar(e(1, "facil", "Bucles"));
        long sesion = sesion(yo);
        long problema = siguiente(yo, sesion);

        responder(yo, problema, "", sesion).andExpect(status().isBadRequest());

        assertThat(jdbc.sql("SELECT acerto FROM app_schema.app_sesion_dojo_ejercicio WHERE sesion_id = ?").param(sesion)
                .query(Boolean.class).optional()).isEmpty();
        assertThat(nivelDe(sesion)).isEqualTo("facil");
        // Sigue pendiente: se puede responder bien después.
        responder(yo, problema, "ok", sesion).andExpect(status().isOk());
    }

    @Test
    void el_mismo_ejercicio_no_cuenta_dos_veces_en_la_sesion() throws Exception {
        String yo = alumno();
        publicar(e(1, "facil", "Bucles"), e(2, "facil", "Listas"));
        long sesion = sesion(yo);
        long problema = siguiente(yo, sesion);

        responder(yo, problema, "ok", sesion).andExpect(status().isOk());
        responder(yo, problema, "ok", sesion).andExpect(status().isConflict());

        assertThat(jdbc.sql("SELECT racha_aciertos FROM app_schema.app_sesion_dojo WHERE id = ?").param(sesion)
                .query(Integer.class).single()).isEqualTo(1);
    }

    @Test
    void un_ejercicio_que_no_se_entrego_en_la_sesion_da_409() throws Exception {
        String yo = alumno();
        publicar(e(1, "facil", "Bucles"), e(2, "facil", "Listas"));

        responder(yo, 2, "ok", sesion(yo)).andExpect(status().isConflict());
    }

    @Test
    void la_sesion_de_otro_estudiante_es_un_404_y_una_terminada_un_409() throws Exception {
        String yo = alumno();
        String otro = alumno();
        publicar(e(1, "facil", "Bucles"), e(2, "facil", "Listas"));
        long sesionAjena = sesion(otro);
        long mia = sesion(yo);
        long problema = siguiente(yo, mia);

        responder(yo, problema, "ok", sesionAjena).andExpect(status().isNotFound());
        terminar(yo, mia).andExpect(status().isOk());
        responder(yo, problema, "ok", mia).andExpect(status().isConflict());
    }

    // ---------- terminar la sesión: guardar el nivel (HU54-T3) y el resumen (HU56) ----------

    @Test
    void al_terminar_se_guarda_el_nivel_y_la_siguiente_sesion_parte_de_el() throws Exception {
        String yo = alumno();
        publicar(e(1, "facil", "Bucles"), e(2, "facil", "Listas"), e(3, "medio", "Bucles"));
        long sesion = sesion(yo);
        responder(yo, siguiente(yo, sesion), "ok", sesion);
        responder(yo, siguiente(yo, sesion), "ok", sesion);

        terminar(yo, sesion).andExpect(status().isOk()).andExpect(jsonPath("$.nivel_alcanzado").value("medio"));

        assertThat(jdbc.sql("SELECT nivel FROM app_schema.app_nivel_dificultad WHERE estudiante_id = ? AND lenguaje = 'python'")
                .param(idEstudiante(yo)).query(String.class).single()).isEqualTo("medio");
        assertThat(jdbc.sql("SELECT fecha_fin IS NOT NULL FROM app_schema.app_sesion_dojo WHERE id = ?").param(sesion)
                .query(Boolean.class).single()).isTrue();
        iniciar(yo).andExpect(status().isCreated()).andExpect(jsonPath("$.nivel").value("medio"));
    }

    @Test
    void al_terminar_otra_vez_el_nivel_guardado_se_actualiza_y_no_se_duplica() throws Exception {
        String yo = alumno();
        guardarNivel(yo, "dificil");
        publicar(e(5, "dificil", "Bucles"), e(6, "dificil", "Listas"), e(3, "medio", "Bucles"));
        long sesion = sesion(yo);
        responder(yo, siguiente(yo, sesion), "no", sesion);
        responder(yo, siguiente(yo, sesion), "no", sesion);

        terminar(yo, sesion).andExpect(status().isOk()).andExpect(jsonPath("$.nivel_alcanzado").value("medio"));

        assertThat(jdbc.sql("SELECT count(*) FROM app_schema.app_nivel_dificultad WHERE estudiante_id = ?")
                .param(idEstudiante(yo)).query(Integer.class).single()).isEqualTo(1);
        assertThat(jdbc.sql("SELECT nivel FROM app_schema.app_nivel_dificultad WHERE estudiante_id = ?")
                .param(idEstudiante(yo)).query(String.class).single()).isEqualTo("medio");
    }

    @Test
    void el_resumen_trae_aciertos_puntos_del_cinturon_y_los_temas_donde_mas_fallo() throws Exception {
        String yo = alumno();
        publicar(e(1, "facil", "Bucles"), e(2, "facil", "Listas"), e(3, "facil", "Listas"), e(4, "facil", ""));
        long sesion = sesion(yo);
        for (int i = 0; i < 4; i++) {
            long problema = siguiente(yo, sesion);
            // El 1 es el único acierto; el 4 (sin tema) y los dos de Listas fallan.
            responder(yo, problema, problema == 1 ? "ok" : "no", sesion).andExpect(status().isOk());
        }

        terminar(yo, sesion).andExpect(status().isOk())
                .andExpect(jsonPath("$.respondidos").value(4))
                .andExpect(jsonPath("$.aciertos").value(1))
                .andExpect(jsonPath("$.puntos_sumados").value(10))
                .andExpect(jsonPath("$.temas_mas_fallados.length()").value(2))
                .andExpect(jsonPath("$.temas_mas_fallados[0].tema").value("Listas"))
                .andExpect(jsonPath("$.temas_mas_fallados[0].fallos").value(2))
                .andExpect(jsonPath("$.temas_mas_fallados[1].tema").value("General"))
                .andExpect(jsonPath("$.temas_mas_fallados[1].fallos").value(1))
                .andExpect(jsonPath("$.cinturon.nivel").value(1))
                .andExpect(jsonPath("$.cinturon.puntos").value(10));
    }

    @Test
    void el_resumen_de_una_sesion_sin_fallos_no_trae_temas_y_sin_puntos_no_hay_cinturon() throws Exception {
        String yo = alumno();
        publicar(e(1, "facil", "Bucles"));

        terminar(yo, sesion(yo)).andExpect(status().isOk())
                .andExpect(jsonPath("$.respondidos").value(0))
                .andExpect(jsonPath("$.puntos_sumados").value(0))
                .andExpect(jsonPath("$.temas_mas_fallados.length()").value(0))
                .andExpect(jsonPath("$.cinturon").doesNotExist());
    }

    @Test
    void los_puntos_de_un_ejercicio_ya_resuelto_antes_no_se_cuentan_en_la_sesion() throws Exception {
        String yo = alumno();
        publicar(e(1, "facil", "Bucles"));
        // Lo resolvió antes, fuera de toda sesión: el cinturón ya recibió sus puntos.
        responder(yo, 1, "ok", null).andExpect(status().isOk());
        jdbc.sql("UPDATE app_schema.app_puntos_ejercicio SET fecha = now() - interval '1 day'").update();
        long sesion = sesion(yo);
        responder(yo, siguiente(yo, sesion), "ok", sesion).andExpect(status().isOk());

        terminar(yo, sesion).andExpect(status().isOk())
                .andExpect(jsonPath("$.aciertos").value(1))
                .andExpect(jsonPath("$.puntos_sumados").value(0))
                .andExpect(jsonPath("$.cinturon.puntos").value(10));
    }

    @Test
    void terminar_dos_veces_da_409_y_la_sesion_ajena_o_inexistente_da_404() throws Exception {
        String yo = alumno();
        long sesion = sesion(yo);

        terminar(yo, sesion).andExpect(status().isOk());
        terminar(yo, sesion).andExpect(status().isConflict());
        terminar(alumno(), sesion).andExpect(status().isNotFound());
        terminar(yo, 999999).andExpect(status().isNotFound());
    }

    @Test
    void una_sesion_terminada_ya_no_entrega_ejercicios() throws Exception {
        String yo = alumno();
        publicar(e(1, "facil", "Bucles"));
        long sesion = sesion(yo);
        terminar(yo, sesion).andExpect(status().isOk());

        mvc.perform(post("/api/dojo/sesiones/" + sesion + "/siguiente").with(tokenDeCliente(yo, "mobile")))
                .andExpect(status().isConflict());
    }

    @Test
    void sin_token_no_se_puede_terminar_una_sesion() throws Exception {
        mvc.perform(post("/api/dojo/sesiones/1/terminar")).andExpect(status().isUnauthorized());
    }
}
