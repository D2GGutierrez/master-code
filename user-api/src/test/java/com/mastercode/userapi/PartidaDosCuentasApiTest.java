package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

// HU58-T4 y HU60-T4 (ADR-15): una partida de principio a fin entre dos cuentas, como la juegan los dos celulares, y el
// ranking que deja. A diferencia de las demás pruebas de los duelos, aquí no se siembra nada en la base: todo pasa por la
// API, en el orden en que la usa la app (retar, invitaciones, aceptar, turnos, marcador, historial, ranking).
class PartidaDosCuentasApiTest extends PartidaApiBase {

    private static String alias(String email) {
        return email.substring(0, email.indexOf('@'));
    }

    private ResultActions invitaciones(String email) throws Exception {
        return mvc.perform(get("/api/partidas/invitaciones").with(tokenDeCliente(email, "mobile")));
    }

    private ResultActions historial(String email) throws Exception {
        return mvc.perform(get("/api/partidas").with(tokenDeCliente(email, "mobile")));
    }

    private ResultActions ranking(String email, long curso) throws Exception {
        return mvc.perform(get("/api/cursos/" + curso + "/ranking").with(tokenDeCliente(email, "mobile")));
    }

    // ---------- HU58-T4: la partida completa con dos cuentas ----------

    @Test
    void una_partida_completa_entre_dos_cuentas_de_principio_a_fin() throws Exception {
        publicarTres();
        String ana = alumno();
        String beto = alumno();

        // 1. Ana reta a Beto: el reto queda pendiente, con los tres ejercicios ya elegidos.
        long partida = idDeLaRespuesta(retar(ana, CURSO, idDe(beto)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("pendiente")).andExpect(jsonPath("$.rival").value(alias(beto)))
                .andExpect(jsonPath("$.total_ejercicios").value(3)));
        marcador(ana, partida).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("pendiente"));

