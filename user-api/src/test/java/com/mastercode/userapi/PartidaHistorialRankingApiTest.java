package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

// HU60-T1 y HU60-T2 (ADR-15): el historial de partidas del estudiante y el ranking del curso por partidas ganadas. Las
// partidas terminadas se siembran directo en la base: lo que se prueba aquí es cómo se cuentan y se ordenan.
class PartidaHistorialRankingApiTest extends PartidaApiBase {

    // El ranking usa su propio curso: ninguna otra prueba inscribe gente en él.
    private static final long CURSO_RANKING = 20;
    private static final long OTRO_CURSO = 21;

    private long inscrito(long curso) {
        long id = crearEstudiante(correoUnico());
        inscribir(id, curso);
        return id;
    }

    private String correoDe(long id) {
        return jdbc.sql("SELECT email FROM app_schema.app_estudiante WHERE id = ?").param(id).query(String.class).single();
    }

    private static String alias(String email) {
        return email.substring(0, email.indexOf('@'));
    }

    // Una partida terminada entre a (retador) y b. ganador nulo es empate.
    private long terminada(long curso, long a, long b, int puntajeA, int puntajeB, Long ganador, String motivo,
            OffsetDateTime fin) {
        long id = jdbc.sql("""
                INSERT INTO app_schema.app_partida (curso_id, estado, fecha_inicio, fecha_fin, ganador_id, motivo_fin)
                VALUES (?, 'terminada', ?, ?, ?, ?) RETURNING id
                """).params(curso, fin.minusMinutes(5), fin, ganador, motivo).query(Long.class).single();
        jdbc.sql("INSERT INTO app_schema.app_partida_jugador (partida_id, estudiante_id, es_retador, puntaje) VALUES (?, ?, TRUE, ?)")
                .params(id, a, puntajeA).update();
        jdbc.sql("INSERT INTO app_schema.app_partida_jugador (partida_id, estudiante_id, es_retador, puntaje) VALUES (?, ?, FALSE, ?)")
                .params(id, b, puntajeB).update();
        return id;
    }

    private long gana(long curso, long ganador, long perdedor, int puntosGanador, int puntosPerdedor) {
        return terminada(curso, ganador, perdedor, puntosGanador, puntosPerdedor, ganador, "completada", OffsetDateTime.now());
    }

    private long empatan(long curso, long a, long b, int puntos) {
        return terminada(curso, a, b, puntos, puntos, null, "completada", OffsetDateTime.now());
    }

    private ResultActions historial(String email, String consulta) throws Exception {
        return mvc.perform(get("/api/partidas" + consulta).with(tokenDeCliente(email, "mobile")));
    }

    private ResultActions ranking(String email, long curso, String consulta) throws Exception {
        return mvc.perform(get("/api/cursos/" + curso + "/ranking" + consulta).with(tokenDeCliente(email, "mobile")));
    }

    // ---------- historial (HU60-T1) ----------

    @Test
    void sin_partidas_el_historial_viene_vacio() throws Exception {
        historial(alumno(), "").andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void el_historial_muestra_resultado_rival_y_puntajes_de_cada_partida() throws Exception {
        long yo = inscrito(CURSO_RANKING);
        long rival = inscrito(CURSO_RANKING);
        long victoria = gana(CURSO_RANKING, yo, rival, 40, 20);

        historial(correoDe(yo), "").andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(victoria))
                .andExpect(jsonPath("$.items[0].curso_id").value(CURSO_RANKING))
                .andExpect(jsonPath("$.items[0].estado").value("terminada"))
                .andExpect(jsonPath("$.items[0].resultado").value("gane"))
                .andExpect(jsonPath("$.items[0].rival").value(alias(correoDe(rival))))
                .andExpect(jsonPath("$.items[0].mi_puntaje").value(40))
                .andExpect(jsonPath("$.items[0].puntaje_rival").value(20))
                .andExpect(jsonPath("$.items[0].motivo_fin").value("completada"))
                .andExpect(content().string(not(containsString("@"))));
        // El rival ve la misma partida al revés.
        historial(correoDe(rival), "").andExpect(jsonPath("$.items[0].resultado").value("perdi"))
                .andExpect(jsonPath("$.items[0].mi_puntaje").value(20)).andExpect(jsonPath("$.items[0].puntaje_rival").value(40));
    }

