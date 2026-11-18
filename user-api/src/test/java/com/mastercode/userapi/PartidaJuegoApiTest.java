package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.mastercode.userapi.admin.AdminNoDisponibleException;
import com.mastercode.userapi.cinturon.CinturonRepository;
import com.mastercode.userapi.racha.RachaRepository;

// HU58-T2, HU58-T3 y H9 (ADR-15): los dos jugadores reciben el mismo ejercicio en cada turno, ninguno se adelanta en la
// secuencia, y al terminar los dos gana el de mayor puntaje (o empatan).
class PartidaJuegoApiTest extends PartidaApiBase {

    @Autowired
    private RachaRepository rachas;

    @Autowired
    private CinturonRepository cinturones;

    private String yo;
    private String rival;
    private long partida;

    // Una partida de tres ejercicios ya aceptada entre dos compañeros.
    private void empezar() throws Exception {
        publicarTres();
        yo = alumno();
        rival = alumno();
        partida = partida(yo, rival);
    }

    // ---------- el ejercicio del turno (HU58-T2) ----------

    @Test
    void los_dos_jugadores_reciben_el_mismo_ejercicio_en_el_turno_uno() throws Exception {
        empezar();
        long esperado = problemaDelTurno(partida, 1);

        pedirEjercicio(yo, partida).andExpect(status().isOk())
                .andExpect(jsonPath("$.turno").value(1)).andExpect(jsonPath("$.total_ejercicios").value(3))
                .andExpect(jsonPath("$.ejercicio.id").value(esperado));
        pedirEjercicio(rival, partida).andExpect(status().isOk())
                .andExpect(jsonPath("$.turno").value(1)).andExpect(jsonPath("$.ejercicio.id").value(esperado));
    }

    @Test
    void cada_turno_trae_el_ejercicio_de_la_secuencia_para_los_dos() throws Exception {
        empezar();

        for (int turno = 1; turno <= 3; turno++) {
            long esperado = problemaDelTurno(partida, turno);
            pedirEjercicio(yo, partida).andExpect(jsonPath("$.turno").value(turno)).andExpect(jsonPath("$.ejercicio.id").value(esperado));
            pedirEjercicio(rival, partida).andExpect(jsonPath("$.turno").value(turno)).andExpect(jsonPath("$.ejercicio.id").value(esperado));
            contestar(yo, partida, turno, BIEN).andExpect(status().isOk());
            contestar(rival, partida, turno, MAL).andExpect(status().isOk());
        }
    }

    @Test
    void pedir_el_ejercicio_otra_vez_no_avanza_el_turno() throws Exception {
        empezar();

        pedirEjercicio(yo, partida).andExpect(jsonPath("$.turno").value(1));
        pedirEjercicio(yo, partida).andExpect(jsonPath("$.turno").value(1));

        assertThat(jdbc.sql("SELECT respondidos FROM app_schema.app_partida_jugador WHERE partida_id = ? AND estudiante_id = ?")
                .params(partida, idDe(yo)).query(Integer.class).single()).isZero();
    }

