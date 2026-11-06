package com.mastercode.userapi.evaluacion;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.mastercode.userapi.Detalle;
import com.mastercode.userapi.admin.AdminCliente;
import com.mastercode.userapi.admin.AdminNoDisponibleException;
import com.mastercode.userapi.admin.EvaluacionDto;
import com.mastercode.userapi.admin.EvaluacionNoExisteException;
import com.mastercode.userapi.inscripcion.InscripcionRepository;
import com.mastercode.userapi.perfil.Estudiante;

// HU64-T2: entrar a una evaluación solo se puede dentro de su ventana. Quién es el estudiante sale del token; la
// evaluación, su ventana y su estado salen de admin-api, que es quien las tiene (ADR-1, D11) y calcula el estado en un
// solo lugar (HU64-T1).
@RestController
public class EvaluacionesController {

    private static final String ACTIVA = "activa";
    private static final String PROGRAMADA = "programada";

    // Lo que ve el estudiante al entrar: la ventana, el límite y los ids de los problemas de la evaluación.
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record Entrada(long id, long cursoId, OffsetDateTime fechaInicio, OffsetDateTime fechaFin, int limiteMinutos,
            String estado, List<Long> problemas) {
    }

    // La respuesta 403 de antes o después de la ventana: dice por qué y trae las fechas para que la pantalla las muestre.
    // Nunca lleva problemas.
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record FueraDeVentana(String detail, String estado, OffsetDateTime fechaInicio, OffsetDateTime fechaFin) {
    }

    private final AdminCliente admin;
    private final InscripcionRepository inscripciones;

    public EvaluacionesController(AdminCliente admin, InscripcionRepository inscripciones) {
        this.admin = admin;
        this.inscripciones = inscripciones;
    }

    // Antes de la ventana y después de ella (CA-64.1 y CA-64.3) es 403; durante (CA-64.2) es 200. Una evaluación que no
    // existe o de un curso donde el estudiante no está inscrito da el mismo 404, para no revelar qué ids existen.
    @GetMapping("/api/evaluaciones/{evaluacionId}")
    public ResponseEntity<?> entrar(@PathVariable long evaluacionId,
            @RequestAttribute("estudiante") Estudiante estudiante) {
        EvaluacionDto evaluacion;
        try {
            evaluacion = admin.evaluacion(evaluacionId);
        } catch (EvaluacionNoExisteException e) {
            return noEncontrada();
        } catch (AdminNoDisponibleException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new Detalle("No se pudo cargar la evaluación en este momento. Intenta de nuevo."));
        }
        if (!inscripciones.estaInscrito(estudiante.getId(), evaluacion.cursoId())) {
            return noEncontrada();
        }
        if (ACTIVA.equals(evaluacion.estado())) {
            return ResponseEntity.ok(new Entrada(evaluacion.id(), evaluacion.cursoId(), evaluacion.fechaInicio(),
                    evaluacion.fechaFin(), evaluacion.limiteMinutos(), evaluacion.estado(), evaluacion.problemas()));
        }
        // Cualquier estado que no sea «activa» cierra la puerta; «programada» es la única con motivo distinto.
        String motivo = PROGRAMADA.equals(evaluacion.estado()) ? "La evaluación todavía no comienza."
                : "La evaluación ya terminó.";
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new FueraDeVentana(motivo, evaluacion.estado(),
                evaluacion.fechaInicio(), evaluacion.fechaFin()));
    }

    private static ResponseEntity<Detalle> noEncontrada() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Detalle("Evaluación no encontrada."));
    }
}
