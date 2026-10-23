package com.mastercode.userapi.sandbox;

import com.fasterxml.jackson.annotation.JsonProperty;

// Mismos valores que el Estado de ai-service (app/providers/sandbox/base.py).
public enum Estado {
    @JsonProperty("ok") OK,
    @JsonProperty("error_ejecucion") ERROR_EJECUCION,
    @JsonProperty("error_compilacion") ERROR_COMPILACION,
    @JsonProperty("tiempo_agotado") TIEMPO_AGOTADO,
    // Falló el motor, no el código del estudiante.
    @JsonProperty("error_servicio") ERROR_SERVICIO
}
