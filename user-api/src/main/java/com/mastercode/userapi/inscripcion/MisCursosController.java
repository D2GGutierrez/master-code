package com.mastercode.userapi.inscripcion;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.mastercode.userapi.admin.AdminCliente;
import com.mastercode.userapi.admin.AdminNoDisponibleException;
import com.mastercode.userapi.admin.CursoDto;
import com.mastercode.userapi.inscripcion.InscripcionController.Detalle;
import com.mastercode.userapi.inscripcion.InscripcionRepository.Fila;
import com.mastercode.userapi.perfil.Estudiante;

// HU27: los cursos en los que está inscrito el estudiante autenticado.
@RestController
public class MisCursosController {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record MiCurso(long cursoId, String nombre, String lenguaje, String institucion, OffsetDateTime fechaInscripcion) {
    }

    private final AdminCliente admin;
    private final InscripcionRepository inscripciones;

    public MisCursosController(AdminCliente admin, InscripcionRepository inscripciones) {
        this.admin = admin;
        this.inscripciones = inscripciones;
    }

    // Ruta aparte de /api/cursos: esa será el catálogo de cursos disponibles (HU25).
    @GetMapping("/api/mis-cursos")
    public ResponseEntity<?> misCursos(@RequestAttribute("estudiante") Estudiante estudiante) {
        // Solo las inscripciones del token: nunca se acepta un id de estudiante por parámetro.
        List<Fila> filas = inscripciones.deEstudiante(estudiante.getId());
        if (filas.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        Map<Long, CursoDto> cursos;
        try {
            // Una sola consulta para todos los cursos, no una por curso.
            cursos = admin.cursosPorIds(filas.stream().map(Fila::cursoId).toList()).stream()
                    .collect(Collectors.toMap(CursoDto::id, Function.identity()));
        } catch (AdminNoDisponibleException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new Detalle("No se pudieron cargar tus cursos en este momento. Intenta de nuevo."));
        }

        // Un curso que ya no existe en admin-api no se muestra, en vez de mostrarlo sin nombre.
        List<MiCurso> resultado = filas.stream()
                .filter(fila -> cursos.containsKey(fila.cursoId()))
                .map(fila -> {
                    CursoDto curso = cursos.get(fila.cursoId());
                    return new MiCurso(curso.id(), curso.nombre(), curso.lenguaje(), curso.institucion(), fila.fecha());
                })
                .toList();
        return ResponseEntity.ok(resultado);
    }
}
