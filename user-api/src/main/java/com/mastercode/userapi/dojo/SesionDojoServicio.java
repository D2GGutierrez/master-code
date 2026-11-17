package com.mastercode.userapi.dojo;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mastercode.userapi.cinturon.CinturonRepository;

// HU54 y HU56: lo que pasa con una sesión del Dojo además de entregar ejercicios. Cada respuesta corregida mueve el nivel
// según la regla (HU54-T2); al terminar se guarda el nivel (HU54-T3) y se arma el resumen con los puntos que la sesión
// sumó al cinturón (HU56-T3) y los temas donde más falló (HU56-T1).
@Service
public class SesionDojoServicio {

    // Cuántos temas muestra el resumen.
    static final int TEMAS_EN_EL_RESUMEN = 3;
    static final String TEMA_GENERAL = "General";

    // El cinturón que llevaba el estudiante al terminar; vacío si todavía no tiene (no sumó puntos en ese lenguaje).
    public record CinturonActual(int nivel, int puntos) {
    }

    public record Resumen(String nivelAlcanzado, int respondidos, int aciertos, int puntosSumados,
            List<SesionDojoRepository.TemaFallado> temasMasFallados, Optional<CinturonActual> cinturon) {
    }

    private final SesionDojoRepository sesiones;
    private final CinturonRepository cinturones;

    public SesionDojoServicio(SesionDojoRepository sesiones, CinturonRepository cinturones) {
        this.sesiones = sesiones;
        this.cinturones = cinturones;
    }

    // El nivel con el que empieza una sesión nueva: el guardado al terminar la última, o «facil» si el estudiante es
    // nuevo en ese lenguaje (HU54-T4).
    public Nivel nivelInicial(long estudianteId, String lenguaje) {
        return sesiones.nivelGuardado(estudianteId, lenguaje).orElse(Nivel.INICIAL);
    }

    // Apunta una respuesta corregida y mueve el nivel. false si el ejercicio ya no estaba pendiente en la sesión.
    @Transactional
    public boolean registrarRespuesta(SesionDojoRepository.Sesion sesion, long problemaId, String tema, boolean acerto) {
        String temaLimpio = tema == null || tema.isBlank() ? TEMA_GENERAL : tema.trim();
        if (!sesiones.registrarRespuesta(sesion.id(), problemaId, acerto, temaLimpio)) {
            return false;
        }
        // Se lee la sesión justo antes de mover el nivel, para partir del estado más reciente.
        SesionDojoRepository.Sesion actual = sesiones.buscar(sesion.id(), sesion.estudianteId()).orElse(sesion);
        sesiones.guardarEstado(sesion.id(), ReglaDificultad.aplicar(actual.estado(), acerto));
        return true;
    }

    // Cierra la sesión, guarda el nivel y devuelve el resumen. Vacío si ya estaba cerrada.
    @Transactional
    public Optional<Resumen> terminar(SesionDojoRepository.Sesion sesion) {
        if (!sesiones.cerrar(sesion.id())) {
            return Optional.empty();
        }
        // El estado se lee después de cerrar: ya no puede cambiar (las respuestas exigen sesión abierta).
        Nivel nivel = sesiones.buscar(sesion.id(), sesion.estudianteId()).orElse(sesion).estado().nivel();
        sesiones.guardarNivel(sesion.estudianteId(), sesion.lenguaje(), nivel);
        Optional<CinturonActual> cinturon = cinturones.buscar(sesion.estudianteId(), sesion.lenguaje().toLowerCase(Locale.ROOT))
                .map(c -> new CinturonActual(c.nivel(), c.puntos()));
        return Optional.of(new Resumen(nivel.valor(), sesiones.respondidos(sesion.id()), sesiones.aciertos(sesion.id()),
                sesiones.puntosDeLaSesion(sesion.id()), sesiones.temasMasFallados(sesion.id(), TEMAS_EN_EL_RESUMEN),
                cinturon));
    }
}
