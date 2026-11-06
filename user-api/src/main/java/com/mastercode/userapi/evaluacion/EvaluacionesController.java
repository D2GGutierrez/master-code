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
import com.mastercode.userapi.admin.AdminNoDisponibleException;
import com.mastercode.userapi.admin.EvaluacionDto;
import com.mastercode.userapi.evaluacion.AccesoEvaluacionServicio.Acceso;
import com.mastercode.userapi.perfil.Estudiante;

// HU64-T2 y HU64-T3: entrar a una evaluación solo se puede dentro de su ventana y mientras al estudiante le quede
// tiempo. Quién es el estudiante sale del token; la evaluación, su ventana y su estado salen de admin-api, que es quien
// las tiene (ADR-1, D11). La decisión la toma AccesoEvaluacionServicio, que también usa el envío de código.
@RestController
public class EvaluacionesController {

    // Lo que ve el estudiante al entrar: la ventana, el límite, los ids de los problemas y su propio plazo. El reloj de
    // su intento empieza con la primera entrada: fecha_inicio_intento no cambia al volver a entrar.
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record Entrada(long id, long cursoId, OffsetDateTime fechaInicio, OffsetDateTime fechaFin, int limiteMinutos,
            String estado, OffsetDateTime fechaInicioIntento, OffsetDateTime fechaLimite, List<Long> problemas) {
    }

    // La respuesta 403 de antes o después de la ventana: dice por qué y trae las fechas para que la pantalla las muestre.
    // Nunca lleva problemas.
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record FueraDeVentana(String detail, String estado, OffsetDateTime fechaInicio, OffsetDateTime fechaFin) {
    }

    // La respuesta 403 cuando se le acabó el tiempo al estudiante dentro de la ventana (CA-64.4). Tampoco lleva problemas.
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record TiempoAgotado(String detail, String estado, OffsetDateTime fechaInicioIntento, OffsetDateTime fechaLimite) {
    }

    private final AccesoEvaluacionServicio acceso;

    public EvaluacionesController(AccesoEvaluacionServicio acceso) {
        this.acceso = acceso;
    }

    // Antes de la ventana y después de ella (CA-64.1 y CA-64.3) es 403; durante (CA-64.2) es 200 y empieza el reloj del
    // estudiante; si ya usó su límite de minutos (CA-64.4) vuelve a ser 403. Una evaluación que no existe o de un curso
    // donde el estudiante no está inscrito da el mismo 404, para no revelar qué ids existen.
    @GetMapping("/api/evaluaciones/{evaluacionId}")
    public ResponseEntity<?> entrar(@PathVariable long evaluacionId,
            @RequestAttribute("estudiante") Estudiante estudiante) {
        Acceso resultado;
        try {
            resultado = acceso.entrar(estudiante.getId(), evaluacionId);
        } catch (AdminNoDisponibleException e) {
            return sinAdmin();
        }
        if (resultado.resultado() != AccesoEvaluacionServicio.Resultado.ABIERTA) {
            return rechazo(resultado);
        }
        EvaluacionDto evaluacion = resultado.evaluacion();
        return ResponseEntity.ok(new Entrada(evaluacion.id(), evaluacion.cursoId(), evaluacion.fechaInicio(),
                evaluacion.fechaFin(), evaluacion.limiteMinutos(), evaluacion.estado(), resultado.plazo().inicio(),
                resultado.plazo().fechaLimite(), evaluacion.problemas()));
    }

    // La respuesta de cada motivo por el que no se entra (o no se envía código a una evaluación). La usa también el envío.
    public static ResponseEntity<?> rechazo(Acceso resultado) {
        EvaluacionDto evaluacion = resultado.evaluacion();
        return switch (resultado.resultado()) {
            case NO_ENCONTRADA -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new Detalle("Evaluación no encontrada."));
            case PROGRAMADA -> ResponseEntity.status(HttpStatus.FORBIDDEN).body(new FueraDeVentana(
                    "La evaluación todavía no comienza.", "programada", evaluacion.fechaInicio(), evaluacion.fechaFin()));
            case CERRADA -> ResponseEntity.status(HttpStatus.FORBIDDEN).body(new FueraDeVentana(
                    "La evaluación ya terminó.", "cerrada", evaluacion.fechaInicio(), evaluacion.fechaFin()));
            case TIEMPO_AGOTADO -> ResponseEntity.status(HttpStatus.FORBIDDEN).body(new TiempoAgotado(
                    "Se acabó el tiempo de tu evaluación.", "tiempo_agotado", resultado.plazo().inicio(),
                    resultado.plazo().fechaLimite()));
            case ABIERTA -> throw new IllegalArgumentException("Una evaluación abierta no se rechaza.");
        };
    }

    static ResponseEntity<Detalle> sinAdmin() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new Detalle("No se pudo cargar la evaluación en este momento. Intenta de nuevo."));
    }
}
