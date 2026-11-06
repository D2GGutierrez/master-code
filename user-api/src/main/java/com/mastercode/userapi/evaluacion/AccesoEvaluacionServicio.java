package com.mastercode.userapi.evaluacion;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;

import com.mastercode.userapi.admin.AdminCliente;
import com.mastercode.userapi.admin.EvaluacionDto;
import com.mastercode.userapi.admin.EvaluacionNoExisteException;
import com.mastercode.userapi.inscripcion.InscripcionRepository;

// HU64-T2 y HU64-T3: la puerta de una evaluación, la misma al entrar y al enviar código. Decide en este orden: que la
// evaluación exista y sea de un curso del estudiante, que esté dentro de su ventana (el estado lo calcula admin-api,
// HU64-T1) y que al estudiante no se le haya acabado su tiempo. El cierre por tiempo es perezoso (ADR-8 D11): no hay
// tarea programada, se compara la hora actual con el inicio del intento más el límite de minutos cada vez que se pregunta.
@Service
public class AccesoEvaluacionServicio {

    private static final String ACTIVA = "activa";
    private static final String PROGRAMADA = "programada";

    public enum Resultado {
        // No existe, o es de un curso donde el estudiante no está inscrito: se responde igual para no revelar cuál.
        NO_ENCONTRADA,
        // Antes de la ventana.
        PROGRAMADA,
        // Después de la ventana (o cualquier estado que no sea «activa»).
        CERRADA,
        // Dentro de la ventana, pero el estudiante ya usó su límite de minutos.
        TIEMPO_AGOTADO,
        ABIERTA
    }

    // evaluacion es null solo con NO_ENCONTRADA; plazo solo viene dentro de la ventana (ABIERTA y TIEMPO_AGOTADO).
    public record Acceso(Resultado resultado, EvaluacionDto evaluacion, PlazoDelIntento plazo) {
    }

    private final AdminCliente admin;
    private final InscripcionRepository inscripciones;
    private final IntentoEvaluacionRepository intentos;

    public AccesoEvaluacionServicio(AdminCliente admin, InscripcionRepository inscripciones,
            IntentoEvaluacionRepository intentos) {
        this.admin = admin;
        this.inscripciones = inscripciones;
        this.intentos = intentos;
    }

    // Si admin-api no responde lanza AdminNoDisponibleException: sin poder comprobar la ventana nadie entra. Dentro de la
    // ventana, entrar por primera vez empieza el reloj del estudiante; fuera de ella no se guarda nada.
    public Acceso entrar(long estudianteId, long evaluacionId) {
        EvaluacionDto evaluacion;
        try {
            evaluacion = admin.evaluacion(evaluacionId);
        } catch (EvaluacionNoExisteException e) {
            return new Acceso(Resultado.NO_ENCONTRADA, null, null);
        }
        if (!inscripciones.estaInscrito(estudianteId, evaluacion.cursoId())) {
            return new Acceso(Resultado.NO_ENCONTRADA, null, null);
        }
        if (!ACTIVA.equals(evaluacion.estado())) {
            Resultado fuera = PROGRAMADA.equals(evaluacion.estado()) ? Resultado.PROGRAMADA : Resultado.CERRADA;
            return new Acceso(fuera, evaluacion, null);
        }
        OffsetDateTime ahora = OffsetDateTime.now();
        OffsetDateTime inicio = intentos.iniciar(estudianteId, evaluacionId, ahora);
        PlazoDelIntento plazo = PlazoDelIntento.de(inicio, evaluacion.limiteMinutos(), evaluacion.fechaFin());
        return new Acceso(plazo.vencido(ahora) ? Resultado.TIEMPO_AGOTADO : Resultado.ABIERTA, evaluacion, plazo);
    }
}
