package com.mastercode.userapi.ejercicio;

// La respuesta del estudiante no se puede corregir (falta, es muy larga, no corresponde al ejercicio). El mensaje se
// muestra tal cual al estudiante: es un error 400, no un resultado incorrecto.
public class RespuestaInvalidaException extends RuntimeException {

    public RespuestaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
