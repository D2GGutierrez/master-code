package com.mastercode.userapi.catalogo;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.mastercode.userapi.Detalle;
import com.mastercode.userapi.admin.AdminCliente;
import com.mastercode.userapi.admin.AdminNoDisponibleException;
import com.mastercode.userapi.admin.CursoDto;

// HU25: los cursos publicados de la institución del estudiante, con búsqueda por nombre.
// «Publicado» lo decide admin-api (tiene al menos un problema visible); aquí se pide y se limita a la institución.
@RestController
public class CatalogoController {

    // Igual que MiCurso, sin fecha de inscripción. Nunca lleva el código de invitación: admin-api no lo entrega.
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record CursoDisponible(long cursoId, String nombre, String lenguaje, String institucion) {
    }

    private final AdminCliente admin;

    public CatalogoController(AdminCliente admin) {
        this.admin = admin;
    }

    // ?buscar= es parte del nombre del curso, sin importar mayúsculas. Sin coincidencias la respuesta es una lista
    // vacía (200): el mensaje «sin resultados» lo muestra la pantalla (CA-25.4).
    @GetMapping("/api/cursos")
    public ResponseEntity<?> cursosDisponibles(@RequestParam(required = false) String buscar,
            @AuthenticationPrincipal Jwt jwt) {
        // La institución sale del token, nunca de un parámetro: el estudiante no puede pedir otra (CA-25.3).
        // El decodificador ya exige que el claim exista y no esté vacío (SeguridadConfig).
        String institucion = jwt.getClaimAsString("institucion");
        String texto = buscar == null || buscar.isBlank() ? null : buscar.trim();

        List<CursoDto> cursos;
        try {
            cursos = admin.cursosPublicados(institucion, texto);
        } catch (AdminNoDisponibleException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new Detalle("No se pudieron cargar los cursos en este momento. Intenta de nuevo."));
        }

        // Aunque admin-api ya filtra por institución, la restricción se vuelve a aplicar aquí: si algún día
        // admin-api ignorara el parámetro, un estudiante no vería cursos de otra institución.
        List<CursoDisponible> resultado = cursos.stream()
                .filter(curso -> institucion.equalsIgnoreCase(curso.institucion()))
                .map(curso -> new CursoDisponible(curso.id(), curso.nombre(), curso.lenguaje(), curso.institucion()))
                .toList();
        return ResponseEntity.ok(resultado);
    }
}
