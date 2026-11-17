package com.mastercode.userapi;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.springframework.test.web.servlet.ResultActions;

import com.mastercode.userapi.admin.ProblemaDto;
import com.mastercode.userapi.admin.ProblemaResumenDto;
import com.mastercode.userapi.admin.RespuestaDelProblemaDto;

// Lo que comparten las pruebas de los ejercicios del celular (HU45 a HU48, HU54): admin-api es un mock con un curso donde
// se publican ejercicios de los tres tipos, y ayudas para responder, abrir sesiones del Dojo y pedir el siguiente.
abstract class EjerciciosMovilesBase extends PruebaDeApi {

    static final long CURSO = 10;

    // Un ejercicio del celular tal como lo publica admin-api. En opción múltiple, la correcta es la que tiene
    // esCorrecta; en los otros dos tipos, la respuesta esperada.
    record Ejercicio(long id, String tipo, String dificultad, String tema, String esperada,
            List<RespuestaDelProblemaDto.Opcion> opciones) {
    }

    static Ejercicio prediccion(long id, String dificultad, String tema, String esperada) {
        return new Ejercicio(id, "prediccion_salida", dificultad, tema, esperada, List.of());
    }

    static Ejercicio completar(long id, String dificultad, String tema, String esperada) {
        return new Ejercicio(id, "completar_espacio", dificultad, tema, esperada, List.of());
    }

    // Opción múltiple con tres alternativas: la correcta tiene el id 100 * id + 2.
    static Ejercicio multiple(long id, String dificultad, String tema) {
        return new Ejercicio(id, "opcion_multiple", dificultad, tema, "", List.of(
                new RespuestaDelProblemaDto.Opcion(100 * id + 1, "Una alternativa", false),
                new RespuestaDelProblemaDto.Opcion(100 * id + 2, "La alternativa buena", true),
                new RespuestaDelProblemaDto.Opcion(100 * id + 3, "Otra alternativa", false)));
    }

    static long correctaDe(Ejercicio ejercicio) {
        return 100 * ejercicio.id() + 2;
    }

    void publicar(Ejercicio... ejercicios) {
        List<ProblemaResumenDto> resumenes = new ArrayList<>();
        for (Ejercicio x : ejercicios) {
            resumenes.add(new ProblemaResumenDto(x.id(), "Ejercicio " + x.id(), "python", x.dificultad(), "movil", x.tipo(),
                    null, x.tema()));
            List<ProblemaDto.Opcion> opcionesVisibles = x.opciones().stream()
                    .map(o -> new ProblemaDto.Opcion(o.id(), o.texto())).toList();
            when(admin.problema(x.id())).thenReturn(new ProblemaDto(x.id(), "Ejercicio " + x.id(), "¿Qué imprime?", "python",
                    x.dificultad(), "movil", x.tipo(), "", List.of(), CURSO, opcionesVisibles, x.tema()));
            when(admin.respuestaDelProblema(x.id())).thenReturn(
                    new RespuestaDelProblemaDto(x.id(), CURSO, x.tipo(), x.esperada(), x.opciones()));
        }
        when(admin.problemasDelCurso(CURSO, "movil")).thenReturn(resumenes);
        when(admin.problemasDelCurso(CURSO, null)).thenReturn(resumenes);
    }

    // Un estudiante inscrito en el curso; devuelve su correo.
    String alumno() {
        String email = correoUnico();
        inscribir(crearEstudiante(email), CURSO);
        return email;
    }

    long idDe(String email) {
        return jdbc.sql("SELECT id FROM app_schema.app_estudiante WHERE email = ?").param(email).query(Long.class).single();
    }

    // Cuerpo JSON con solo los campos que no son null.
    private static String cuerpo(String respuesta, Long opcionId, Long sesionId, String intentoId) {
        List<String> campos = new ArrayList<>();
        if (respuesta != null) {
            campos.add("\"respuesta\":\"" + respuesta.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
                    .replace("\r", "\\r").replace("\t", "\\t") + "\"");
        }
        if (opcionId != null) {
            campos.add("\"opcion_id\":" + opcionId);
        }
        if (sesionId != null) {
            campos.add("\"sesion_id\":" + sesionId);
        }
        if (intentoId != null) {
            campos.add("\"intento_id\":\"" + intentoId + "\"");
        }
        return "{" + String.join(",", campos) + "}";
    }

    ResultActions enviar(String email, long problemaId, String respuesta, Long opcionId, Long sesionId, String intentoId)
            throws Exception {
        return mvc.perform(post("/api/problemas/" + problemaId + "/respuesta").with(tokenDeCliente(email, "mobile"))
                .contentType(APPLICATION_JSON).content(cuerpo(respuesta, opcionId, sesionId, intentoId)));
    }

    ResultActions responder(String email, long problemaId, String respuesta) throws Exception {
        return enviar(email, problemaId, respuesta, null, null, null);
    }

    ResultActions iniciarSesion(String email, String lenguaje) throws Exception {
        return mvc.perform(post("/api/dojo/sesiones").with(tokenDeCliente(email, "mobile"))
                .contentType(APPLICATION_JSON).content("{\"lenguaje\":\"" + lenguaje + "\"}"));
    }

    long sesion(String email) throws Exception {
        String texto = iniciarSesion(email, "python").andExpect(status().isCreated()).andReturn().getResponse()
                .getContentAsString();
        return Long.parseLong(texto.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    ResultActions pedirSiguiente(String email, long sesion) throws Exception {
        return mvc.perform(post("/api/dojo/sesiones/" + sesion + "/siguiente").with(tokenDeCliente(email, "mobile")));
    }

    long idDelSiguiente(String email, long sesion) throws Exception {
        String texto = pedirSiguiente(email, sesion).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        return Long.parseLong(texto.replaceAll(".*?\"id\":(\\d+).*", "$1"));
    }

    int intentosGuardados(long estudianteId, long problemaId) {
        return jdbc.sql("SELECT count(*) FROM app_schema.app_envio WHERE estudiante_id = ? AND problema_id = ?")
                .params(estudianteId, problemaId).query(Integer.class).single();
    }

    void guardarNivel(String email, String nivel) {
        jdbc.sql("INSERT INTO app_schema.app_nivel_dificultad (estudiante_id, lenguaje, nivel) VALUES (?, 'python', ?)")
                .params(idDe(email), nivel).update();
    }
}
