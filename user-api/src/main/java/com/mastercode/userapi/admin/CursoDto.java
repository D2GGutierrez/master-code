package com.mastercode.userapi.admin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Un curso tal como lo entrega la API interna de admin-api. user-api no tiene la tabla (ADR-1).
// Ignora campos desconocidos: admin-api puede agregar datos sin romper a user-api.
@JsonIgnoreProperties(ignoreUnknown = true)
public record CursoDto(long id, String nombre, String lenguaje, String institucion) {
}
