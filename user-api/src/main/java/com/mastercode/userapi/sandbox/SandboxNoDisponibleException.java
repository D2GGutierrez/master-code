package com.mastercode.userapi.sandbox;

// ai-service no respondió, falló (5xx) o devolvió algo ilegible. No es culpa del código del estudiante.
public class SandboxNoDisponibleException extends RuntimeException {

    public SandboxNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
