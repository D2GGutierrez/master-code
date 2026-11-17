package com.mastercode.userapi.ejercicio;

import java.util.Optional;

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
import com.mastercode.userapi.dojo.SesionDojoRepository;
import com.mastercode.userapi.envio.EnvioRepository;
import com.mastercode.userapi.inscripcion.InscripcionRepository;
import com.mastercode.userapi.perfil.Estudiante;

// H2 (ADR-9, D15): la ruta única donde el celular envía la respuesta a un ejercicio que no es de código. Elige el
// corrector según el tipo (CorrectorDeEjercicios) y devuelve si acertó. Todavía no guarda el intento ni limita cuántos
// hay: eso es HU45-T3 (cada intento será una fila de app_envio, D14). El estudiante sale del token, nunca del cuerpo.
@RestController
public class RespuestasController {

    // respuesta: lo escrito (predicción de salida); opcion_id: la alternativa elegida (opción múltiple); sesion_id: la
    // sesión del Dojo en la que se entregó el ejercicio, si se responde dentro de una (HU54): el resultado mueve el nivel.
    // intento_id: identificador único que la app le pone a cada intento; sirve para que un reenvío tras perder la señal
    // no cuente dos veces (HU48-T3).
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record Solicitud(String respuesta, Long opcionId, Long sesionId, String intentoId) {
    }

    // Largo máximo de intento_id (una UUID tiene 36).
    private static final int MAX_CLAVE = 40;

    // respuesta_correcta solo sale si falló (HU48-T2) y nunca antes de que el estudiante responda (CA-46.4).
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Resultado(String resultado, String respuestaCorrecta, int intentosRestantes) {
    }

    private final AdminCliente admin;
    private final InscripcionRepository inscripciones;
    private final CorrectorDeEjercicios corrector;
    private final SesionDojoRepository sesiones;
    private final EnvioRepository envios;
    private final RegistroDeRespuestas registro;

    public RespuestasController(AdminCliente admin, InscripcionRepository inscripciones, CorrectorDeEjercicios corrector,
            SesionDojoRepository sesiones, EnvioRepository envios, RegistroDeRespuestas registro) {
        this.admin = admin;
        this.inscripciones = inscripciones;
        this.corrector = corrector;
        this.sesiones = sesiones;
        this.envios = envios;
        this.registro = registro;
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

        // HU48-T3: un reenvío (misma clave que un intento ya guardado) no gasta intento, no vuelve a contar ni exige que
        // la sesión siga abierta: solo devuelve el mismo resultado, porque la respuesta original pudo no llegar a la app.
        String clave = solicitud.intentoId() == null || solicitud.intentoId().isBlank() ? null : solicitud.intentoId().trim();
        if (clave != null && clave.length() > MAX_CLAVE) {
            return ResponseEntity.badRequest().body(new Detalle("intento_id es demasiado largo."));
        }
        boolean reenvio = clave != null && envios.existeClave(estudiante.getId(), clave);

        // HU54: dentro de una sesión del Dojo, esta debe ser del estudiante, seguir abierta y tener el ejercicio pendiente.
        Optional<SesionDojoRepository.Sesion> sesion = Optional.empty();
        if (solicitud.sesionId() != null) {
            sesion = sesiones.buscar(solicitud.sesionId(), estudiante.getId());
            if (sesion.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Detalle("Sesión no encontrada."));
            }
            if (!reenvio && !sesion.get().abierta()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(new Detalle("Esta sesión ya terminó."));
            }
            if (!reenvio && !sesiones.pendiente(sesion.get().id(), problemaId)) {
                return sinPendiente();
            }
        }

        // HU45-T3 (CA-45.5): una respuesta nueva se rechaza si ya no quedan intentos, antes de pedir la respuesta esperada.
        if (!reenvio && IntentosMoviles.restantes(envios.intentosDe(estudiante.getId(), problemaId)) == 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new Detalle("Ya usaste los " + IntentosMoviles.MAXIMO + " intentos de este ejercicio."));
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
        // Lo que guardó el intento: el texto escrito o la alternativa elegida.
        String guardado = solicitud.opcionId() != null ? "opcion:" + solicitud.opcionId() : solicitud.respuesta();
        if (!reenvio) {
            try {
                registro.registrar(estudiante.getId(), problema, guardado, veredicto.correcta(), sesion, clave);
            } catch (RegistroDeRespuestas.SesionSinPendienteException e) {
                return sinPendiente();
            }
        }
        // Un reenvío que gana la carrera contra su original se registra como «ya estaba»: se responde igual, sin contar.
        int usados = envios.intentosDe(estudiante.getId(), problemaId);
        return ResponseEntity.ok(new Resultado(veredicto.correcta() ? "correcto" : "incorrecto",
                veredicto.respuestaCorrecta(), IntentosMoviles.restantes(usados)));
    }

    private static ResponseEntity<Detalle> noEncontrado() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Detalle("Problema no encontrado."));
    }

    private static ResponseEntity<Detalle> sinPendiente() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new Detalle("Este ejercicio no está pendiente de respuesta en la sesión."));
    }

    private static ResponseEntity<Detalle> noDisponible() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new Detalle("No se pudo corregir tu respuesta en este momento. Intenta de nuevo."));
    }
}
