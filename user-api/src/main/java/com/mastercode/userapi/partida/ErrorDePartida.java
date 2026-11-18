package com.mastercode.userapi.partida;

import org.springframework.http.HttpStatus;

// Una regla de la partida que no se cumple: se traduce a la respuesta HTTP con su mensaje. Se lanza solo cuando lo que ya
// se escribió (la señal del jugador, un cierre por abandono) debe quedar: PartidaServicio no deshace por esta excepción.
public class ErrorDePartida extends RuntimeException {

    private final HttpStatus estado;

    public ErrorDePartida(HttpStatus estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public HttpStatus estado() {
        return estado;
    }
}
