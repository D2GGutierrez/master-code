package com.mastercode.userapi.cinturon;

import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU50-T2: suma al cinturón del lenguaje los puntos de un ejercicio resuelto. Cada ejercicio da puntos solo la primera
// vez que el estudiante lo resuelve; repetirlo no suma. Apuntar el ejercicio y sumar van en la misma transacción: no
// queda un ejercicio «ya contado» sin sus puntos.
@Service
public class CinturonServicio {

    private final CinturonRepository cinturones;

    public CinturonServicio(CinturonRepository cinturones) {
        this.cinturones = cinturones;
    }

    // Devuelve los puntos que se sumaron: 0 si el ejercicio ya los había dado o su tipo no da puntos.
    @Transactional
    public int sumarPorAcierto(long estudianteId, String lenguaje, long problemaId, String tipoEjercicio) {
        int puntos = PuntosPorEjercicio.de(tipoEjercicio);
        if (puntos == 0 || lenguaje == null || lenguaje.isBlank()) {
            return 0;
        }
        // El mismo texto que el lenguaje del problema, sin importar cómo llegue escrito.
        String lenguajeNormalizado = lenguaje.trim().toLowerCase(Locale.ROOT);
        if (!cinturones.registrarPuntosDelEjercicio(estudianteId, problemaId, lenguajeNormalizado, puntos)) {
            return 0;
        }
        cinturones.sumar(estudianteId, lenguajeNormalizado, puntos);
        return puntos;
    }
}
