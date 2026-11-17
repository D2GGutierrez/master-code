package com.mastercode.userapi.dojo;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

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
import com.mastercode.userapi.admin.AdminNoDisponibleException;
import com.mastercode.userapi.admin.ProblemaDto;
import com.mastercode.userapi.perfil.Estudiante;
import com.mastercode.userapi.problema.ProblemasController;

// HU53-T2 (y el inicio de sesión que necesita): el Dojo es la práctica libre del celular, en sesiones de ejercicios
// cortos de un lenguaje. El estudiante sale del token, nunca del cuerpo ni de la ruta: no hay forma de usar la sesión de
// otro. Los ejercicios llegan con la forma del detalle de un problema (GET /api/problemas/{id}): sin respuesta
// esperada y con las alternativas mezcladas y sin marca de cuál es la correcta.
@RestController
public class DojoController {

    // Los mismos lenguajes que admin-api (cursos y problemas).
    private static final Set<String> LENGUAJES = Set.of("python", "java", "javascript");

    record Solicitud(String lenguaje) {
    }

    // nivel: la dificultad con la que parte la sesión (HU54-T3, HU54-T4).
    record SesionIniciada(long id, String lenguaje, String nivel) {
    }

    // HU56: lo que ve el estudiante al terminar. cinturon sale null si todavía no tiene cinturón en ese lenguaje.
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.ALWAYS)
    record ResumenDeSesion(String nivelAlcanzado, int respondidos, int aciertos, int puntosSumados,
            List<SesionDojoRepository.TemaFallado> temasMasFallados, SesionDojoServicio.CinturonActual cinturon) {
    }

    private final SesionDojoRepository sesiones;
    private final SesionDojoServicio sesionesServicio;
    private final SiguienteEjercicioServicio siguiente;

    public DojoController(SesionDojoRepository sesiones, SesionDojoServicio sesionesServicio,
            SiguienteEjercicioServicio siguiente) {
        this.sesiones = sesiones;
        this.sesionesServicio = sesionesServicio;
        this.siguiente = siguiente;
    }

    // Empieza una sesión (CA-53.1): de un lenguaje, para el estudiante del token.
    @PostMapping("/api/dojo/sesiones")
    public ResponseEntity<?> iniciar(@RequestBody Solicitud solicitud, @RequestAttribute("estudiante") Estudiante estudiante) {
        String lenguaje = solicitud.lenguaje() == null ? "" : solicitud.lenguaje().trim().toLowerCase(Locale.ROOT);
        if (!LENGUAJES.contains(lenguaje)) {
            return ResponseEntity.badRequest().body(new Detalle("lenguaje debe ser python, java o javascript."));
        }
        Nivel nivel = sesionesServicio.nivelInicial(estudiante.getId(), lenguaje);
        long id = sesiones.crear(estudiante.getId(), lenguaje, nivel);
        return ResponseEntity.status(HttpStatus.CREATED).body(new SesionIniciada(id, lenguaje, nivel.valor()));
    }

    // Entrega el siguiente ejercicio de la sesión (CA-53.2) y lo apunta como entregado, así no vuelve a salir en ella
    // (CA-53.3). 204 cuando ya no quedan ejercicios. Una sesión ajena o inexistente da 404; una ya terminada, 409.
    @PostMapping("/api/dojo/sesiones/{sesionId}/siguiente")
    public ResponseEntity<?> siguiente(@PathVariable long sesionId, @RequestAttribute("estudiante") Estudiante estudiante) {
        Optional<SesionDojoRepository.Sesion> sesion = sesiones.buscar(sesionId, estudiante.getId());
        if (sesion.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Detalle("Sesión no encontrada."));
        }
        if (!sesion.get().abierta()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new Detalle("Esta sesión ya terminó."));
        }
        Optional<ProblemaDto> problema;
        try {
            problema = siguiente.siguiente(sesion.get());
        } catch (AdminNoDisponibleException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new Detalle("No se pudo cargar el ejercicio en este momento. Intenta de nuevo."));
        }
        if (problema.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(ProblemasController.detalleDe(problema.get()));
    }

    // HU54-T3 y HU56: termina la sesión. Guarda el nivel alcanzado y devuelve el resumen: aciertos, puntos que sumó al
    // cinturón y los temas donde más falló. Una sesión ajena o inexistente da 404; una que ya terminó, 409.
    @PostMapping("/api/dojo/sesiones/{sesionId}/terminar")
    public ResponseEntity<?> terminar(@PathVariable long sesionId, @RequestAttribute("estudiante") Estudiante estudiante) {
        Optional<SesionDojoRepository.Sesion> sesion = sesiones.buscar(sesionId, estudiante.getId());
        if (sesion.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Detalle("Sesión no encontrada."));
        }
        Optional<SesionDojoServicio.Resumen> resumen = sesionesServicio.terminar(sesion.get());
        if (resumen.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new Detalle("Esta sesión ya terminó."));
        }
        SesionDojoServicio.Resumen r = resumen.get();
        return ResponseEntity.ok(new ResumenDeSesion(r.nivelAlcanzado(), r.respondidos(), r.aciertos(), r.puntosSumados(),
                r.temasMasFallados(), r.cinturon().orElse(null)));
    }
}
