package com.mastercode.userapi.sandbox;

// El motor activo no tiene el lenguaje pedido (ai-service responde 400).
public class LenguajeNoSoportadoException extends RuntimeException {

    public LenguajeNoSoportadoException(String lenguaje) {
        super("El sandbox no soporta el lenguaje: " + lenguaje);
    }
}
