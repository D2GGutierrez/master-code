package com.mastercode.userapi.admin;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Un problema tal como lo entrega la ruta interna de calificación de admin-api (HU36-T2): trae TODOS los casos,
// los ocultos también, porque user-api los necesita para calificar. Solo se usa dentro de user-api: al estudiante
// nunca se le reenvía el contenido de un caso oculto (HU39), solo si pasó o falló.
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProblemaCalificableDto(
        long id,
        @JsonProperty("curso_id") long cursoId,
        String lenguaje,
        @JsonProperty("tipo_ejercicio") String tipoEjercicio,
        @JsonProperty("casos_prueba") List<Caso> casosPrueba) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Caso(
            String entrada,
            @JsonProperty("salida_esperada") String salidaEsperada,
            @JsonProperty("es_publico") boolean esPublico) {
    }
}
