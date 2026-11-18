package com.mastercode.userapi;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;

import org.springframework.test.web.servlet.ResultActions;

// Lo que comparten las pruebas de los duelos (HU57 a HU60): ayudas para retar, aceptar, jugar y mover los relojes de la
// partida. Los plazos (caducidad del reto, señal de los jugadores) se prueban escribiendo las fechas en la base: así no
// hay que esperar segundos de verdad.
abstract class PartidaApiBase extends EjerciciosMovilesBase {

    // Los ejercicios de las pruebas: predicción de salida con respuesta esperada «42».
    static final String BIEN = "42";
    static final String MAL = "nope";

    ResultActions retar(String de, Long cursoId, Long rivalId) throws Exception {
        String cuerpo = "{" + (cursoId == null ? "" : "\"curso_id\":" + cursoId)
                + (cursoId != null && rivalId != null ? "," : "") + (rivalId == null ? "" : "\"rival_id\":" + rivalId) + "}";
        return mvc.perform(post("/api/partidas").with(tokenDeCliente(de, "mobile")).contentType(APPLICATION_JSON)
                .content(cuerpo));
    }

    static long idDeLaRespuesta(ResultActions r) throws Exception {
        String texto = r.andReturn().getResponse().getContentAsString();
        return Long.parseLong(texto.replaceAll("(?s)^.*?\"(?:id|partida_id)\":(\\d+).*$", "$1"));
    }

    // Un reto pendiente del primero al segundo.
    long reto(String retador, String rival) throws Exception {
        return idDeLaRespuesta(retar(retador, CURSO, idDe(rival)).andExpect(status().isCreated()));
    }

    ResultActions aceptar(String email, long partida) throws Exception {
        return mvc.perform(post("/api/partidas/" + partida + "/aceptar").with(tokenDeCliente(email, "mobile")));
    }

    ResultActions rechazar(String email, long partida) throws Exception {
        return mvc.perform(post("/api/partidas/" + partida + "/rechazar").with(tokenDeCliente(email, "mobile")));
    }

    // Una partida ya aceptada: los dos jugadores con señal de ahora.
    long partida(String retador, String rival) throws Exception {
        long id = reto(retador, rival);
        aceptar(rival, id).andExpect(status().isOk());
        return id;
    }

    ResultActions pedirEjercicio(String email, long partida) throws Exception {
        return mvc.perform(get("/api/partidas/" + partida + "/ejercicio").with(tokenDeCliente(email, "mobile")));
    }

    ResultActions contestar(String email, long partida, Integer turno, String respuesta) throws Exception {
        String cuerpo = "{" + (turno == null ? "" : "\"turno\":" + turno + ",") + "\"respuesta\":\"" + respuesta + "\"}";
        return mvc.perform(post("/api/partidas/" + partida + "/respuesta").with(tokenDeCliente(email, "mobile"))
                .contentType(APPLICATION_JSON).content(cuerpo));
    }

    ResultActions contestarOpcion(String email, long partida, int turno, long opcionId) throws Exception {
        return mvc.perform(post("/api/partidas/" + partida + "/respuesta").with(tokenDeCliente(email, "mobile"))
                .contentType(APPLICATION_JSON).content("{\"turno\":" + turno + ",\"opcion_id\":" + opcionId + "}"));
    }

    ResultActions marcador(String email, long partida) throws Exception {
        return mvc.perform(get("/api/partidas/" + partida + "/marcador").with(tokenDeCliente(email, "mobile")));
    }

    // Responde todos los turnos que le faltan al jugador: acierta los primeros «aciertos» y falla el resto.
    void jugarTodo(String email, long partida, int aciertos) throws Exception {
        int total = totalDeEjercicios(partida);
        int respondidos = jdbc.sql("SELECT respondidos FROM app_schema.app_partida_jugador WHERE partida_id = ? AND estudiante_id = ?")
                .params(partida, idDe(email)).query(Integer.class).single();
        for (int turno = respondidos + 1; turno <= total; turno++) {
            contestar(email, partida, turno, turno <= aciertos ? BIEN : MAL).andExpect(status().isOk());
        }
    }

    int totalDeEjercicios(long partida) {
        return jdbc.sql("SELECT count(*) FROM app_schema.app_partida_ejercicio WHERE partida_id = ?")
                .param(partida).query(Integer.class).single();
    }

    long problemaDelTurno(long partida, int turno) {
        return jdbc.sql("SELECT problema_id FROM app_schema.app_partida_ejercicio WHERE partida_id = ? AND turno = ?")
                .params(partida, turno).query(Long.class).single();
    }

    String estado(long partida) {
        return jdbc.sql("SELECT estado FROM app_schema.app_partida WHERE id = ?").param(partida).query(String.class).single();
    }

    Long ganador(long partida) {
        return jdbc.sql("SELECT ganador_id FROM app_schema.app_partida WHERE id = ?").param(partida).query(Long.class)
                .optional().orElse(null);
    }

    String motivo(long partida) {
        return jdbc.sql("SELECT motivo_fin FROM app_schema.app_partida WHERE id = ?").param(partida).query(String.class)
                .optional().orElse(null);
    }

    // Deja la última señal del jugador hace tantos segundos.
    void senalHaceSegundos(long partida, String email, long segundos) {
        jdbc.sql("UPDATE app_schema.app_partida_jugador SET ultima_senal = ? WHERE partida_id = ? AND estudiante_id = ?")
                .params(OffsetDateTime.now().minusSeconds(segundos), partida, idDe(email)).update();
    }

    void creadaHaceHoras(long partida, long horas) {
        jdbc.sql("UPDATE app_schema.app_partida SET fecha_creacion = ? WHERE id = ?")
                .params(OffsetDateTime.now().minusHours(horas), partida).update();
    }

    int cuantasPartidas() {
        return jdbc.sql("SELECT count(*) FROM app_schema.app_partida").query(Integer.class).single();
    }

    // Tres ejercicios de predicción de salida con respuesta «42».
    void publicarTres() {
        publicar(prediccion(1, "facil", "bucles", BIEN), prediccion(2, "facil", "bucles", BIEN),
                prediccion(3, "media", "listas", BIEN));
    }
}
