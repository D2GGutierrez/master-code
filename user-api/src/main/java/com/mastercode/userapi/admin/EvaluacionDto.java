package com.mastercode.userapi.admin;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Una evaluación tal como la entrega la API interna de admin-api (HU64-T2, JSON en snake_case): la ventana, el límite
// de tiempo, el estado que admin-api calcula comparando la ventana con su hora y los ids de los problemas que un
// estudiante puede ver hoy (solo ids, nada del contenido).
@JsonIgnoreProperties(ignoreUnknown = true)
public record EvaluacionDto(
        long id,
        @JsonProperty("curso_id") long cursoId,
        @JsonProperty("fecha_inicio") OffsetDateTime fechaInicio,
        @JsonProperty("fecha_fin") OffsetDateTime fechaFin,
        @JsonProperty("limite_minutos") int limiteMinutos,
        String estado,
        List<Long> problemas) {
}
