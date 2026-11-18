package com.mastercode.userapi.partida;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.mastercode.userapi.admin.AdminCliente;
import com.mastercode.userapi.admin.AdminNoDisponibleException;
import com.mastercode.userapi.admin.CursoNoExisteException;
import com.mastercode.userapi.admin.ProblemaDto;
import com.mastercode.userapi.admin.ProblemaNoVisibleException;
import com.mastercode.userapi.admin.ProblemaResumenDto;
import com.mastercode.userapi.admin.RespuestaDelProblemaDto;
import com.mastercode.userapi.cinturon.CinturonServicio;
import com.mastercode.userapi.ejercicio.CorrectorDeEjercicios;
import com.mastercode.userapi.ejercicio.RespuestaInvalidaException;
import com.mastercode.userapi.ejercicio.Veredicto;
import com.mastercode.userapi.inscripcion.InscripcionRepository;
import com.mastercode.userapi.partida.PartidaRepository.Jugador;
import com.mastercode.userapi.partida.PartidaRepository.Partida;
import com.mastercode.userapi.problema.ProblemasController;
import com.mastercode.userapi.racha.RachaServicio;

// Los duelos (ADR-15): retar a un compañero, aceptar, jugar la misma secuencia de ejercicios, marcador con desconexión y
// cierre. Las reglas de tiempo se evalúan al consultar (D20): no hay ningún proceso que cierre partidas por su cuenta.
// noRollbackFor: lo que ya se escribió antes de lanzar un ErrorDePartida (la señal del jugador, el cierre por abandono)
// debe quedar; las llamadas a admin-api van antes de escribir, así que un error de ellas no deja nada a medias.
@Service
@Transactional(noRollbackFor = ErrorDePartida.class)
public class PartidaServicio {

    // ---------- lo que sale hacia la app ----------

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Reto(long id, long cursoId, String estado, String rival, int totalEjercicios) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record InvitacionVista(long id, long cursoId, String retador, OffsetDateTime fechaCreacion,
            OffsetDateTime venceEn) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Companero(long id, String alias) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Marcadores(String alias, int puntaje, int respondidos) {
    }

    // resultado y motivoFin salen null mientras la partida no termina; resultado es lo que ve quien pide: gane, perdi o
    // empate.
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Marcador(long partidaId, long cursoId, String estado, int totalEjercicios, Marcadores yo,
            Marcadores rival, boolean rivalConectado, String resultado, String motivoFin) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EjercicioDelTurno(int turno, int totalEjercicios, ProblemasController.ProblemaDetalle ejercicio) {
    }

    // respuestaCorrecta solo sale si falló (como en el Dojo).
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RespuestaDePartida(String resultado, String respuestaCorrecta, Marcador marcador) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record PartidaDelHistorial(long id, long cursoId, String estado, String rival, int miPuntaje,
            int puntajeRival, String resultado, String motivoFin, OffsetDateTime fechaInicio, OffsetDateTime fechaFin) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Pagina<T>(List<T> items, int pagina, int tamano, int total) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EntradaDeRanking(int posicion, String alias, int jugadas, int ganadas, int empatadas, int perdidas,
            int puntos, double porcentajeVictorias, boolean soyYo) {
    }

    // yo es la entrada del estudiante que pide, esté o no en la página devuelta.
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Ranking(long cursoId, int total, int pagina, int tamano, List<EntradaDeRanking> items,
            EntradaDeRanking yo) {
    }

    // Lo que se carga de una partida para decidir: ella, sus dos jugadores y cuál es quien pide.
    private record Contexto(Partida partida, Jugador yo, Jugador rival) {
    }

    static final int TAMANO_MAXIMO = 50;
    private static final String TIPO_DE_CODIGO = "escritura_codigo";
    private static final String EN_CURSO = "en_curso";

    private final PartidaRepository partidas;
    private final InscripcionRepository inscripciones;
    private final AdminCliente admin;
    private final CorrectorDeEjercicios corrector;
    private final CinturonServicio cinturones;
    private final RachaServicio rachas;
    private final ReglasDePartida reglas;
    private final Clock reloj;

    public PartidaServicio(PartidaRepository partidas, InscripcionRepository inscripciones, AdminCliente admin,
            CorrectorDeEjercicios corrector, CinturonServicio cinturones, RachaServicio rachas, ReglasDePartida reglas,
            Clock relojDePartida) {
        this.partidas = partidas;
        this.inscripciones = inscripciones;
        this.admin = admin;
        this.corrector = corrector;
        this.cinturones = cinturones;
        this.rachas = rachas;
        this.reglas = reglas;
        this.reloj = relojDePartida;
    }

