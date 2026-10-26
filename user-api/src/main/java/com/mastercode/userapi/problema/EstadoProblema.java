package com.mastercode.userapi.problema;

import java.util.Arrays;
import java.util.Optional;

// Estado de un problema para un estudiante (HU30, CA-30.3), calculado solo con sus envíos (APP_ENVIO).
// «Sin abrir» es no tener ningún envío: abrir un problema no deja rastro, solo se guardan los envíos.
public enum EstadoProblema {
    SIN_ABRIR("sin_abrir"),
    INTENTADO("intentado"),
    RESUELTO("resuelto");

    private final String valor;

    EstadoProblema(String valor) {
        this.valor = valor;
    }

    // El texto que viaja en la API y que se acepta en el filtro ?estado=.
    public String valor() {
        return valor;
    }

    // «resuelto» es null si el estudiante no tiene envíos del problema; si los tiene, true cuando alguno fue
    // correcto (basta uno, aunque haya fallado antes o después).
    public static EstadoProblema de(Boolean resuelto) {
        if (resuelto == null) {
            return SIN_ABRIR;
        }
        return resuelto ? RESUELTO : INTENTADO;
    }

    // Estricto: solo los tres textos exactos; cualquier otro valor del filtro es un error del cliente.
    public static Optional<EstadoProblema> deTexto(String texto) {
        return Arrays.stream(values()).filter(estado -> estado.valor.equals(texto)).findFirst();
    }
}
