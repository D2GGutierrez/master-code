package com.mastercode.userapi.problema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.mastercode.userapi.Detalle;
import com.mastercode.userapi.admin.AdminCliente;
import com.mastercode.userapi.admin.AdminNoDisponibleException;
import com.mastercode.userapi.admin.CursoNoExisteException;
import com.mastercode.userapi.admin.ProblemaDto;
import com.mastercode.userapi.admin.ProblemaNoVisibleException;
import com.mastercode.userapi.admin.ProblemaResumenDto;
import com.mastercode.userapi.inscripcion.InscripcionRepository;
import com.mastercode.userapi.perfil.Estudiante;

// HU29 (listado de problemas de un curso), HU31 (solo los aprobados) y HU32-T1 (detalle de un problema).
// Qué problemas son visibles lo decide admin-api: su API interna parte de Problema.visibles_para_estudiante().
// Aquí solo se decide quién puede preguntar: un estudiante inscrito en el curso. Las respuestas se arman
// campo por campo con records propios y no se reenvía el objeto de admin-api: si admin-api agregara un dato
// interno, no saldría hacia el estudiante sin que alguien lo decida aquí.
@RestController
public class ProblemasController {

    // Quien consulta es un cliente web o móvil. «ambas» es un valor de los problemas, no de los clientes.
    private static final Set<String> PLATAFORMAS = Set.of("web", "movil");

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record ProblemaDeLista(long id, String titulo, String lenguaje, String dificultad, String plataforma,
            String tipoEjercicio, OffsetDateTime fechaPublicacion) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record CasoDeEjemplo(String entrada, String salidaEsperada) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record ProblemaDetalle(long id, long cursoId, String titulo, String enunciado, String lenguaje, String dificultad,
            String plataforma, String tipoEjercicio, List<CasoDeEjemplo> casosPrueba) {
    }

    private final AdminCliente admin;
    private final InscripcionRepository inscripciones;

    public ProblemasController(AdminCliente admin, InscripcionRepository inscripciones) {
        this.admin = admin;
        this.inscripciones = inscripciones;
    }

    // ?plataforma=web|movil deja los problemas de esa plataforma y los de ambas (la web no ejecuta ejercicios del
    // celular, ni el celular los de código).
    @GetMapping("/api/cursos/{cursoId}/problemas")
    public ResponseEntity<?> delCurso(@PathVariable long cursoId,
            @RequestParam(required = false) String plataforma,
            @RequestAttribute("estudiante") Estudiante estudiante) {
        // Primero el acceso: quien no está inscrito recibe 403 sin que se llame a admin-api (CA-29.1).
        // Un curso que no existe también da 403, porque nadie está inscrito en él: no revela qué cursos hay.
        if (!inscripciones.estaInscrito(estudiante.getId(), cursoId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new Detalle("No estás inscrito en este curso."));
        }
        String filtro = plataforma == null || plataforma.isBlank() ? null : plataforma.trim();
        if (filtro != null && !PLATAFORMAS.contains(filtro)) {
            return ResponseEntity.badRequest().body(new Detalle("plataforma debe ser web o movil."));
        }

        List<ProblemaResumenDto> problemas;
        try {
            problemas = admin.problemasDelCurso(cursoId, filtro);
        } catch (CursoNoExisteException e) {
            // Inscrito en un curso que admin-api ya no tiene: igual que «Mis cursos», no se finge que existe.
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Detalle("El curso ya no existe."));
        } catch (AdminNoDisponibleException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new Detalle("No se pudieron cargar los problemas en este momento. Intenta de nuevo."));
        }

        List<ProblemaDeLista> resultado = problemas.stream()
                .map(p -> new ProblemaDeLista(p.id(), p.titulo(), p.lenguaje(), p.dificultad(), p.plataforma(),
                        p.tipoEjercicio(), p.fechaPublicacion()))
                .toList();
        return ResponseEntity.ok(resultado);
    }

    // HU31-T2: abrir un problema por id no es una vía para saltarse la aprobación ni la inscripción. Un problema sin
    // aprobar, descartado, inexistente o de un curso donde el estudiante no está inscrito responde igual (404), para
    // que adivinar ids no revele cuáles existen.
    @GetMapping("/api/problemas/{problemaId}")
    public ResponseEntity<?> detalle(@PathVariable long problemaId,
            @RequestAttribute("estudiante") Estudiante estudiante) {
        ProblemaDto problema;
        try {
            problema = admin.problema(problemaId);
        } catch (ProblemaNoVisibleException e) {
            return noEncontrado();
        } catch (AdminNoDisponibleException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new Detalle("No se pudo cargar el problema en este momento. Intenta de nuevo."));
        }
        // El curso del problema solo se conoce después de preguntarle a admin-api.
        if (!inscripciones.estaInscrito(estudiante.getId(), problema.cursoId())) {
            return noEncontrado();
        }

        List<ProblemaDto.Caso> casos = problema.casosPrueba() == null ? List.of() : problema.casosPrueba();
        return ResponseEntity.ok(new ProblemaDetalle(problema.id(), problema.cursoId(), problema.titulo(),
                problema.enunciado(), problema.lenguaje(), problema.dificultad(), problema.plataforma(),
                problema.tipoEjercicio(),
                casos.stream().map(c -> new CasoDeEjemplo(c.entrada(), c.salidaEsperada())).toList()));
    }

    private static ResponseEntity<Detalle> noEncontrado() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Detalle("Problema no encontrado."));
    }
}