    // ---------- retos (HU57-T2, HU58-T1, H7) ----------

    // HU57-T3: los compañeros a quienes retar: los demás inscritos del curso. Solo con alias, nunca el correo.
    public List<Companero> companeros(long estudianteId, long cursoId) {
        exigirInscrito(estudianteId, cursoId);
        return partidas.companeros(cursoId, estudianteId).stream()
                .map(c -> new Companero(c.id(), Alias.de(c.email()))).toList();
    }

    // HU57-T2 y HU58-T1: crea la partida pendiente, invita al rival y guarda la secuencia de ejercicios.
    public Reto crear(long estudianteId, long cursoId, Long rivalId) {
        if (rivalId == null) {
            throw new ErrorDePartida(HttpStatus.BAD_REQUEST, "rival_id es obligatorio.");
        }
        if (rivalId == estudianteId) {
            throw new ErrorDePartida(HttpStatus.BAD_REQUEST, "No puedes retarte a ti mismo.");
        }
        exigirInscrito(estudianteId, cursoId);
        if (!inscripciones.estaInscrito(rivalId, cursoId)) {
            throw new ErrorDePartida(HttpStatus.BAD_REQUEST, "Ese compañero no está inscrito en el curso.");
        }
        Instant ahora = Instant.now(reloj);
        if (partidas.hayRetoVivo(cursoId, estudianteId, rivalId, limiteDeCaducidad(ahora))) {
            throw new ErrorDePartida(HttpStatus.CONFLICT, "Ya tienes un reto o una partida en curso con ese compañero.");
        }
        List<Long> secuencia = secuenciaDelCurso(cursoId);
        long id = partidas.crear(cursoId, estudianteId, rivalId, secuencia, ahora);
        Contexto c = cargar(id, estudianteId, ahora);
        return new Reto(id, cursoId, c.partida().estado(), Alias.de(c.rival().email()), secuencia.size());
    }

