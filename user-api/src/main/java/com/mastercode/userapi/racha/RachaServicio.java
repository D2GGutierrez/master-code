package com.mastercode.userapi.racha;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mastercode.userapi.perfil.ZonaHorariaRepository;

// HU49-T2 y HU49-T4 (ADR-13, D23): la racha de práctica, en días seguidos, con el día que es para el estudiante (su zona
// horaria, H6) y no el del servidor.
//
// Qué cuenta (HU49-T4, CA-49.1): un ACIERTO ese día, en el celular o en la web. Un intento fallido no mantiene la racha.
// Cuándo se corta (CA-49.4): al pasar un día completo sin ningún acierto. Si el último acierto fue ayer, la racha sigue
// viva (todavía puede sumarse hoy); si fue antes de ayer, vale cero.
@Service
public class RachaServicio {

    // La racha de un lenguaje tal como se muestra hoy: viva o ya cortada (rachaActual 0), y si ya practicó hoy.
    public record RachaDelDia(String lenguaje, int rachaActual, int rachaMaxima, boolean practicoHoy) {
    }

    private final RachaRepository rachas;
    private final ZonaHorariaRepository zonas;
    private final Clock reloj;

    public RachaServicio(RachaRepository rachas, ZonaHorariaRepository zonas, Clock relojDeRacha) {
        this.rachas = rachas;
        this.zonas = zonas;
        this.reloj = relojDeRacha;
    }

    // Se llama al registrar un acierto, en la misma transacción que lo guarda.
    @Transactional
    public void registrarAcierto(long estudianteId, String lenguaje) {
        registrarAcierto(estudianteId, lenguaje, reloj.instant());
    }

    // El instante se puede dar: así las pruebas eligen la hora sin esperar al reloj.
    @Transactional
    public void registrarAcierto(long estudianteId, String lenguaje, Instant instante) {
        if (lenguaje == null || lenguaje.isBlank()) {
            return;
        }
        LocalDate hoy = diaLocal(estudianteId, instante);
        rachas.registrarActividad(estudianteId, lenguaje.trim().toLowerCase(Locale.ROOT), hoy);
    }

    public List<RachaDelDia> delDia(long estudianteId) {
        return delDia(estudianteId, reloj.instant());
    }

    public List<RachaDelDia> delDia(long estudianteId, Instant instante) {
        LocalDate hoy = diaLocal(estudianteId, instante);
        return rachas.listar(estudianteId).stream()
                .map(r -> delDia(r, hoy))
                .toList();
    }

    // Regla pura (sin base de datos): la racha guardada, vista desde el día «hoy».
    static RachaDelDia delDia(RachaRepository.Racha guardada, LocalDate hoy) {
        LocalDate ultimo = guardada.fechaUltimaActividad();
        boolean practicoHoy = ultimo != null && !ultimo.isBefore(hoy);
        // Viva: practicó hoy o ayer. Antes de ayer pasó al menos un día completo sin acierto.
        boolean viva = ultimo != null && !ultimo.isBefore(hoy.minusDays(1));
        return new RachaDelDia(guardada.lenguaje(), viva ? guardada.rachaActual() : 0, guardada.rachaMaxima(), practicoHoy);
    }

    // El día en la zona del estudiante: 23:30 en Lima ya es el día siguiente en UTC, pero para él sigue siendo hoy.
    LocalDate diaLocal(long estudianteId, Instant instante) {
        ZoneId zona = zonas.de(estudianteId);
        return instante.atZone(zona).toLocalDate();
    }
}
