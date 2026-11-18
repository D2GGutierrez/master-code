package com.mastercode.userapi.partida;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.mastercode.userapi.Detalle;
import com.mastercode.userapi.partida.PartidaServicio.Marcador;
import com.mastercode.userapi.perfil.Estudiante;

// Los duelos entre compañeros del curso (ADR-15): retos, juego, marcador, historial y ranking. El estudiante sale del
// token, nunca del cuerpo ni de la ruta. Todas las reglas están en PartidaServicio; aquí solo se traduce a HTTP.
@RestController
public class PartidasController {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record SolicitudDeReto(Long cursoId, Long rivalId) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record SolicitudDeRespuesta(Integer turno, String respuesta, Long opcionId) {
    }

    private final PartidaServicio servicio;

    public PartidasController(PartidaServicio servicio) {
        this.servicio = servicio;
    }

    @ExceptionHandler(ErrorDePartida.class)
    ResponseEntity<Detalle> error(ErrorDePartida e) {
        return ResponseEntity.status(e.estado()).body(new Detalle(e.getMessage()));
    }

    // HU57-T3: a quién se puede retar en el curso.
    @GetMapping("/api/cursos/{cursoId}/companeros")
    public List<PartidaServicio.Companero> companeros(@PathVariable long cursoId,
            @RequestAttribute("estudiante") Estudiante estudiante) {
        return servicio.companeros(estudiante.getId(), cursoId);
    }

    // HU57-T2 y HU58-T1.
    @PostMapping("/api/partidas")
    public ResponseEntity<PartidaServicio.Reto> retar(@RequestBody SolicitudDeReto solicitud,
            @RequestAttribute("estudiante") Estudiante estudiante) {
        if (solicitud.cursoId() == null) {
            throw new ErrorDePartida(HttpStatus.BAD_REQUEST, "curso_id es obligatorio.");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicio.crear(estudiante.getId(), solicitud.cursoId(), solicitud.rivalId()));
    }

    // H7.
    @GetMapping("/api/partidas/invitaciones")
    public List<PartidaServicio.InvitacionVista> invitaciones(@RequestAttribute("estudiante") Estudiante estudiante) {
        return servicio.invitaciones(estudiante.getId());
    }

    @PostMapping("/api/partidas/{partidaId}/aceptar")
    public Marcador aceptar(@PathVariable long partidaId, @RequestAttribute("estudiante") Estudiante estudiante) {
        return servicio.aceptar(estudiante.getId(), partidaId);
    }

    @PostMapping("/api/partidas/{partidaId}/rechazar")
    public ResponseEntity<Void> rechazar(@PathVariable long partidaId, @RequestAttribute("estudiante") Estudiante estudiante) {
        servicio.rechazar(estudiante.getId(), partidaId);
        return ResponseEntity.noContent().build();
    }

    // HU58-T2: el ejercicio del turno. 204 cuando ya respondió todos y espera al rival.
    @GetMapping("/api/partidas/{partidaId}/ejercicio")
    public ResponseEntity<PartidaServicio.EjercicioDelTurno> ejercicio(@PathVariable long partidaId,
            @RequestAttribute("estudiante") Estudiante estudiante) {
        Optional<PartidaServicio.EjercicioDelTurno> ejercicio = servicio.ejercicio(estudiante.getId(), partidaId);
        return ejercicio.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    // HU58-T3 y H9.
    @PostMapping("/api/partidas/{partidaId}/respuesta")
    public PartidaServicio.RespuestaDePartida responder(@PathVariable long partidaId,
            @RequestBody SolicitudDeRespuesta solicitud, @RequestAttribute("estudiante") Estudiante estudiante) {
        return servicio.responder(estudiante.getId(), partidaId, solicitud.turno(), solicitud.respuesta(),
                solicitud.opcionId());
    }

    // HU59-T2 (D20): se sondea cada 2 s. El ETag sale del contenido: si no cambió desde la última consulta, 304 sin
    // cuerpo. Que el rival se conecte o desconecte también cambia el contenido, así que también cambia el ETag.
    @GetMapping("/api/partidas/{partidaId}/marcador")
    public ResponseEntity<Marcador> marcador(@PathVariable long partidaId,
            @RequestHeader(value = "If-None-Match", required = false) String siNoCambio,
            @RequestAttribute("estudiante") Estudiante estudiante) {
        Marcador marcador = servicio.marcador(estudiante.getId(), partidaId);
        String etag = "\"" + huella(marcador) + "\"";
        if (etag.equals(siNoCambio)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).cacheControl(CacheControl.noCache()).build();
        }
        return ResponseEntity.ok().eTag(etag).cacheControl(CacheControl.noCache()).body(marcador);
    }

    // HU60-T1.
    @GetMapping("/api/partidas")
    public PartidaServicio.Pagina<PartidaServicio.PartidaDelHistorial> historial(
            @RequestParam(defaultValue = "1") int pagina, @RequestParam(defaultValue = "20") int tamano,
            @RequestAttribute("estudiante") Estudiante estudiante) {
        return servicio.historial(estudiante.getId(), pagina, tamano);
    }

    // HU60-T2.
    @GetMapping("/api/cursos/{cursoId}/ranking")
    public PartidaServicio.Ranking ranking(@PathVariable long cursoId, @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "20") int tamano, @RequestAttribute("estudiante") Estudiante estudiante) {
        return servicio.ranking(estudiante.getId(), cursoId, pagina, tamano);
    }

    // Los records tienen toString con todos sus campos: dos marcadores iguales dan la misma huella.
    private static String huella(Marcador marcador) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(marcador.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
