package com.mastercode.userapi.dojo;

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

    record SesionIniciada(long id, String lenguaje) {
    }

    private final SesionDojoRepository sesiones;
    private final SiguienteEjercicioServicio siguiente;

    public DojoController(SesionDojoRepository sesiones, SiguienteEjercicioServicio siguiente) {
        this.sesiones = sesiones;
        this.siguiente = siguiente;
    }

    // Empieza una sesión (CA-53.1): de un lenguaje, para el estudiante del token.
    @PostMapping("/api/dojo/sesiones")
    public ResponseEntity<?> iniciar(@RequestBody Solicitud solicitud, @RequestAttribute("estudiante") Estudiante estudiante) {
        String lenguaje = solicitud.lenguaje() == null ? "" : solicitud.lenguaje().trim().toLowerCase(Locale.ROOT);
        if (!LENGUAJES.contains(lenguaje)) {
            return ResponseEntity.badRequest().body(new Detalle("lenguaje debe ser python, java o javascript."));
        }
        long id = sesiones.crear(estudiante.getId(), lenguaje);
        return ResponseEntity.status(HttpStatus.CREATED).body(new SesionIniciada(id, lenguaje));
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
}