    @Test
    void un_empate_sale_como_empate_y_un_abandono_con_su_motivo() throws Exception {
        long yo = inscrito(CURSO_RANKING);
        long rival = inscrito(CURSO_RANKING);
        empatan(CURSO_RANKING, yo, rival, 20);
        terminada(CURSO_RANKING, yo, rival, 10, 0, rival, "abandono", OffsetDateTime.now().minusHours(1));

        historial(correoDe(yo), "").andExpect(jsonPath("$.items[0].resultado").value("empate"))
                .andExpect(jsonPath("$.items[1].resultado").value("perdi"))
                .andExpect(jsonPath("$.items[1].motivo_fin").value("abandono"));
    }

    @Test
    void el_historial_va_de_la_mas_reciente_a_la_mas_antigua() throws Exception {
        long yo = inscrito(CURSO_RANKING);
        long rival = inscrito(CURSO_RANKING);
        long vieja = terminada(CURSO_RANKING, yo, rival, 10, 0, yo, "completada", OffsetDateTime.now().minusDays(3));
        long nueva = terminada(CURSO_RANKING, yo, rival, 10, 0, yo, "completada", OffsetDateTime.now().minusMinutes(1));
        long media = terminada(CURSO_RANKING, rival, yo, 10, 0, rival, "completada", OffsetDateTime.now().minusDays(1));

        historial(correoDe(yo), "").andExpect(jsonPath("$.items[0].id").value(nueva))
                .andExpect(jsonPath("$.items[1].id").value(media)).andExpect(jsonPath("$.items[2].id").value(vieja));
    }

    @Test
    void solo_salen_las_partidas_en_curso_y_terminadas() throws Exception {
        publicarTres();
        String yo = alumno();
        String a = alumno();
        String b = alumno();
        String c = alumno();
        reto(yo, a);
        rechazar(c, reto(yo, c)).andExpect(status().isNoContent());
        creadaHaceHoras(reto(b, yo), 30);
        long enCurso = partida(yo, alumno());

        historial(yo, "").andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").value(enCurso)).andExpect(jsonPath("$.items[0].estado").value("en_curso"))
                .andExpect(jsonPath("$.items[0].resultado").doesNotExist());
    }

