package com.mastercode.userapi.ejercicio;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.mastercode.userapi.Detalle;
import com.mastercode.userapi.admin.AdminCliente;
import com.mastercode.userapi.admin.AdminNoDisponibleException;
import com.mastercode.userapi.admin.ProblemaDto;
import com.mastercode.userapi.admin.ProblemaNoVisibleException;
import com.mastercode.userapi.admin.RespuestaDelProblemaDto;
import com.mastercode.userapi.inscripcion.InscripcionRepository;
import com.mastercode.userapi.perfil.Estudiante;

// H2 (ADR-9, D15): la ruta única donde el celular envía la respuesta a un ejercicio que no es de código. Elige el
// corrector según el tipo (CorrectorDeEjercicios) y devuelve si acertó. Todavía no guarda el intento ni limita cuántos
// hay: eso es HU45-T3 (cada intento será una fila de app_envio, D14). El estudiante sale del token, nunca del cuerpo.
@RestController
public class RespuestasController {

    // respuesta: lo escrito (predicción de salida); opcion_id: la alternativa elegida (opción múltiple).
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record Solicitud(String respuesta, Long opcionId) {
    }

    // respuesta_correcta solo sale si falló (HU48-T2) y nunca antes de que el estudiante responda (CA-46.4).
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Resultado(String resultado, String respuestaCorrecta) {
    }

    private final AdminCliente admin;
    private final InscripcionRepository inscripciones;
    private final CorrectorDeEjercicios corrector;

    public RespuestasController(AdminCliente admin, InscripcionRepository inscripciones, CorrectorDeEjercicios corrector) {
        this.admin = admin;
        this.inscripciones = inscripciones;
        this.corrector = corrector;
    }

    // El acceso es el de abrir un problema (HU31-T2): uno no visible o de un curso donde el estudiante no está inscrito
    // da el mismo 404, y la respuesta esperada solo se pide a admin-api cuando ya se sabe que puede responder.
    @PostMapping("/api/problemas/{problemaId}/respuesta")
    public ResponseEntity<?> responder(@PathVariable long problemaId, @RequestBody Solicitud solicitud,
            @RequestAttribute("estudiante") Estudiante estudiante) {
        ProblemaDto problema;
        try {
            problema = admin.problema(problemaId);
        } catch (ProblemaNoVisibleException e) {
            return noEncontrado();
        } catch (AdminNoDisponibleException e) {
            return noDisponible();
        }
        if (!inscripciones.estaInscrito(estudiante.getId(), problema.cursoId())) {
            return noEncontrado();
        }

        RespuestaDelProblemaDto paraCorregir;
        try {
            paraCorregir = admin.respuestaDelProblema(problemaId);
        } catch (ProblemaNoVisibleException e) {
            return noEncontrado();
        } catch (AdminNoDisponibleException e) {
            return noDisponible();
        }

        Veredicto veredicto;
        try {
            veredicto = corrector.corregir(paraCorregir, solicitud.respuesta(), solicitud.opcionId());
        } catch (RespuestaInvalidaException e) {
            return ResponseEntity.badRequest().body(new Detalle(e.getMessage()));
        }
        return ResponseEntity.ok(new Resultado(veredicto.correcta() ? "correcto" : "incorrecto",
                veredicto.respuestaCorrecta()));
    }

    private static ResponseEntity<Detalle> noEncontrado() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Detalle("Problema no encontrado."));
    }

    private static ResponseEntity<Detalle> noDisponible() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new Detalle("No se pudo corregir tu respuesta en este momento. Intenta de nuevo."));
    }
}