        // 2. Beto ve la invitación (Ana no ve la suya) y la acepta: la partida pasa a en_curso para los dos.
        invitaciones(beto).andExpect(status().isOk()).andExpect(content().string(containsString("\"id\":" + partida)))
                .andExpect(content().string(containsString("\"retador\":\"" + alias(ana) + "\"")));
        invitaciones(ana).andExpect(status().isOk()).andExpect(content().string(not(containsString("\"id\":" + partida))));
        aceptar(beto, partida).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("en_curso"));
        marcador(ana, partida).andExpect(jsonPath("$.estado").value("en_curso"));
        invitaciones(beto).andExpect(content().string(not(containsString("\"id\":" + partida))));

        // 3. Los tres turnos: los dos reciben el mismo ejercicio en cada uno. Ana acierta los tres; Beto, solo el primero.
        for (int turno = 1; turno <= 3; turno++) {
            long esperado = problemaDelTurno(partida, turno);
            pedirEjercicio(ana, partida).andExpect(status().isOk()).andExpect(jsonPath("$.turno").value(turno))
                    .andExpect(jsonPath("$.ejercicio.id").value(esperado));
            pedirEjercicio(beto, partida).andExpect(status().isOk()).andExpect(jsonPath("$.turno").value(turno))
                    .andExpect(jsonPath("$.ejercicio.id").value(esperado));
            // Quien responde primero cambia de turno en turno: ninguno depende de que el otro vaya antes.
            String primero = turno % 2 == 1 ? ana : beto;
            String segundo = turno % 2 == 1 ? beto : ana;
            contestar(primero, partida, turno, respuestaDe(primero, ana, turno)).andExpect(status().isOk());
            contestar(segundo, partida, turno, respuestaDe(segundo, ana, turno)).andExpect(status().isOk());
            if (turno < 3) {
                // Con la partida todavía en juego, los dos marcadores cuadran vistos desde cada lado.
                marcador(ana, partida).andExpect(jsonPath("$.estado").value("en_curso"))
                        .andExpect(jsonPath("$.yo.respondidos").value(turno)).andExpect(jsonPath("$.rival.respondidos").value(turno));
                marcador(beto, partida).andExpect(jsonPath("$.yo.respondidos").value(turno))
                        .andExpect(jsonPath("$.rival.respondidos").value(turno));
            }
        }

        // 4. Termina por completar los tres turnos: 30 contra 10.
        assertThat(estado(partida)).isEqualTo("terminada");
        assertThat(motivo(partida)).isEqualTo("completada");
        assertThat(ganador(partida)).isEqualTo(idDe(ana));
        marcador(ana, partida).andExpect(jsonPath("$.estado").value("terminada")).andExpect(jsonPath("$.resultado").value("gane"))
                .andExpect(jsonPath("$.motivo_fin").value("completada")).andExpect(jsonPath("$.yo.puntaje").value(30))
                .andExpect(jsonPath("$.rival.puntaje").value(10));
        marcador(beto, partida).andExpect(jsonPath("$.resultado").value("perdi")).andExpect(jsonPath("$.yo.puntaje").value(10))
                .andExpect(jsonPath("$.rival.puntaje").value(30));

        // 5. Ya no hay más ejercicios ni respuestas para ninguno.
        pedirEjercicio(ana, partida).andExpect(status().isConflict());
        contestar(beto, partida, 3, BIEN).andExpect(status().isConflict());

        // 6. Los dos la ven en su historial con su resultado.
        historial(ana).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(partida))
                .andExpect(jsonPath("$.items[0].resultado").value("gane")).andExpect(jsonPath("$.items[0].mi_puntaje").value(30))
                .andExpect(jsonPath("$.items[0].puntaje_rival").value(10)).andExpect(jsonPath("$.items[0].rival").value(alias(beto)));
        historial(beto).andExpect(jsonPath("$.items[0].id").value(partida)).andExpect(jsonPath("$.items[0].resultado").value("perdi"))
                .andExpect(jsonPath("$.items[0].rival").value(alias(ana)));

        // 7. Y en el ranking del curso: una ganada para Ana, una perdida para Beto.
        ranking(ana, CURSO).andExpect(status().isOk()).andExpect(jsonPath("$.yo.jugadas").value(1))
                .andExpect(jsonPath("$.yo.ganadas").value(1)).andExpect(jsonPath("$.yo.perdidas").value(0))
                .andExpect(jsonPath("$.yo.puntos").value(30)).andExpect(jsonPath("$.yo.soy_yo").value(true));
        ranking(beto, CURSO).andExpect(jsonPath("$.yo.jugadas").value(1)).andExpect(jsonPath("$.yo.ganadas").value(0))
                .andExpect(jsonPath("$.yo.perdidas").value(1)).andExpect(jsonPath("$.yo.puntos").value(10));
    }

    // ---------- HU60-T4: el ranking en los casos de borde ----------

    // Dos partidas jugadas de verdad que terminan igualadas: cuentan como empate para los dos, no como ganada ni perdida.
    @Test
    void el_ranking_cuenta_un_empate_jugado_de_verdad() throws Exception {
        publicarTres();
        String ana = alumno();
        String beto = alumno();
        long partida = partida(ana, beto);

        jugarTodo(ana, partida, 2);
        jugarTodo(beto, partida, 2);

        assertThat(estado(partida)).isEqualTo("terminada");
        assertThat(ganador(partida)).isNull();
        marcador(ana, partida).andExpect(jsonPath("$.resultado").value("empate")).andExpect(jsonPath("$.yo.puntaje").value(20))
                .andExpect(jsonPath("$.rival.puntaje").value(20));
        marcador(beto, partida).andExpect(jsonPath("$.resultado").value("empate"));
        historial(ana).andExpect(jsonPath("$.items[0].resultado").value("empate"));
        ranking(ana, CURSO).andExpect(jsonPath("$.yo.jugadas").value(1)).andExpect(jsonPath("$.yo.empatadas").value(1))
                .andExpect(jsonPath("$.yo.ganadas").value(0)).andExpect(jsonPath("$.yo.perdidas").value(0))
                .andExpect(jsonPath("$.yo.porcentaje_victorias").value(0.0));
        ranking(beto, CURSO).andExpect(jsonPath("$.yo.empatadas").value(1));
    }

    // Los dos con los mismos números: el orden entre ellos es siempre el mismo (por id) y ambos aparecen con su posición.
    @Test
    void con_el_mismo_empate_el_orden_es_estable() throws Exception {
        publicarTres();
        String ana = alumno();
        String beto = alumno();
        long partida = partida(ana, beto);
        jugarTodo(ana, partida, 1);
        jugarTodo(beto, partida, 1);

        int posicionDeAna = posicion(ana);
        int posicionDeBeto = posicion(beto);

        // Misma jugada, misma partida empatada: se desempata por el id del estudiante, el más antiguo primero.
        assertThat(posicionDeAna).isNotEqualTo(posicionDeBeto);
        assertThat(posicionDeAna < posicionDeBeto).isEqualTo(idDe(ana) < idDe(beto));
        assertThat(posicion(ana)).isEqualTo(posicionDeAna);
    }

    // Quien está inscrito pero nunca jugó sale en el ranking con ceros, no se oculta ni da error.
    @Test
    void quien_no_ha_jugado_sale_en_el_ranking_con_ceros() throws Exception {
        String sinJugar = alumno();

        ranking(sinJugar, CURSO).andExpect(status().isOk()).andExpect(jsonPath("$.yo.soy_yo").value(true))
                .andExpect(jsonPath("$.yo.alias").value(alias(sinJugar))).andExpect(jsonPath("$.yo.jugadas").value(0))
                .andExpect(jsonPath("$.yo.ganadas").value(0)).andExpect(jsonPath("$.yo.empatadas").value(0))
                .andExpect(jsonPath("$.yo.perdidas").value(0)).andExpect(jsonPath("$.yo.puntos").value(0))
                .andExpect(jsonPath("$.yo.porcentaje_victorias").value(0.0));
        historial(sinJugar).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
    }

    // Un reto sin aceptar o rechazado no cuenta: no es una partida jugada.
    @Test
    void un_reto_pendiente_o_rechazado_no_cuenta_en_el_ranking() throws Exception {
        publicarTres();
        String ana = alumno();
        String beto = alumno();
        String carla = alumno();
        reto(ana, beto);
        long rechazado = reto(ana, carla);
        rechazar(carla, rechazado).andExpect(status().isNoContent());

        ranking(ana, CURSO).andExpect(jsonPath("$.yo.jugadas").value(0)).andExpect(jsonPath("$.yo.ganadas").value(0));
        ranking(carla, CURSO).andExpect(jsonPath("$.yo.jugadas").value(0));
    }

    // Quien no está inscrito en el curso recibe 403, sea cual sea la página que pida, y no ve a nadie.
    @Test
    void un_estudiante_no_inscrito_recibe_403_en_el_ranking() throws Exception {
        String fuera = correoUnico();
        crearEstudiante(fuera);

        ranking(fuera, CURSO).andExpect(status().isForbidden());
        mvc.perform(get("/api/cursos/" + CURSO + "/ranking?pagina=2&tamano=5").with(tokenDeCliente(fuera, "mobile")))
                .andExpect(status().isForbidden());
    }

    // Sin sesión, ni siquiera se llega a saber si el curso existe.
    @Test
    void el_ranking_pide_sesion() throws Exception {
        mvc.perform(get("/api/cursos/" + CURSO + "/ranking")).andExpect(status().isUnauthorized());
    }

    // Ana acierta siempre; Beto, solo el primer turno.
    private static String respuestaDe(String quien, String ana, int turno) {
        return quien.equals(ana) || turno == 1 ? BIEN : MAL;
    }

    // La posición de un estudiante en el ranking completo (la entrada «yo» siempre viene).
    private int posicion(String email) throws Exception {
        String cuerpo = ranking(email, CURSO).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"yo\":\\{\"posicion\":(\\d+)").matcher(cuerpo);
        assertThat(m.find()).as("la respuesta trae «yo» con su posición: " + cuerpo).isTrue();
        return Integer.parseInt(m.group(1));
    }
}
