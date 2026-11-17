package com.mastercode.userapi.admin;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Un problema tal como lo entrega la API interna de admin-api para un estudiante: sin casos ocultos,
// sin respuesta esperada y sin saber cuál alternativa es correcta. Trae el curso porque user-api debe
// comprobar que el estudiante está inscrito en él antes de entregarlo. codigo_base es el código inicial (HU34);
// llega vacío si el docente no escribió ninguno. opciones son las alternativas de un ejercicio de opción múltiple
// (HU46-T2), solo con id y texto; en los demás tipos llega la lista vacía. tema (D13) puede llegar vacío; no se muestra
// al estudiante: user-api lo usa para sumar los fallos de una sesión por tema (HU56-T1).
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProblemaDto(
        long id,
        String titulo,
        String enunciado,
        String lenguaje,
        String dificultad,
        String plataforma,
        @JsonProperty("tipo_ejercicio") String tipoEjercicio,
        @JsonProperty("codigo_base") String codigoBase,
        @JsonProperty("casos_prueba") List<Caso> casosPrueba,
        @JsonProperty("curso_id") long cursoId,
        List<Opcion> opciones,
        String tema) {

    // Sin tema (HU56-T1): para los problemas que llegan sin él.
    public ProblemaDto(long id, String titulo, String enunciado, String lenguaje, String dificultad, String plataforma,
            String tipoEjercicio, String codigoBase, List<Caso> casosPrueba, long cursoId, List<Opcion> opciones) {
        this(id, titulo, enunciado, lenguaje, dificultad, plataforma, tipoEjercicio, codigoBase, casosPrueba, cursoId,
                opciones, null);
    }

    // Sin alternativas: el constructor de siempre, para los problemas que no son de opción múltiple.
    public ProblemaDto(long id, String titulo, String enunciado, String lenguaje, String dificultad, String plataforma,
            String tipoEjercicio, String codigoBase, List<Caso> casosPrueba, long cursoId) {
        this(id, titulo, enunciado, lenguaje, dificultad, plataforma, tipoEjercicio, codigoBase, casosPrueba, cursoId,
                List.of(), null);
    }

    // Una alternativa tal como le llega al estudiante: sin la marca de cuál es la correcta.
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Opcion(long id, String texto) {
    }

    // Un caso de ejemplo: admin-api solo manda los públicos.
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Caso(String entrada, @JsonProperty("salida_esperada") String salidaEsperada) {
    }
}
