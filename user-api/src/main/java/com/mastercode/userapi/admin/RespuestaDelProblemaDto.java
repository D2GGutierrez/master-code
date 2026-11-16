package com.mastercode.userapi.admin;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Un problema tal como lo entrega la ruta interna de respuesta de admin-api (H1): trae la respuesta esperada y las
// alternativas CON su marca de cuál es la correcta, porque user-api las necesita para corregir. Solo se usa dentro de
// user-api: al estudiante nunca se le reenvía, ni antes de responder (CA-46.4) ni la marca de las alternativas.
@JsonIgnoreProperties(ignoreUnknown = true)
public record RespuestaDelProblemaDto(
        long id,
        @JsonProperty("curso_id") long cursoId,
        @JsonProperty("tipo_ejercicio") String tipoEjercicio,
        @JsonProperty("respuesta_esperada") String respuestaEsperada,
        List<Opcion> opciones) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Opcion(long id, String texto, @JsonProperty("es_correcta") boolean esCorrecta) {
    }
}
