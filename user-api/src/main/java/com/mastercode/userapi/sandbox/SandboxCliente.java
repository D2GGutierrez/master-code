package com.mastercode.userapi.sandbox;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// Cliente de ai-service para ejecutar código (ADR-7 D1). user-api no sabe qué motor hay detrás
// (Judge0, Piston o mock): eso lo decide ai-service con una variable de entorno.
public class SandboxCliente {

    private final RestClient http;

    public SandboxCliente(RestClient http) {
        this.http = http;
    }

    // Un código del estudiante que falla NO lanza excepción: llega como Estado.ERROR_EJECUCION, etc.
    public EjecucionResultado ejecutar(EjecucionSolicitud solicitud) {
        EjecucionResultado resultado;
        try {
            resultado = http.post()
                    .uri("/execute")
                    .body(solicitud)
                    .retrieve()
                    .onStatus(HttpStatus.BAD_REQUEST::equals, (peticion, respuesta) -> {
                        throw new LenguajeNoSoportadoException(solicitud.lenguaje());
                    })
                    .body(EjecucionResultado.class);
        } catch (RestClientException e) {
            // Sin conexión, tiempo agotado, 5xx o cuerpo que no se puede leer.
            throw new SandboxNoDisponibleException("ai-service no pudo ejecutar el código", e);
        }
        if (resultado == null || resultado.estado() == null) {
            throw new SandboxNoDisponibleException("ai-service devolvió una respuesta vacía", null);
        }
        return resultado;
    }
}
