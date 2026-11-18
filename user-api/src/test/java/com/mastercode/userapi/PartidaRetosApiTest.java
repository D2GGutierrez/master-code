package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.mastercode.userapi.admin.AdminNoDisponibleException;

// HU57-T2, HU58-T1 y H7 (ADR-15): retar a un compañero, guardar la secuencia de ejercicios, y aceptar, rechazar y listar
// las invitaciones.
class PartidaRetosApiTest extends PartidaApiBase {

    // ---------- crear el reto (HU57-T2, HU58-T1) ----------

    @Test
    void retar_crea_una_partida_pendiente_con_los_dos_jugadores() throws Exception {
        publicarTres();
        String yo = alumno();
        String rival = alumno();

        retar(yo, CURSO, idDe(rival)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("pendiente"))
                .andExpect(jsonPath("$.curso_id").value(CURSO))
                .andExpect(jsonPath("$.total_ejercicios").value(3))
                .andExpect(jsonPath("$.rival").value(rival.substring(0, rival.indexOf('@'))));

        assertThat(cuantasPartidas()).isEqualTo(1);
        assertThat(jdbc.sql("SELECT estudiante_id FROM app_schema.app_partida_jugador WHERE es_retador").query(Long.class)
                .single()).isEqualTo(idDe(yo));
        assertThat(jdbc.sql("SELECT count(*) FROM app_schema.app_partida_jugador").query(Integer.class).single()).isEqualTo(2);
    }

    @Test
    void la_respuesta_no_trae_el_correo_del_rival() throws Exception {
        publicarTres();
        String yo = alumno();
        String rival = alumno();

        retar(yo, CURSO, idDe(rival)).andExpect(content().string(not(containsString("@"))));
    }

