package com.mastercode.userapi.admin;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Un problema tal como lo entrega la API interna de admin-api para un estudiante: sin casos ocultos,
// sin respuesta esperada y sin saber cuál alternativa es correcta. Trae el curso porque user-api debe
// comprobar que el estudiante está inscrito en él antes de entregarlo.
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProblemaDto(
        long id,
        String titulo,
        String enunciado,
        String lenguaje,
        String dificultad,
        String plataforma,
        @JsonProperty("tipo_ejercicio") String tipoEjercicio,
        @JsonProperty("casos_prueba") List<Caso> casosPrueba,
        @JsonProperty("curso_id") long cursoId) {

    // Un caso de ejemplo: admin-api solo manda los públicos.
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Caso(String entrada, @JsonProperty("salida_esperada") String salidaEsperada) {
    }
}
