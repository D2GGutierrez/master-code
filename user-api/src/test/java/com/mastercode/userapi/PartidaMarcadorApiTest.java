package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

// HU59-T2 y HU59-T4 (ADR-10 D20): el marcador se sondea cada 2 s con ETag; cada consulta deja la señal del jugador; a los
// 10 s sin señal el rival ve «se desconectó» y a los 30 s la partida se cierra por abandono.
class PartidaMarcadorApiTest extends PartidaApiBase {

    private String yo;
    private String rival;
    private long partida;

    private void empezar() throws Exception {
        publicarTres();
        yo = alumno();
        rival = alumno();
        partida = partida(yo, rival);
    }

    private String etagDe(String email) throws Exception {
        return marcador(email, partida).andExpect(status().isOk()).andReturn().getResponse().getHeader("ETag");
    }

    // ---------- el contenido (HU59-T2) ----------

    @Test
    void el_marcador_trae_a_los_dos_jugadores_el_progreso_y_el_estado() throws Exception {
        empezar();
        contestar(yo, partida, 1, BIEN).andExpect(status().isOk());

        marcador(yo, partida).andExpect(status().isOk())
                .andExpect(jsonPath("$.partida_id").value(partida))
                .andExpect(jsonPath("$.curso_id").value(CURSO))
                .andExpect(jsonPath("$.estado").value("en_curso"))
                .andExpect(jsonPath("$.total_ejercicios").value(3))
                .andExpect(jsonPath("$.yo.alias").value(yo.substring(0, yo.indexOf('@'))))
                .andExpect(jsonPath("$.yo.puntaje").value(10))
                .andExpect(jsonPath("$.yo.respondidos").value(1))
                .andExpect(jsonPath("$.rival.alias").value(rival.substring(0, rival.indexOf('@'))))
                .andExpect(jsonPath("$.rival.puntaje").value(0))
                .andExpect(jsonPath("$.rival.respondidos").value(0))
                .andExpect(jsonPath("$.rival_conectado").value(true))
                .andExpect(jsonPath("$.resultado").doesNotExist())
                .andExpect(jsonPath("$.motivo_fin").doesNotExist());
        // Cada uno se ve como «yo»: el rival ve lo mismo al revés.
        marcador(rival, partida).andExpect(jsonPath("$.yo.puntaje").value(0))
                .andExpect(jsonPath("$.rival.puntaje").value(10));
    }