    @Test
    void no_salen_las_partidas_de_otros() throws Exception {
        long a = inscrito(CURSO_RANKING);
        long b = inscrito(CURSO_RANKING);
        gana(CURSO_RANKING, a, b, 10, 0);

        historial(alumno(), "").andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void el_historial_se_pagina() throws Exception {
        long yo = inscrito(CURSO_RANKING);
        long rival = inscrito(CURSO_RANKING);
        for (int i = 0; i < 5; i++) {
            terminada(CURSO_RANKING, yo, rival, 10, 0, yo, "completada", OffsetDateTime.now().minusMinutes(i));
        }

        historial(correoDe(yo), "?pagina=1&tamano=2").andExpect(jsonPath("$.items", hasSize(2))).andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.pagina").value(1)).andExpect(jsonPath("$.tamano").value(2));
        historial(correoDe(yo), "?pagina=3&tamano=2").andExpect(jsonPath("$.items", hasSize(1)));
        historial(correoDe(yo), "?pagina=4&tamano=2").andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void un_tamano_o_una_pagina_fuera_de_rango_se_ajustan() throws Exception {
        long yo = inscrito(CURSO_RANKING);

        historial(correoDe(yo), "?pagina=0&tamano=500").andExpect(status().isOk()).andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.tamano").value(50));
        historial(correoDe(yo), "?tamano=0").andExpect(jsonPath("$.tamano").value(1));
    }

    // ---------- ranking (HU60-T2) ----------

    @Test
    void el_ranking_ordena_por_partidas_ganadas() throws Exception {
        long a = inscrito(CURSO_RANKING);
        long b = inscrito(CURSO_RANKING);
        long c = inscrito(CURSO_RANKING);
        gana(CURSO_RANKING, b, a, 10, 0);
        gana(CURSO_RANKING, b, c, 10, 0);
        gana(CURSO_RANKING, c, a, 10, 0);

        ranking(correoDe(a), CURSO_RANKING, "").andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items[0].alias").value(alias(correoDe(b))))
                .andExpect(jsonPath("$.items[0].posicion").value(1))
                .andExpect(jsonPath("$.items[0].ganadas").value(2))
                .andExpect(jsonPath("$.items[1].alias").value(alias(correoDe(c))))
                .andExpect(jsonPath("$.items[2].alias").value(alias(correoDe(a))))
                .andExpect(jsonPath("$.items[2].posicion").value(3))
                .andExpect(content().string(not(containsString("@"))));
    }

    @Test
    void con_las_mismas_ganadas_gana_el_mayor_porcentaje_de_victorias() throws Exception {
        long a = inscrito(CURSO_RANKING);
        long b = inscrito(CURSO_RANKING);
        long x = inscrito(CURSO_RANKING);
        long y = inscrito(CURSO_RANKING);
        // a: 2 ganadas de 2. b: 2 ganadas de 3 (perdió una contra x).
        gana(CURSO_RANKING, a, x, 10, 0);
        gana(CURSO_RANKING, a, y, 10, 0);
        gana(CURSO_RANKING, b, x, 10, 0);
        gana(CURSO_RANKING, b, y, 10, 0);
        gana(CURSO_RANKING, x, b, 10, 0);

        ranking(correoDe(a), CURSO_RANKING, "")
                .andExpect(jsonPath("$.items[0].alias").value(alias(correoDe(a))))
                .andExpect(jsonPath("$.items[0].porcentaje_victorias").value(100.0))
                .andExpect(jsonPath("$.items[1].alias").value(alias(correoDe(b))))
                .andExpect(jsonPath("$.items[1].jugadas").value(3))
                .andExpect(jsonPath("$.items[1].porcentaje_victorias").value(66.7));
    }

    @Test
    void con_las_mismas_ganadas_y_porcentaje_gana_el_de_mas_puntos() throws Exception {
        long a = inscrito(CURSO_RANKING);
        long b = inscrito(CURSO_RANKING);
        long x = inscrito(CURSO_RANKING);
        long y = inscrito(CURSO_RANKING);
        gana(CURSO_RANKING, a, x, 20, 0);
        gana(CURSO_RANKING, b, y, 50, 0);

        ranking(correoDe(a), CURSO_RANKING, "")
                .andExpect(jsonPath("$.items[0].alias").value(alias(correoDe(b))))
                .andExpect(jsonPath("$.items[0].puntos").value(50))
                .andExpect(jsonPath("$.items[1].alias").value(alias(correoDe(a))));
    }

    @Test
    void en_un_empate_total_el_orden_es_siempre_el_mismo_por_id() throws Exception {
        long a = inscrito(CURSO_RANKING);
        long b = inscrito(CURSO_RANKING);
        long x = inscrito(CURSO_RANKING);
        long y = inscrito(CURSO_RANKING);
        gana(CURSO_RANKING, a, x, 30, 0);
        gana(CURSO_RANKING, b, y, 30, 0);

        for (int veces = 0; veces < 2; veces++) {
            ranking(correoDe(x), CURSO_RANKING, "")
                    .andExpect(jsonPath("$.items[0].alias").value(alias(correoDe(a))))
                    .andExpect(jsonPath("$.items[1].alias").value(alias(correoDe(b))));
        }
    }

    @Test
    void los_inscritos_sin_partidas_salen_al_final_con_ceros() throws Exception {
        long a = inscrito(CURSO_RANKING);
        long b = inscrito(CURSO_RANKING);
        long sinJugar = inscrito(CURSO_RANKING);
        gana(CURSO_RANKING, a, b, 10, 0);

        ranking(correoDe(a), CURSO_RANKING, "").andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items[0].alias").value(alias(correoDe(a))))
                .andExpect(jsonPath("$.items[2].alias").value(alias(correoDe(sinJugar))))
                .andExpect(jsonPath("$.items[2].jugadas").value(0))
                .andExpect(jsonPath("$.items[2].ganadas").value(0))
                .andExpect(jsonPath("$.items[2].puntos").value(0))
                .andExpect(jsonPath("$.items[2].porcentaje_victorias").value(0.0));
    }

    @Test
    void un_empate_cuenta_como_jugada_pero_no_como_ganada_ni_perdida() throws Exception {
        long a = inscrito(CURSO_RANKING);
        long b = inscrito(CURSO_RANKING);
        empatan(CURSO_RANKING, a, b, 20);

        ranking(correoDe(a), CURSO_RANKING, "")
                .andExpect(jsonPath("$.yo.jugadas").value(1)).andExpect(jsonPath("$.yo.ganadas").value(0))
                .andExpect(jsonPath("$.yo.empatadas").value(1)).andExpect(jsonPath("$.yo.perdidas").value(0))
                .andExpect(jsonPath("$.yo.puntos").value(20));
    }

    @Test
    void un_abandono_es_una_victoria_del_que_se_quedo_y_una_derrota_del_otro() throws Exception {
        long a = inscrito(CURSO_RANKING);
        long b = inscrito(CURSO_RANKING);
        terminada(CURSO_RANKING, a, b, 0, 10, a, "abandono", OffsetDateTime.now());

        ranking(correoDe(a), CURSO_RANKING, "").andExpect(jsonPath("$.yo.ganadas").value(1))
                .andExpect(jsonPath("$.items[0].alias").value(alias(correoDe(a))))
                .andExpect(jsonPath("$.items[1].perdidas").value(1));
    }

    @Test
    void las_victorias_se_cuentan_gane_quien_gane_siendo_retador_o_invitado() throws Exception {
        long a = inscrito(CURSO_RANKING);
        long b = inscrito(CURSO_RANKING);
        terminada(CURSO_RANKING, a, b, 0, 10, b, "completada", OffsetDateTime.now());

        ranking(correoDe(a), CURSO_RANKING, "").andExpect(jsonPath("$.items[0].alias").value(alias(correoDe(b))))
                .andExpect(jsonPath("$.items[0].ganadas").value(1));
    }

    @Test
    void no_cuentan_las_partidas_de_otro_curso_ni_las_que_no_terminaron() throws Exception {
        publicarTres();
        long a = inscrito(CURSO_RANKING);
        long b = inscrito(CURSO_RANKING);
        inscribir(a, OTRO_CURSO);
        inscribir(b, OTRO_CURSO);
        gana(OTRO_CURSO, a, b, 100, 0);
        // En curso y pendiente en el mismo curso: tampoco.
        long enCurso = jdbc.sql("INSERT INTO app_schema.app_partida (curso_id, estado, fecha_inicio) VALUES (?, 'en_curso', now()) RETURNING id")
                .param(CURSO_RANKING).query(Long.class).single();
        jdbc.sql("INSERT INTO app_schema.app_partida_jugador (partida_id, estudiante_id, es_retador, puntaje) VALUES (?, ?, TRUE, 90)")
                .params(enCurso, a).update();
        jdbc.sql("INSERT INTO app_schema.app_partida_jugador (partida_id, estudiante_id, es_retador) VALUES (?, ?, FALSE)")
                .params(enCurso, b).update();

        ranking(correoDe(a), CURSO_RANKING, "").andExpect(jsonPath("$.yo.jugadas").value(0)).andExpect(jsonPath("$.yo.ganadas").value(0))
                .andExpect(jsonPath("$.yo.puntos").value(0));
        ranking(correoDe(a), OTRO_CURSO, "").andExpect(jsonPath("$.yo.ganadas").value(1)).andExpect(jsonPath("$.yo.puntos").value(100));
    }

    @Test
    void solo_un_inscrito_ve_el_ranking() throws Exception {
        String fuera = correoUnico();
        crearEstudiante(fuera);

        ranking(fuera, CURSO_RANKING, "").andExpect(status().isForbidden());
    }

    @Test
    void el_ranking_siempre_trae_la_entrada_propia_aunque_este_en_otra_pagina() throws Exception {
        long a = inscrito(CURSO_RANKING);
        long b = inscrito(CURSO_RANKING);
        long c = inscrito(CURSO_RANKING);
        gana(CURSO_RANKING, a, c, 10, 0);
        gana(CURSO_RANKING, b, c, 10, 0);

        // c queda último: en la segunda página de a lo más.
        ranking(correoDe(c), CURSO_RANKING, "?pagina=1&tamano=2").andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[?(@.soy_yo == true)]").doesNotExist())
                .andExpect(jsonPath("$.yo.alias").value(alias(correoDe(c)))).andExpect(jsonPath("$.yo.posicion").value(3))
                .andExpect(jsonPath("$.yo.soy_yo").value(true));
        ranking(correoDe(c), CURSO_RANKING, "?pagina=2&tamano=2").andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].soy_yo").value(true));
    }

    @Test
    void la_posicion_es_la_del_orden_completo_y_no_la_de_la_pagina() throws Exception {
        long a = inscrito(CURSO_RANKING);
        long b = inscrito(CURSO_RANKING);
        long c = inscrito(CURSO_RANKING);
        gana(CURSO_RANKING, a, c, 10, 0);
        gana(CURSO_RANKING, b, c, 10, 0);

        ranking(correoDe(a), CURSO_RANKING, "?pagina=2&tamano=2").andExpect(jsonPath("$.items[0].posicion").value(3));
    }

    @Test
    void un_ranking_de_un_curso_sin_inscritos_ajenos_no_incluye_a_otros_cursos() throws Exception {
        long a = inscrito(CURSO_RANKING);
        inscrito(OTRO_CURSO);

        ranking(correoDe(a), CURSO_RANKING, "").andExpect(jsonPath("$.total").value(1));
        assertThat(a).isPositive();
    }
}
