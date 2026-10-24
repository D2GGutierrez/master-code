package com.mastercode.userapi.admin;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Una fila del listado de problemas de un curso, tal como la entrega la API interna de admin-api (JSON en
// snake_case). Solo trae problemas que un estudiante puede ver: el filtro de aprobados se aplica allá (HU31).
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProblemaResumenDto(
        long id,
        String titulo,
        String lenguaje,
        String dificultad,
        String plataforma,
        @JsonProperty("tipo_ejercicio") String tipoEjercicio,
        @JsonProperty("fecha_publicacion") OffsetDateTime fechaPublicacion) {
}