    @Test
    void una_partida_pendiente_tambien_tiene_marcador_pero_sin_rival_conectado() throws Exception {
        publicarTres();
        yo = alumno();
        rival = alumno();
        long pendiente = reto(yo, rival);

        marcador(yo, pendiente).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("pendiente"))
                .andExpect(jsonPath("$.rival_conectado").value(false));
    }

    @Test
    void un_reto_vencido_se_ve_caducado() throws Exception {
        publicarTres();
        yo = alumno();
        rival = alumno();
        long pendiente = reto(yo, rival);
        creadaHaceHoras(pendiente, 25);

        marcador(yo, pendiente).andExpect(jsonPath("$.estado").value("caducada"));
    }

    // ---------- ETag y 304 (HU59-T2) ----------

    @Test
    void la_respuesta_trae_un_etag_y_sin_cambios_la_siguiente_consulta_da_304() throws Exception {
        empezar();
        String etag = etagDe(yo);
        assertThat(etag).isNotBlank();

        mvc.perform(get("/api/partidas/" + partida + "/marcador").with(tokenDeCliente(yo, "mobile"))
                .header("If-None-Match", etag)).andExpect(status().isNotModified()).andExpect(header().string("ETag", etag));
    }

    @Test
    void sondear_varias_veces_sin_cambios_mantiene_el_mismo_etag() throws Exception {
        empezar();

        assertThat(etagDe(yo)).isEqualTo(etagDe(yo));
    }

    @Test
    void cuando_el_rival_responde_el_etag_cambia_y_vuelve_a_dar_el_cuerpo() throws Exception {
        empezar();
        String antes = etagDe(yo);

        contestar(rival, partida, 1, BIEN).andExpect(status().isOk());

        mvc.perform(get("/api/partidas/" + partida + "/marcador").with(tokenDeCliente(yo, "mobile"))
                .header("If-None-Match", antes)).andExpect(status().isOk())
                .andExpect(jsonPath("$.rival.puntaje").value(10));
        assertThat(etagDe(yo)).isNotEqualTo(antes);
    }

    @Test
    void cada_jugador_tiene_su_propio_etag() throws Exception {
        empezar();
        contestar(yo, partida, 1, BIEN).andExpect(status().isOk());

        assertThat(etagDe(yo)).isNotEqualTo(etagDe(rival));
    }

    // ---------- el rival se desconectó (HU59-T4) ----------

    @Test
    void con_senal_reciente_el_rival_esta_conectado() throws Exception {
        empezar();
        senalHaceSegundos(partida, rival, 5);

        marcador(yo, partida).andExpect(jsonPath("$.rival_conectado").value(true));
    }

    @Test
    void sin_senal_por_mas_de_diez_segundos_el_rival_aparece_desconectado() throws Exception {
        empezar();
        senalHaceSegundos(partida, rival, 15);

        marcador(yo, partida).andExpect(status().isOk()).andExpect(jsonPath("$.rival_conectado").value(false))
                .andExpect(jsonPath("$.estado").value("en_curso"));
    }

    @Test
    void el_cambio_de_conectado_a_desconectado_cambia_el_etag() throws Exception {
        empezar();
        String conectado = etagDe(yo);
        senalHaceSegundos(partida, rival, 15);

        marcador(yo, partida).andExpect(status().isOk());
        assertThat(etagDe(yo)).isNotEqualTo(conectado);
    }

    @Test
    void el_rival_que_vuelve_dentro_del_plazo_sigue_en_la_partida() throws Exception {
        empezar();
        senalHaceSegundos(partida, rival, 25);

        marcador(yo, partida).andExpect(jsonPath("$.rival_conectado").value(false)).andExpect(jsonPath("$.estado").value("en_curso"));
        marcador(rival, partida).andExpect(jsonPath("$.estado").value("en_curso"));
        marcador(yo, partida).andExpect(jsonPath("$.rival_conectado").value(true));
    }

    @Test
    void cada_consulta_del_jugador_renueva_su_senal() throws Exception {
        empezar();
        senalHaceSegundos(partida, yo, 8);

        marcador(yo, partida).andExpect(status().isOk());

        marcador(rival, partida).andExpect(jsonPath("$.rival_conectado").value(true));
    }

    @Test
    void pedir_el_ejercicio_o_responder_tambien_renueva_la_senal() throws Exception {
        empezar();
        senalHaceSegundos(partida, yo, 15);
        pedirEjercicio(yo, partida).andExpect(status().isOk());
        marcador(rival, partida).andExpect(jsonPath("$.rival_conectado").value(true));

        senalHaceSegundos(partida, yo, 15);
        contestar(yo, partida, 1, BIEN).andExpect(status().isOk());
        marcador(rival, partida).andExpect(jsonPath("$.rival_conectado").value(true));
    }

    // ---------- abandono (HU59-T4) ----------

    @Test
    void a_los_treinta_segundos_sin_senal_gana_el_que_sigue_presente_por_abandono() throws Exception {
        empezar();
        senalHaceSegundos(partida, rival, 40);

        marcador(yo, partida).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("terminada"))
                .andExpect(jsonPath("$.resultado").value("gane"))
                .andExpect(jsonPath("$.motivo_fin").value("abandono"))
                .andExpect(jsonPath("$.rival_conectado").value(false));

        assertThat(estado(partida)).isEqualTo("terminada");
        assertThat(ganador(partida)).isEqualTo(idDe(yo));
        assertThat(motivo(partida)).isEqualTo("abandono");
    }

    @Test
    void quien_abandono_ve_que_perdio_cuando_vuelve() throws Exception {
        empezar();
        senalHaceSegundos(partida, rival, 40);
        marcador(yo, partida).andExpect(status().isOk());

        marcador(rival, partida).andExpect(jsonPath("$.estado").value("terminada")).andExpect(jsonPath("$.resultado").value("perdi"))
                .andExpect(jsonPath("$.motivo_fin").value("abandono"));
    }

    @Test
    void quien_vuelve_tarde_no_se_salva_solo_por_volver() throws Exception {
        empezar();
        senalHaceSegundos(partida, rival, 40);

        marcador(rival, partida).andExpect(jsonPath("$.estado").value("terminada")).andExpect(jsonPath("$.resultado").value("perdi"));
        assertThat(ganador(partida)).isEqualTo(idDe(yo));
    }

    @Test
    void si_los_dos_se_ausentaron_es_empate_por_abandono() throws Exception {
        empezar();
        senalHaceSegundos(partida, yo, 40);
        senalHaceSegundos(partida, rival, 45);

        marcador(yo, partida).andExpect(jsonPath("$.estado").value("terminada")).andExpect(jsonPath("$.resultado").value("empate"))
                .andExpect(jsonPath("$.motivo_fin").value("abandono"));

        assertThat(ganador(partida)).isNull();
        assertThat(motivo(partida)).isEqualTo("abandono");
    }

    @Test
    void el_abandono_gana_aunque_el_que_se_quedo_vaya_perdiendo() throws Exception {
        empezar();
        contestar(rival, partida, 1, BIEN).andExpect(status().isOk());
        contestar(yo, partida, 1, MAL).andExpect(status().isOk());
        senalHaceSegundos(partida, rival, 40);

        marcador(yo, partida).andExpect(jsonPath("$.resultado").value("gane"));
    }

    @Test
    void al_pedir_el_ejercicio_con_el_rival_ausente_la_partida_se_cierra_y_responde_409() throws Exception {
        empezar();
        senalHaceSegundos(partida, rival, 40);

        pedirEjercicio(yo, partida).andExpect(status().isConflict());

        assertThat(estado(partida)).isEqualTo("terminada");
        assertThat(ganador(partida)).isEqualTo(idDe(yo));
    }

    @Test
    void al_responder_con_el_rival_ausente_la_partida_se_cierra_y_la_respuesta_no_cuenta() throws Exception {
        empezar();
        senalHaceSegundos(partida, rival, 40);

        contestar(yo, partida, 1, BIEN).andExpect(status().isConflict());

        assertThat(estado(partida)).isEqualTo("terminada");
        assertThat(motivo(partida)).isEqualTo("abandono");
        assertThat(jdbc.sql("SELECT count(*) FROM app_schema.app_partida_respuesta WHERE partida_id = ?").param(partida)
                .query(Integer.class).single()).isZero();
    }

    @Test
    void una_partida_terminada_no_cambia_aunque_pase_el_tiempo() throws Exception {
        empezar();
        jugarTodo(yo, partida, 3);
        jugarTodo(rival, partida, 0);
        senalHaceSegundos(partida, rival, 100);
        senalHaceSegundos(partida, yo, 100);

        marcador(yo, partida).andExpect(jsonPath("$.motivo_fin").value("completada")).andExpect(jsonPath("$.resultado").value("gane"));
        assertThat(motivo(partida)).isEqualTo("completada");
    }

    @Test
    void el_abandono_no_se_aplica_a_un_reto_pendiente() throws Exception {
        publicarTres();
        yo = alumno();
        rival = alumno();
        long pendiente = reto(yo, rival);
        senalHaceSegundos(pendiente, yo, 3600);

        marcador(yo, pendiente).andExpect(jsonPath("$.estado").value("pendiente"));
        marcador(rival, pendiente).andExpect(jsonPath("$.estado").value("pendiente"));
    }

    @Test
    void aceptar_un_reto_viejo_da_a_los_dos_el_plazo_completo() throws Exception {
        publicarTres();
        yo = alumno();
        rival = alumno();
        long reto = reto(yo, rival);
        // El retador dejó el reto esperando horas: su señal es vieja.
        senalHaceSegundos(reto, yo, 3 * 3600);
        creadaHaceHoras(reto, 3);

        aceptar(rival, reto).andExpect(status().isOk());

        marcador(rival, reto).andExpect(jsonPath("$.estado").value("en_curso")).andExpect(jsonPath("$.rival_conectado").value(true));
    }
}