    @Test
    void el_ejercicio_de_opcion_multiple_no_trae_la_marca_de_la_correcta() throws Exception {
        publicar(multiple(1, "facil", "a"));
        yo = alumno();
        rival = alumno();
        partida = partida(yo, rival);

        pedirEjercicio(yo, partida).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("es_correcta"))))
                .andExpect(jsonPath("$.ejercicio.opciones.length()").value(3));
    }

    @Test
    void sin_aceptar_todavia_no_hay_ejercicio() throws Exception {
        publicarTres();
        yo = alumno();
        rival = alumno();
        long pendiente = reto(yo, rival);

        pedirEjercicio(yo, pendiente).andExpect(status().isConflict());
        pedirEjercicio(rival, pendiente).andExpect(status().isConflict());
    }

    @Test
    void quien_termino_espera_al_rival_con_204() throws Exception {
        empezar();
        jugarTodo(yo, partida, 3);

        pedirEjercicio(yo, partida).andExpect(status().isNoContent());
        // El rival sigue en el primero.
        pedirEjercicio(rival, partida).andExpect(status().isOk()).andExpect(jsonPath("$.turno").value(1));
    }

    @Test
    void un_ajeno_no_ve_la_partida() throws Exception {
        empezar();
        String tercero = alumno();

        pedirEjercicio(tercero, partida).andExpect(status().isNotFound());
        contestar(tercero, partida, 1, BIEN).andExpect(status().isNotFound());
        marcador(tercero, partida).andExpect(status().isNotFound());
    }

    // ---------- responder (HU58-T3, H9) ----------

    @Test
    void un_acierto_suma_diez_puntos_y_cuenta_el_ejercicio() throws Exception {
        empezar();

        contestar(yo, partida, 1, BIEN).andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("correcto"))
                .andExpect(jsonPath("$.respuesta_correcta").doesNotExist())
                .andExpect(jsonPath("$.marcador.yo.puntaje").value(10))
                .andExpect(jsonPath("$.marcador.yo.respondidos").value(1))
                .andExpect(jsonPath("$.marcador.rival.puntaje").value(0))
                .andExpect(jsonPath("$.marcador.rival.respondidos").value(0));
    }

    @Test
    void un_fallo_no_suma_pero_cuenta_el_ejercicio_y_dice_la_respuesta() throws Exception {
        empezar();

        contestar(yo, partida, 1, MAL).andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("incorrecto"))
                .andExpect(jsonPath("$.respuesta_correcta").value(BIEN))
                .andExpect(jsonPath("$.marcador.yo.puntaje").value(0))
                .andExpect(jsonPath("$.marcador.yo.respondidos").value(1));
    }

    @Test
    void la_opcion_multiple_se_corrige_por_la_alternativa_elegida() throws Exception {
        publicar(multiple(1, "facil", "a"));
        yo = alumno();
        rival = alumno();
        partida = partida(yo, rival);

        contestarOpcion(yo, partida, 1, correctaDe(multiple(1, "facil", "a"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("correcto"));
        contestarOpcion(rival, partida, 1, 101).andExpect(status().isOk()).andExpect(jsonPath("$.resultado").value("incorrecto"));
    }

    @Test
    void no_se_puede_responder_un_turno_adelantado() throws Exception {
        empezar();

        contestar(yo, partida, 2, BIEN).andExpect(status().isConflict());

        assertThat(jdbc.sql("SELECT count(*) FROM app_schema.app_partida_respuesta WHERE partida_id = ?").param(partida)
                .query(Integer.class).single()).isZero();
    }

    @Test
    void ir_adelantado_al_rival_si_se_puede_pero_solo_de_a_un_turno_en_orden() throws Exception {
        empezar();
        contestar(yo, partida, 1, BIEN).andExpect(status().isOk());
        contestar(yo, partida, 2, BIEN).andExpect(status().isOk());

        // El rival no se mueve del turno uno aunque yo ya vaya en el tres.
        pedirEjercicio(rival, partida).andExpect(jsonPath("$.turno").value(1));
        contestar(rival, partida, 3, BIEN).andExpect(status().isConflict());
    }

    @Test
    void no_se_puede_repetir_un_turno_ya_respondido() throws Exception {
        empezar();
        contestar(yo, partida, 1, BIEN).andExpect(status().isOk());

        contestar(yo, partida, 1, BIEN).andExpect(status().isConflict());

        assertThat(jdbc.sql("SELECT puntaje FROM app_schema.app_partida_jugador WHERE partida_id = ? AND estudiante_id = ?")
                .params(partida, idDe(yo)).query(Integer.class).single()).isEqualTo(10);
    }

    @Test
    void falta_el_turno_o_no_existe() throws Exception {
        empezar();

        contestar(yo, partida, null, BIEN).andExpect(status().isBadRequest());
        contestar(yo, partida, 0, BIEN).andExpect(status().isBadRequest());
        contestar(yo, partida, 99, BIEN).andExpect(status().isBadRequest());
    }

    @Test
    void una_respuesta_vacia_se_rechaza_y_no_gasta_el_turno() throws Exception {
        empezar();

        contestar(yo, partida, 1, "  ").andExpect(status().isBadRequest());

        contestar(yo, partida, 1, BIEN).andExpect(status().isOk());
    }

    @Test
    void si_admin_api_no_responde_no_se_registra_la_respuesta() throws Exception {
        empezar();
        when(admin.respuestaDelProblema(problemaDelTurno(partida, 1))).thenThrow(new AdminNoDisponibleException("caído", null));

        contestar(yo, partida, 1, BIEN).andExpect(status().isServiceUnavailable());

        assertThat(jdbc.sql("SELECT count(*) FROM app_schema.app_partida_respuesta WHERE partida_id = ?").param(partida)
                .query(Integer.class).single()).isZero();
    }

    @Test
    void la_partida_guarda_sus_propias_respuestas_y_no_toca_los_envios_ni_los_intentos_del_dojo() throws Exception {
        empezar();

        contestar(yo, partida, 1, BIEN).andExpect(status().isOk());
        contestar(yo, partida, 2, MAL).andExpect(status().isOk());

        assertThat(jdbc.sql("SELECT count(*) FROM app_schema.app_partida_respuesta WHERE partida_id = ? AND estudiante_id = ?")
                .params(partida, idDe(yo)).query(Integer.class).single()).isEqualTo(2);
        assertThat(intentosGuardados(idDe(yo), problemaDelTurno(partida, 1))).isZero();
        assertThat(intentosGuardados(idDe(yo), problemaDelTurno(partida, 2))).isZero();
        // Los tres intentos del Dojo del ejercicio siguen enteros.
        responder(yo, problemaDelTurno(partida, 1), BIEN).andExpect(status().isOk())
                .andExpect(jsonPath("$.intentos_restantes").value(2));
    }

    @Test
    void un_acierto_en_la_partida_suma_a_la_racha_y_al_cinturon() throws Exception {
        empezar();
        long problema = problemaDelTurno(partida, 1);

        contestar(yo, partida, 1, BIEN).andExpect(status().isOk());

        assertThat(rachas.listar(idDe(yo))).hasSize(1);
        assertThat(rachas.listar(idDe(yo)).get(0).rachaActual()).isEqualTo(1);
        assertThat(cinturones.buscar(idDe(yo), "python")).isPresent();
        assertThat(cinturones.buscar(idDe(yo), "python").orElseThrow().puntos()).isEqualTo(10);
        assertThat(problema).isPositive();
    }

    @Test
    void un_fallo_no_suma_a_la_racha_ni_al_cinturon() throws Exception {
        empezar();

        contestar(yo, partida, 1, MAL).andExpect(status().isOk());

        assertThat(rachas.listar(idDe(yo))).isEmpty();
        assertThat(cinturones.buscar(idDe(yo), "python")).isEmpty();
    }

    @Test
    void el_cinturon_no_suma_dos_veces_el_mismo_ejercicio_aunque_se_juegue_otra_partida() throws Exception {
        publicar(prediccion(1, "facil", "a", BIEN));
        yo = alumno();
        rival = alumno();
        long primera = partida(yo, rival);
        contestar(yo, primera, 1, BIEN).andExpect(status().isOk());
        contestar(rival, primera, 1, BIEN).andExpect(status().isOk());
        assertThat(estado(primera)).isEqualTo("terminada");

        long segunda = partida(yo, rival);
        contestar(yo, segunda, 1, BIEN).andExpect(status().isOk());

        assertThat(cinturones.buscar(idDe(yo), "python").orElseThrow().puntos()).isEqualTo(10);
    }

    // ---------- el cierre (H9) ----------

    @Test
    void la_partida_sigue_en_curso_mientras_falte_alguien() throws Exception {
        empezar();

        jugarTodo(yo, partida, 3);

        assertThat(estado(partida)).isEqualTo("en_curso");
    }

    @Test
    void gana_el_de_mayor_puntaje_cuando_los_dos_terminan() throws Exception {
        empezar();
        jugarTodo(yo, partida, 3);

        // El rival termina con dos aciertos: 20 contra 30.
        contestar(rival, partida, 1, BIEN).andExpect(status().isOk());
        contestar(rival, partida, 2, BIEN).andExpect(status().isOk());
        contestar(rival, partida, 3, MAL).andExpect(status().isOk())
                .andExpect(jsonPath("$.marcador.estado").value("terminada"))
                .andExpect(jsonPath("$.marcador.resultado").value("perdi"))
                .andExpect(jsonPath("$.marcador.motivo_fin").value("completada"))
                .andExpect(jsonPath("$.marcador.yo.puntaje").value(20))
                .andExpect(jsonPath("$.marcador.rival.puntaje").value(30));

        assertThat(estado(partida)).isEqualTo("terminada");
        assertThat(ganador(partida)).isEqualTo(idDe(yo));
        assertThat(motivo(partida)).isEqualTo("completada");
        assertThat(jdbc.sql("SELECT fecha_fin FROM app_schema.app_partida WHERE id = ?").param(partida)
                .query(java.sql.Timestamp.class).single()).isNotNull();
        marcador(yo, partida).andExpect(jsonPath("$.resultado").value("gane"));
    }

    @Test
    void con_el_mismo_puntaje_es_empate_y_no_hay_ganador() throws Exception {
        empezar();
        jugarTodo(yo, partida, 2);
        jugarTodo(rival, partida, 2);

        assertThat(estado(partida)).isEqualTo("terminada");
        assertThat(ganador(partida)).isNull();
        assertThat(motivo(partida)).isEqualTo("completada");
        marcador(yo, partida).andExpect(jsonPath("$.resultado").value("empate"));
        marcador(rival, partida).andExpect(jsonPath("$.resultado").value("empate"));
    }

    @Test
    void todos_fallados_por_los_dos_tambien_es_empate() throws Exception {
        empezar();
        jugarTodo(yo, partida, 0);
        jugarTodo(rival, partida, 0);

        assertThat(ganador(partida)).isNull();
        assertThat(estado(partida)).isEqualTo("terminada");
    }

    @Test
    void una_partida_terminada_ya_no_recibe_respuestas_ni_entrega_ejercicios() throws Exception {
        empezar();
        jugarTodo(yo, partida, 3);
        jugarTodo(rival, partida, 0);

        contestar(yo, partida, 1, BIEN).andExpect(status().isConflict());
        pedirEjercicio(yo, partida).andExpect(status().isConflict());
        assertThat(jdbc.sql("SELECT sum(puntaje) FROM app_schema.app_partida_jugador WHERE partida_id = ?").param(partida)
                .query(Integer.class).single()).isEqualTo(30);
    }

    @Test
    void sin_aceptar_no_se_puede_responder() throws Exception {
        publicarTres();
        yo = alumno();
        rival = alumno();
        long pendiente = reto(yo, rival);

        contestar(yo, pendiente, 1, BIEN).andExpect(status().isConflict());
    }

    @Test
    void los_dos_pueden_responder_a_la_vez_sin_pisarse_el_marcador() throws Exception {
        empezar();

        contestar(yo, partida, 1, BIEN).andExpect(status().isOk());
        contestar(rival, partida, 1, BIEN).andExpect(status().isOk());

        List<Integer> puntajes = jdbc.sql("SELECT puntaje FROM app_schema.app_partida_jugador WHERE partida_id = ?")
                .param(partida).query(Integer.class).list();
        assertThat(puntajes).containsExactly(10, 10);
    }
}