    @Test
    void la_secuencia_se_guarda_al_crear_con_turnos_seguidos_y_sin_repetir() throws Exception {
        publicarTres();
        long partida = reto(alumno(), alumno());

        List<Long> turnos = jdbc.sql("SELECT turno FROM app_schema.app_partida_ejercicio WHERE partida_id = ? ORDER BY turno")
                .param(partida).query(Long.class).list();
        List<Long> problemas = jdbc.sql("SELECT problema_id FROM app_schema.app_partida_ejercicio WHERE partida_id = ?")
                .param(partida).query(Long.class).list();
        assertThat(turnos).containsExactly(1L, 2L, 3L);
        assertThat(problemas).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void la_secuencia_tiene_como_maximo_cinco_ejercicios() throws Exception {
        publicar(prediccion(1, "facil", "a", BIEN), prediccion(2, "facil", "a", BIEN), prediccion(3, "facil", "a", BIEN),
                prediccion(4, "facil", "a", BIEN), prediccion(5, "facil", "a", BIEN), prediccion(6, "facil", "a", BIEN),
                prediccion(7, "facil", "a", BIEN));

        long partida = reto(alumno(), alumno());

        assertThat(totalDeEjercicios(partida)).isEqualTo(5);
    }

    @Test
    void un_curso_con_menos_de_cinco_ejercicios_da_una_secuencia_mas_corta() throws Exception {
        publicar(prediccion(1, "facil", "a", BIEN), prediccion(2, "facil", "a", BIEN));

        assertThat(totalDeEjercicios(reto(alumno(), alumno()))).isEqualTo(2);
    }

    @Test
    void los_ejercicios_de_codigo_no_entran_en_la_secuencia() throws Exception {
        publicar(prediccion(1, "facil", "a", BIEN), new Ejercicio(9, "escritura_codigo", "facil", "a", "", List.of()));

        long partida = reto(alumno(), alumno());

        assertThat(totalDeEjercicios(partida)).isEqualTo(1);
        assertThat(problemaDelTurno(partida, 1)).isEqualTo(1);
    }

    @Test
    void un_curso_sin_ejercicios_del_celular_no_permite_retar() throws Exception {
        publicar(new Ejercicio(9, "escritura_codigo", "facil", "a", "", List.of()));
        String yo = alumno();
        String rival = alumno();

        retar(yo, CURSO, idDe(rival)).andExpect(status().isConflict());

        assertThat(cuantasPartidas()).isZero();
    }

    @Test
    void si_admin_api_no_responde_no_se_crea_nada() throws Exception {
        when(admin.problemasDelCurso(CURSO, "movil")).thenThrow(new AdminNoDisponibleException("caído", null));
        String yo = alumno();
        String rival = alumno();

        retar(yo, CURSO, idDe(rival)).andExpect(status().isServiceUnavailable());

        assertThat(cuantasPartidas()).isZero();
    }

    @Test
    void quien_no_esta_inscrito_en_el_curso_no_puede_retar() throws Exception {
        publicarTres();
        String fuera = correoUnico();
        crearEstudiante(fuera);

        retar(fuera, CURSO, idDe(alumno())).andExpect(status().isForbidden());

        assertThat(cuantasPartidas()).isZero();
    }

    @Test
    void no_se_puede_retar_a_alguien_que_no_esta_en_el_curso() throws Exception {
        publicarTres();
        String yo = alumno();
        String ajeno = correoUnico();
        crearEstudiante(ajeno);

        retar(yo, CURSO, idDe(ajeno)).andExpect(status().isBadRequest());

        assertThat(cuantasPartidas()).isZero();
    }

    @Test
    void no_se_puede_retar_a_uno_mismo() throws Exception {
        publicarTres();
        String yo = alumno();

        retar(yo, CURSO, idDe(yo)).andExpect(status().isBadRequest());
    }

    @Test
    void faltan_el_curso_o_el_rival() throws Exception {
        publicarTres();
        String yo = alumno();

        retar(yo, null, idDe(alumno())).andExpect(status().isBadRequest());
        retar(yo, CURSO, null).andExpect(status().isBadRequest());
    }

    @Test
    void un_rival_que_no_existe_se_rechaza() throws Exception {
        publicarTres();

        retar(alumno(), CURSO, 999_999_999L).andExpect(status().isBadRequest());
    }

    // ---------- un solo reto vivo por pareja ----------

    @Test
    void no_se_puede_retar_dos_veces_al_mismo_companero_mientras_el_reto_sigue_vivo() throws Exception {
        publicarTres();
        String yo = alumno();
        String rival = alumno();
        reto(yo, rival);

        retar(yo, CURSO, idDe(rival)).andExpect(status().isConflict());
        // Ni él a mí: el reto vale en los dos sentidos.
        retar(rival, CURSO, idDe(yo)).andExpect(status().isConflict());

        assertThat(cuantasPartidas()).isEqualTo(1);
    }

    @Test
    void tampoco_si_la_partida_ya_esta_en_curso() throws Exception {
        publicarTres();
        String yo = alumno();
        String rival = alumno();
        partida(yo, rival);

        retar(yo, CURSO, idDe(rival)).andExpect(status().isConflict());
    }

    @Test
    void despues_de_rechazar_se_puede_retar_otra_vez() throws Exception {
        publicarTres();
        String yo = alumno();
        String rival = alumno();
        rechazar(rival, reto(yo, rival)).andExpect(status().isNoContent());

        retar(yo, CURSO, idDe(rival)).andExpect(status().isCreated());
    }

    @Test
    void un_reto_vencido_deja_retar_de_nuevo() throws Exception {
        publicarTres();
        String yo = alumno();
        String rival = alumno();
        creadaHaceHoras(reto(yo, rival), 25);

        retar(yo, CURSO, idDe(rival)).andExpect(status().isCreated());
    }

    @Test
    void retar_a_otros_companeros_a_la_vez_si_se_puede() throws Exception {
        publicarTres();
        String yo = alumno();
        reto(yo, alumno());

        retar(yo, CURSO, idDe(alumno())).andExpect(status().isCreated());
    }

    // ---------- los compañeros (HU57-T3) ----------

    @Test
    void los_companeros_son_los_demas_inscritos_solo_con_alias() throws Exception {
        String yo = alumno();
        String otro = alumno();

        mvc.perform(get("/api/cursos/" + CURSO + "/companeros").with(tokenDeCliente(yo, "mobile")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + idDe(otro) + ")].alias").value(otro.substring(0, otro.indexOf('@'))))
                .andExpect(jsonPath("$[?(@.id == " + idDe(yo) + ")]").isEmpty())
                .andExpect(content().string(not(containsString("@"))));
    }

    @Test
    void los_companeros_solo_los_ve_un_inscrito() throws Exception {
        String fuera = correoUnico();
        crearEstudiante(fuera);

        mvc.perform(get("/api/cursos/" + CURSO + "/companeros").with(tokenDeCliente(fuera, "mobile")))
                .andExpect(status().isForbidden());
    }

    // ---------- invitaciones (H7) ----------

    @Test
    void el_invitado_ve_el_reto_con_el_alias_del_retador_y_cuando_vence() throws Exception {
        publicarTres();
        String yo = alumno();
        String rival = alumno();
        long partida = reto(yo, rival);

        mvc.perform(get("/api/partidas/invitaciones").with(tokenDeCliente(rival, "mobile")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(partida))
                .andExpect(jsonPath("$[0].curso_id").value(CURSO))
                .andExpect(jsonPath("$[0].retador").value(yo.substring(0, yo.indexOf('@'))))
                .andExpect(jsonPath("$[0].vence_en").exists())
                .andExpect(content().string(not(containsString("@"))));
    }

    @Test
    void el_retador_no_ve_su_propio_reto_entre_las_invitaciones() throws Exception {
        publicarTres();
        String yo = alumno();
        reto(yo, alumno());

        mvc.perform(get("/api/partidas/invitaciones").with(tokenDeCliente(yo, "mobile")))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void las_invitaciones_salen_la_mas_reciente_primero() throws Exception {
        publicarTres();
        String rival = alumno();
        long vieja = reto(alumno(), rival);
        long nueva = reto(alumno(), rival);
        creadaHaceHoras(vieja, 2);

        mvc.perform(get("/api/partidas/invitaciones").with(tokenDeCliente(rival, "mobile")))
                .andExpect(jsonPath("$[0].id").value(nueva)).andExpect(jsonPath("$[1].id").value(vieja));
    }

    @Test
    void una_invitacion_vencida_ya_no_sale() throws Exception {
        publicarTres();
        String rival = alumno();
        creadaHaceHoras(reto(alumno(), rival), 25);

        mvc.perform(get("/api/partidas/invitaciones").with(tokenDeCliente(rival, "mobile")))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void una_invitacion_aceptada_o_rechazada_ya_no_sale() throws Exception {
        publicarTres();
        String rival = alumno();
        aceptar(rival, reto(alumno(), rival)).andExpect(status().isOk());
        rechazar(rival, reto(alumno(), rival)).andExpect(status().isNoContent());

        mvc.perform(get("/api/partidas/invitaciones").with(tokenDeCliente(rival, "mobile")))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ---------- aceptar y rechazar (H7) ----------

    @Test
    void aceptar_pone_la_partida_en_curso_y_a_los_dos_con_senal() throws Exception {
        publicarTres();
        String yo = alumno();
        String rival = alumno();
        long partida = reto(yo, rival);

        aceptar(rival, partida).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("en_curso"))
                .andExpect(jsonPath("$.total_ejercicios").value(3))
                .andExpect(jsonPath("$.rival_conectado").value(true));

        assertThat(estado(partida)).isEqualTo("en_curso");
        assertThat(jdbc.sql("SELECT fecha_inicio FROM app_schema.app_partida WHERE id = ?").param(partida)
                .query(java.sql.Timestamp.class).single()).isNotNull();
        assertThat(jdbc.sql("SELECT count(*) FROM app_schema.app_partida_jugador WHERE partida_id = ? AND ultima_senal IS NOT NULL")
                .param(partida).query(Integer.class).single()).isEqualTo(2);
    }

    @Test
    void el_retador_no_puede_aceptar_su_propio_reto() throws Exception {
        publicarTres();
        String yo = alumno();
        long partida = reto(yo, alumno());

        aceptar(yo, partida).andExpect(status().isForbidden());

        assertThat(estado(partida)).isEqualTo("pendiente");
    }

    @Test
    void un_tercero_no_puede_aceptar_ni_rechazar_ni_ve_que_existe() throws Exception {
        publicarTres();
        long partida = reto(alumno(), alumno());
        String tercero = alumno();

        aceptar(tercero, partida).andExpect(status().isNotFound());
        rechazar(tercero, partida).andExpect(status().isNotFound());

        assertThat(estado(partida)).isEqualTo("pendiente");
    }

    @Test
    void una_partida_que_no_existe_da_404() throws Exception {
        aceptar(alumno(), 999_999_999L).andExpect(status().isNotFound());
    }

    @Test
    void no_se_acepta_dos_veces() throws Exception {
        publicarTres();
        String rival = alumno();
        long partida = reto(alumno(), rival);
        aceptar(rival, partida).andExpect(status().isOk());

        aceptar(rival, partida).andExpect(status().isConflict());
    }

    @Test
    void un_reto_vencido_no_se_puede_aceptar_ni_rechazar() throws Exception {
        publicarTres();
        String rival = alumno();
        long partida = reto(alumno(), rival);
        creadaHaceHoras(partida, 25);

        aceptar(rival, partida).andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value(containsString("caducó")));
        rechazar(rival, partida).andExpect(status().isConflict());

        assertThat(estado(partida)).isEqualTo("pendiente");
    }

    @Test
    void un_reto_de_casi_un_dia_todavia_se_puede_aceptar() throws Exception {
        publicarTres();
        String rival = alumno();
        long partida = reto(alumno(), rival);
        creadaHaceHoras(partida, 23);

        aceptar(rival, partida).andExpect(status().isOk());
    }

    @Test
    void rechazar_deja_la_partida_rechazada_con_fecha_de_fin() throws Exception {
        publicarTres();
        String rival = alumno();
        long partida = reto(alumno(), rival);

        rechazar(rival, partida).andExpect(status().isNoContent());

        assertThat(estado(partida)).isEqualTo("rechazada");
        assertThat(jdbc.sql("SELECT fecha_fin FROM app_schema.app_partida WHERE id = ?").param(partida)
                .query(java.sql.Timestamp.class).single()).isNotNull();
        rechazar(rival, partida).andExpect(status().isConflict());
        aceptar(rival, partida).andExpect(status().isConflict());
    }

    @Test
    void el_retador_no_puede_rechazar_su_propio_reto() throws Exception {
        publicarTres();
        String yo = alumno();

        rechazar(yo, reto(yo, alumno())).andExpect(status().isForbidden());
    }

    @Test
    void sin_token_de_sesion_las_rutas_de_partidas_responden_401() throws Exception {
        mvc.perform(get("/api/partidas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/partidas/invitaciones")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/cursos/" + CURSO + "/ranking")).andExpect(status().isUnauthorized());
    }
}
