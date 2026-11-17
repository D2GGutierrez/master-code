package com.mastercode.userapi.ejercicio;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mastercode.userapi.admin.ProblemaDto;
import com.mastercode.userapi.cinturon.CinturonServicio;
import com.mastercode.userapi.dojo.SesionDojoRepository;
import com.mastercode.userapi.dojo.SesionDojoServicio;
import com.mastercode.userapi.envio.EnvioRepository;
import com.mastercode.userapi.racha.RachaServicio;

// HU45-T3: lo que se guarda cuando el celular responde un ejercicio ya corregido. Todo va en una sola transacción: el
// intento (un envío más, D14), el resultado dentro de la sesión del Dojo (HU54) y los puntos del cinturón (HU50-T2) y la racha (HU49-T2). O
// queda todo o no queda nada.
@Service
public class RegistroDeRespuestas {

    // Otra respuesta a la vez ya resolvió el ejercicio dentro de la sesión: se deshace todo lo de esta (rollback).
    public static class SesionSinPendienteException extends RuntimeException {
    }

    private final EnvioRepository envios;
    private final SesionDojoServicio sesiones;
    private final CinturonServicio cinturones;
    private final RachaServicio rachas;

    public RegistroDeRespuestas(EnvioRepository envios, SesionDojoServicio sesiones, CinturonServicio cinturones,
            RachaServicio rachas) {
        this.envios = envios;
        this.sesiones = sesiones;
        this.cinturones = cinturones;
        this.rachas = rachas;
    }

    @Transactional
    public void registrar(long estudianteId, ProblemaDto problema, String respuesta, boolean correcta,
            Optional<SesionDojoRepository.Sesion> sesion, String clave) {
        if (envios.registrarRespuestaMovil(estudianteId, problema.id(), respuesta, correcta, clave).isEmpty()) {
            // La misma clave llegó a la vez desde otra petición: ya estaba guardada, no hay nada más que hacer.
            return;
        }
        if (sesion.isPresent() && !sesiones.registrarRespuesta(sesion.get(), problema.id(), problema.tema(), correcta)) {
            throw new SesionSinPendienteException();
        }
        // HU50-T2: un acierto suma puntos al cinturón del lenguaje del ejercicio (solo la primera vez que lo resuelve).
        if (correcta) {
            cinturones.sumarPorAcierto(estudianteId, problema.lenguaje(), problema.id(), problema.tipoEjercicio());
            // HU49-T2: el acierto mantiene la racha de práctica del día, en la hora local del estudiante.
            rachas.registrarAcierto(estudianteId, problema.lenguaje());
        }
    }
}
