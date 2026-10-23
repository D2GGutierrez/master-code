package com.mastercode.userapi.sandbox;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Respuesta de POST /execute. tiempoMs es null si el motor no informó el tiempo.
// Ignora campos desconocidos: ai-service puede agregar datos sin romper a user-api.
@JsonIgnoreProperties(ignoreUnknown = true)
public record EjecucionResultado(
        Estado estado,
        String salida,
        String error,
        @JsonProperty("tiempo_ms") Long tiempoMs) {
}
