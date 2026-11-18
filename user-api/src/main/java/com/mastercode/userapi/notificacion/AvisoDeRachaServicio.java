package com.mastercode.userapi.notificacion;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.mastercode.userapi.perfil.ZonaHorariaRepository;
import com.mastercode.userapi.racha.RachaRepository;

// HU52-T2 (CA-52.1, CA-52.2): avisa a quien está a punto de perder su racha. Una racha está en peligro cuando el
// estudiante practicó ayer pero todavía no hoy (en su día local): si pasa la medianoche sin practicar, la pierde. Se le
// avisa una sola vez por día, desde la hora que eligió (por defecto 20:00, también en su hora local).
@Service
public class AvisoDeRachaServicio {

    private final AvisoAjusteRepository ajustes;
    private final RachaRepository rachas;
    private final ZonaHorariaRepository zonas;
    private final DispositivoRepository dispositivos;
    private final EnviadorDeAvisos enviador;

    public AvisoDeRachaServicio(AvisoAjusteRepository ajustes, RachaRepository rachas, ZonaHorariaRepository zonas,
            DispositivoRepository dispositivos, EnviadorDeAvisos enviador) {
        this.ajustes = ajustes;
        this.rachas = rachas;
        this.zonas = zonas;
        this.dispositivos = dispositivos;
        this.enviador = enviador;
    }

    // La racha que se perdería hoy: la mayor de las vivas que se practicaron ayer, si hoy no practicó en ningún lenguaje.
    // Vacío si no hay nada que avisar. Es una función pura: lo único que mira es lo que se le pasa.
    static Optional<Integer> rachaEnPeligro(List<RachaRepository.Racha> rachas, LocalDate hoy) {
        boolean practicoHoy = rachas.stream()
                .anyMatch(r -> r.fechaUltimaActividad() != null && !r.fechaUltimaActividad().isBefore(hoy));
        if (practicoHoy) {
            return Optional.empty();
        }
        return rachas.stream()
                .filter(r -> r.rachaActual() > 0 && hoy.minusDays(1).equals(r.fechaUltimaActividad()))
                .map(RachaRepository.Racha::rachaActual)
                .max(Integer::compare);
    }

    // ¿Ya es la hora del aviso en la hora local del estudiante, y todavía no se le avisó hoy?
    static boolean tocaAvisar(ZonedDateTime local, LocalTime hora, LocalDate ultimoAviso) {
        return !local.toLocalTime().isBefore(hora) && !local.toLocalDate().equals(ultimoAviso);
    }

    static String mensaje(int racha) {
        return racha == 1
                ? "Llevas 1 día seguido practicando. Resuelve un ejercicio hoy para no perder tu racha."
                : "Llevas " + racha + " días seguidos practicando. Resuelve un ejercicio hoy para no perder tu racha.";
    }

    // Revisa a los candidatos y envía el aviso a los que corresponde. Devuelve a cuántos estudiantes se les avisó. El día
    // solo se marca si algún teléfono recibió el aviso: si el envío falla, la próxima revisión lo vuelve a intentar.
    public int avisarLosQueCorresponde(Instant ahora) {
        LocalDate desde = ahora.atZone(ZoneOffset.UTC).toLocalDate().minusDays(2);
        int avisados = 0;
        for (AvisoAjusteRepository.Candidato candidato : ajustes.candidatos(desde)) {
            ZoneId zona = zonas.de(candidato.estudianteId());
            ZonedDateTime local = ahora.atZone(zona);
            if (!tocaAvisar(local, candidato.hora(), candidato.ultimoAviso())) {
                continue;
            }
            Optional<Integer> racha = rachaEnPeligro(rachas.listar(candidato.estudianteId()), local.toLocalDate());
            if (racha.isEmpty()) {
                continue;
            }
            boolean entregado = false;
            for (String token : dispositivos.tokensDe(candidato.estudianteId())) {
                entregado |= enviador.enviar(token, "Tu racha está en peligro", mensaje(racha.get()));
            }
            if (entregado) {
                ajustes.marcarAviso(candidato.estudianteId(), local.toLocalDate());
                avisados++;
            }
        }
        return avisados;
    }
}