    // Los ejercicios del celular del curso, mezclados, hasta el máximo de la partida. Los de código no entran: no se
    // corrigen sin el sandbox (H2).
    private List<Long> secuenciaDelCurso(long cursoId) {
        List<ProblemaResumenDto> problemas;
        try {
            problemas = admin.problemasDelCurso(cursoId, "movil");
        } catch (CursoNoExisteException e) {
            throw new ErrorDePartida(HttpStatus.NOT_FOUND, "Curso no encontrado.");
        } catch (AdminNoDisponibleException e) {
            throw new ErrorDePartida(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo preparar la partida en este momento. Intenta de nuevo.");
        }
        List<Long> ids = new ArrayList<>(problemas.stream()
                .filter(p -> p.tipoEjercicio() != null && !TIPO_DE_CODIGO.equals(p.tipoEjercicio()))
                .map(ProblemaResumenDto::id).toList());
        if (ids.isEmpty()) {
            throw new ErrorDePartida(HttpStatus.CONFLICT, "El curso todavía no tiene ejercicios para un duelo.");
        }
        Collections.shuffle(ids);
        return new ArrayList<>(ids.subList(0, Math.min(ids.size(), reglas.ejerciciosPorPartida())));
    }

    // H7: los retos que le hicieron al estudiante y siguen por responder (los vencidos ya no salen).
    public List<InvitacionVista> invitaciones(long estudianteId) {
        Instant ahora = Instant.now(reloj);
        return partidas.invitaciones(estudianteId, limiteDeCaducidad(ahora)).stream()
                .map(i -> new InvitacionVista(i.id(), i.cursoId(), Alias.de(i.emailRetador()), i.fechaCreacion(),
                        i.fechaCreacion().plus(reglas.caducidadDelReto())))
                .toList();
    }

    // H7: el invitado acepta. Los dos jugadores empiezan con señal de ahora (ver PartidaRepository.aceptar).
    public Marcador aceptar(long estudianteId, long partidaId) {
        Instant ahora = Instant.now(reloj);
        Contexto c = responderAlReto(estudianteId, partidaId, ahora);
        partidas.aceptar(c.partida().id(), ahora);
        return vista(cargar(partidaId, estudianteId, ahora), ahora);
    }

    public void rechazar(long estudianteId, long partidaId) {
        Instant ahora = Instant.now(reloj);
        Contexto c = responderAlReto(estudianteId, partidaId, ahora);
        partidas.rechazar(c.partida().id(), ahora);
    }

    // Quien responde a un reto debe ser el invitado, y el reto seguir pendiente (no vencido, no ya respondido).
    private Contexto responderAlReto(long estudianteId, long partidaId, Instant ahora) {
        partidas.bloquear(partidaId);
        Contexto c = cargar(partidaId, estudianteId, ahora);
        if (c.yo().esRetador()) {
            throw new ErrorDePartida(HttpStatus.FORBIDDEN, "Solo el compañero retado puede responder al reto.");
        }
        switch (c.partida().estado()) {
            case "pendiente" -> {
            }
            case "caducada" -> throw new ErrorDePartida(HttpStatus.CONFLICT, "El reto caducó.");
            default -> throw new ErrorDePartida(HttpStatus.CONFLICT, "Este reto ya no está disponible.");
        }
        return c;
    }

    // ---------- el juego (HU58-T2, HU58-T3, H9) ----------

    // HU58-T2: el ejercicio que le toca al estudiante: el de su turno, el mismo que le toca al rival cuando llegue a él.
    // Pedirlo de nuevo no avanza nada. Vacío cuando ya respondió todos y espera al rival.
    public Optional<EjercicioDelTurno> ejercicio(long estudianteId, long partidaId) {
        Instant ahora = Instant.now(reloj);
        Contexto c = entrarEnPartida(estudianteId, partidaId, ahora);
        int total = partidas.totalDeEjercicios(partidaId);
        int turno = c.yo().respondidos() + 1;
        if (turno > total) {
            return Optional.empty();
        }
        long problemaId = partidas.ejercicioDelTurno(partidaId, turno).orElseThrow();
        ProblemaDto problema;
        try {
            problema = admin.problema(problemaId);
        } catch (ProblemaNoVisibleException e) {
            throw new ErrorDePartida(HttpStatus.CONFLICT, "Este ejercicio ya no está disponible.");
        } catch (AdminNoDisponibleException e) {
            throw noDisponible();
        }
        return Optional.of(new EjercicioDelTurno(turno, total, ProblemasController.detalleDe(problema)));
    }

    // HU58-T3, H9: responde el ejercicio del turno. El turno debe ser exactamente el siguiente del jugador: no puede
    // saltarse uno, adelantarse al rival en la secuencia ni repetir uno ya respondido. Cuenta para la racha y el cinturón
    // como cualquier acierto, pero no gasta intentos del Dojo ni guarda un envío: la partida guarda sus propias filas.
    public RespuestaDePartida responder(long estudianteId, long partidaId, Integer turno, String respuesta,
            Long opcionId) {
        Instant ahora = Instant.now(reloj);
        Contexto previo = cargar(partidaId, estudianteId, ahora);
        exigirEnCurso(previo.partida());
        if (turno == null) {
            throw new ErrorDePartida(HttpStatus.BAD_REQUEST, "turno es obligatorio.");
        }
        long problemaId = partidas.ejercicioDelTurno(partidaId, turno)
                .orElseThrow(() -> new ErrorDePartida(HttpStatus.BAD_REQUEST, "Ese turno no existe en la partida."));

        // Todo lo de admin-api y la corrección van antes de escribir: si algo falla no queda nada a medias.
        ProblemaDto problema;
        Veredicto veredicto;
        try {
            problema = admin.problema(problemaId);
            RespuestaDelProblemaDto paraCorregir = admin.respuestaDelProblema(problemaId);
            veredicto = corrector.corregir(paraCorregir, respuesta, opcionId);
        } catch (ProblemaNoVisibleException e) {
            throw new ErrorDePartida(HttpStatus.CONFLICT, "Este ejercicio ya no está disponible.");
        } catch (AdminNoDisponibleException e) {
            throw noDisponible();
        } catch (RespuestaInvalidaException e) {
            throw new ErrorDePartida(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        Contexto c = entrarEnPartida(estudianteId, partidaId, ahora);
        int esperado = c.yo().respondidos() + 1;
        if (turno < esperado) {
            throw new ErrorDePartida(HttpStatus.CONFLICT, "Ya respondiste ese ejercicio.");
        }
        if (turno > esperado) {
            throw new ErrorDePartida(HttpStatus.CONFLICT, "Ese no es tu ejercicio actual: responde el turno " + esperado + ".");
        }
        if (!partidas.registrarRespuesta(partidaId, estudianteId, turno, veredicto.correcta())) {
            throw new ErrorDePartida(HttpStatus.CONFLICT, "Ya respondiste ese ejercicio.");
        }
        partidas.sumarAlMarcador(partidaId, estudianteId, veredicto.correcta() ? reglas.puntosPorAcierto() : 0);
        if (veredicto.correcta()) {
            cinturones.sumarPorAcierto(estudianteId, problema.lenguaje(), problema.id(), problema.tipoEjercicio());
            rachas.registrarAcierto(estudianteId, problema.lenguaje());
        }
        cerrarSiTerminaron(partidaId, ahora);
        return new RespuestaDePartida(veredicto.correcta() ? "correcto" : "incorrecto", veredicto.respuestaCorrecta(),
                vista(cargar(partidaId, estudianteId, ahora), ahora));
    }

    // H9: cuando los dos respondieron todos los ejercicios, gana el de mayor puntaje; con el mismo puntaje es empate.
    private void cerrarSiTerminaron(long partidaId, Instant ahora) {
        int total = partidas.totalDeEjercicios(partidaId);
        List<Jugador> jugadores = partidas.jugadores(partidaId);
        if (jugadores.stream().anyMatch(j -> j.respondidos() < total)) {
            return;
        }
        Jugador a = jugadores.get(0);
        Jugador b = jugadores.get(1);
        Long ganador = a.puntaje() == b.puntaje() ? null : (a.puntaje() > b.puntaje() ? a.estudianteId() : b.estudianteId());
        partidas.cerrar(partidaId, ganador, "completada", ahora);
    }

    // ---------- marcador y desconexión (HU59-T2, HU59-T4) ----------

    // HU59-T2: el marcador de la partida, para sondear cada 2 s. Cada consulta de un jugador deja su señal (D20).
    // Una partida que no está en curso solo se lee: no hay nada que vigilar.
    public Marcador marcador(long estudianteId, long partidaId) {
        Instant ahora = Instant.now(reloj);
        Contexto c = cargar(partidaId, estudianteId, ahora);
        if (EN_CURSO.equals(c.partida().estado())) {
            partidas.bloquear(partidaId);
            c = cargar(partidaId, estudianteId, ahora);
            if (EN_CURSO.equals(c.partida().estado())) {
                if (cerrarPorAbandono(c, ahora)) {
                    c = cargar(partidaId, estudianteId, ahora);
                } else {
                    partidas.senal(partidaId, estudianteId, ahora);
                }
            }
        }
        return vista(c, ahora);
    }

    // Entra a una acción de juego: bloquea la partida, comprueba que siga en curso y que nadie haya abandonado, y deja la
    // señal del jugador. Si el rival (o el propio jugador, tras volver tarde) pasó el plazo, la partida se cierra aquí.
    private Contexto entrarEnPartida(long estudianteId, long partidaId, Instant ahora) {
        partidas.bloquear(partidaId);
        Contexto c = cargar(partidaId, estudianteId, ahora);
        exigirEnCurso(c.partida());
        if (cerrarPorAbandono(c, ahora)) {
            throw new ErrorDePartida(HttpStatus.CONFLICT, "La partida terminó por abandono.");
        }
        partidas.senal(partidaId, estudianteId, ahora);
        return c;
    }

    // D20: pasados 30 s desde su última señal un jugador abandonó. Si abandonó uno, gana el otro; si los dos, empate.
    // Se evalúa antes de dejar la señal de quien consulta, así quien vuelve tarde no se salva solo por volver.
    private boolean cerrarPorAbandono(Contexto c, Instant ahora) {
        boolean yoAusente = ausente(c.yo(), c.partida(), ahora);
        boolean rivalAusente = ausente(c.rival(), c.partida(), ahora);
        if (!yoAusente && !rivalAusente) {
            return false;
        }
        Long ganador = yoAusente && rivalAusente ? null : (yoAusente ? c.rival().estudianteId() : c.yo().estudianteId());
        return partidas.cerrar(c.partida().id(), ganador, "abandono", ahora);
    }

    private boolean ausente(Jugador jugador, Partida partida, Instant ahora) {
        OffsetDateTime senal = jugador.ultimaSenal() != null ? jugador.ultimaSenal() : partida.fechaInicio();
        return senal != null && senal.toInstant().plus(reglas.plazoParaVolver()).isBefore(ahora);
    }

    private boolean rivalConectado(Contexto c, Instant ahora) {
        OffsetDateTime senal = c.rival().ultimaSenal();
        return EN_CURSO.equals(c.partida().estado()) && senal != null
                && !senal.toInstant().plus(reglas.rivalDesconectado()).isBefore(ahora);
    }

    // ---------- historial y ranking (HU60-T1, HU60-T2) ----------

    // HU60-T1: las partidas del estudiante (en curso y terminadas), la más reciente primero.
    public Pagina<PartidaDelHistorial> historial(long estudianteId, int pagina, int tamano) {
        int p = Math.max(pagina, 1);
        int t = Math.min(Math.max(tamano, 1), TAMANO_MAXIMO);
        List<PartidaDelHistorial> items = partidas.historial(estudianteId, t, (p - 1) * t).stream()
                .map(f -> new PartidaDelHistorial(f.id(), f.cursoId(), f.estado(), Alias.de(f.emailRival()), f.miPuntaje(),
                        f.puntajeRival(), resultadoDe(f.estado(), f.ganadorId(), estudianteId), f.motivoFin(),
                        f.fechaInicio(), f.fechaFin()))
                .toList();
        return new Pagina<>(items, p, t, partidas.totalDeHistorial(estudianteId));
    }

    // HU60-T2: el ranking del curso por partidas ganadas. Solo lo ven los inscritos. Siempre trae la entrada del propio
    // estudiante, aunque su posición caiga en otra página.
    public Ranking ranking(long estudianteId, long cursoId, int pagina, int tamano) {
        exigirInscrito(estudianteId, cursoId);
        int p = Math.max(pagina, 1);
        int t = Math.min(Math.max(tamano, 1), TAMANO_MAXIMO);
        List<EntradaDeRanking> todas = partidas.ranking(cursoId).stream()
                .map(f -> new EntradaDeRanking((int) f.posicion(), Alias.de(f.email()), f.jugadas(), f.ganadas(),
                        f.empatadas(), f.perdidas(), f.puntos(), Math.round(f.tasaDeVictorias() * 1000) / 10.0,
                        f.estudianteId() == estudianteId))
                .toList();
        int desde = Math.min((p - 1) * t, todas.size());
        List<EntradaDeRanking> items = todas.subList(desde, Math.min(desde + t, todas.size()));
        EntradaDeRanking yo = todas.stream().filter(EntradaDeRanking::soyYo).findFirst().orElse(null);
        return new Ranking(cursoId, todas.size(), p, t, items, yo);
    }

    // ---------- apoyo ----------

    private void exigirInscrito(long estudianteId, long cursoId) {
        if (!inscripciones.estaInscrito(estudianteId, cursoId)) {
            throw new ErrorDePartida(HttpStatus.FORBIDDEN, "No estás inscrito en este curso.");
        }
    }

    private void exigirEnCurso(Partida partida) {
        if (EN_CURSO.equals(partida.estado())) {
            return;
        }
        String mensaje = switch (partida.estado()) {
            case "pendiente" -> "Tu rival todavía no aceptó el reto.";
            case "terminada" -> "La partida ya terminó.";
            default -> "Esta partida no está disponible.";
        };
        throw new ErrorDePartida(HttpStatus.CONFLICT, mensaje);
    }

    private static ErrorDePartida noDisponible() {
        return new ErrorDePartida(HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo cargar el ejercicio en este momento. Intenta de nuevo.");
    }

    private Instant limiteDeCaducidad(Instant ahora) {
        return ahora.minus(reglas.caducidadDelReto());
    }

    // Quien no juega la partida ve lo mismo que si no existiera: 404.
    private Contexto cargar(long partidaId, long estudianteId, Instant ahora) {
        Partida partida = partidas.buscar(partidaId, limiteDeCaducidad(ahora)).orElseThrow(PartidaServicio::noEncontrada);
        List<Jugador> jugadores = partidas.jugadores(partidaId);
        Jugador yo = jugadores.stream().filter(j -> j.estudianteId() == estudianteId).findFirst()
                .orElseThrow(PartidaServicio::noEncontrada);
        Jugador rival = jugadores.stream().filter(j -> j.estudianteId() != estudianteId).findFirst()
                .orElseThrow(PartidaServicio::noEncontrada);
        return new Contexto(partida, yo, rival);
    }

    private static ErrorDePartida noEncontrada() {
        return new ErrorDePartida(HttpStatus.NOT_FOUND, "Partida no encontrada.");
    }

    private Marcador vista(Contexto c, Instant ahora) {
        Partida p = c.partida();
        return new Marcador(p.id(), p.cursoId(), p.estado(), partidas.totalDeEjercicios(p.id()),
                new Marcadores(Alias.de(c.yo().email()), c.yo().puntaje(), c.yo().respondidos()),
                new Marcadores(Alias.de(c.rival().email()), c.rival().puntaje(), c.rival().respondidos()),
                rivalConectado(c, ahora), resultadoDe(p.estado(), p.ganadorId(), c.yo().estudianteId()), p.motivoFin());
    }

    // gane, perdi o empate para quien pide; null mientras la partida no terminó.
    private static String resultadoDe(String estado, Long ganadorId, long estudianteId) {
        if (!"terminada".equals(estado)) {
            return null;
        }
        if (ganadorId == null) {
            return "empate";
        }
        return ganadorId == estudianteId ? "gane" : "perdi";
    }
}
