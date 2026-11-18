package com.mastercode.userapi.partida;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Los plazos y valores de la partida, leídos de application.yml (sección «partida»): ninguno está escrito en el código.
@Component
public class ReglasDePartida {

    private final Duration caducidadDelReto;
    private final int ejerciciosPorPartida;
    private final int puntosPorAcierto;
    private final Duration rivalDesconectado;
    private final Duration plazoParaVolver;

    public ReglasDePartida(@Value("${partida.caducidad-reto-horas:24}") long caducidadRetoHoras,
            @Value("${partida.ejercicios-por-partida:5}") int ejerciciosPorPartida,
            @Value("${partida.puntos-por-acierto:10}") int puntosPorAcierto,
            @Value("${partida.segundos-rival-desconectado:10}") long segundosRivalDesconectado,
            @Value("${partida.segundos-para-volver:30}") long segundosParaVolver) {
        this.caducidadDelReto = Duration.ofHours(caducidadRetoHoras);
        this.ejerciciosPorPartida = ejerciciosPorPartida;
        this.puntosPorAcierto = puntosPorAcierto;
        this.rivalDesconectado = Duration.ofSeconds(segundosRivalDesconectado);
        this.plazoParaVolver = Duration.ofSeconds(segundosParaVolver);
    }

    // HU57-T4: lo que tiene el rival para aceptar el reto.
    public Duration caducidadDelReto() {
        return caducidadDelReto;
    }

    // HU58-T1: cuántos ejercicios tiene la secuencia como máximo.
    public int ejerciciosPorPartida() {
        return ejerciciosPorPartida;
    }

    // H9: lo que vale cada acierto.
    public int puntosPorAcierto() {
        return puntosPorAcierto;
    }

    // D20: sin señal por más de esto, el rival ve «se desconectó».
    public Duration rivalDesconectado() {
        return rivalDesconectado;
    }

    // D20: sin señal por más de esto, el jugador pierde la partida por abandono.
    public Duration plazoParaVolver() {
        return plazoParaVolver;
    }
}
