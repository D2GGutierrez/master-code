package com.mastercode.userapi.inscripcion;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.mastercode.userapi.admin.AdminCliente;
import com.mastercode.userapi.admin.AdminNoDisponibleException;
import com.mastercode.userapi.admin.CodigoInvalidoException;
import com.mastercode.userapi.admin.CodigoVencidoException;
import com.mastercode.userapi.admin.CursoDto;
import com.mastercode.userapi.inscripcion.InscripcionRepository.Fila;
import com.mastercode.userapi.perfil.Estudiante;

// HU26: el estudiante se inscribe con el código de invitación que le da su docente.
@RestController
public class InscripcionController {

    // Solo letras y números: el código va dentro de una ruta hacia admin-api, así que nada raro pasa de aquí.
    private static final Pattern FORMATO = Pattern.compile("[A-Z0-9]{1,32}");

    record Solicitud(String codigo) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record Inscrita(long id, long cursoId, String nombre, String lenguaje, OffsetDateTime fecha) {
    }

    // Mismo cuerpo de error que admin-api (DRF): {"detail": "..."}.
    record Detalle(String detail) {
    }

    private final AdminCliente admin;
    private final InscripcionRepository inscripciones;

    public InscripcionController(AdminCliente admin, InscripcionRepository inscripciones) {
        this.admin = admin;
        this.inscripciones = inscripciones;
    }

    // El estudiante sale siempre del token (AsegurarPerfilFiltro): no se acepta un id por el cuerpo.
    @PostMapping("/api/inscripciones")
    public ResponseEntity<?> inscribir(@RequestBody Solicitud solicitud,
            @RequestAttribute("estudiante") Estudiante estudiante) {
        // Mayúsculas y sin espacios: así «abcd2345 » también sirve; los códigos se guardan en mayúsculas.
        String codigo = solicitud.codigo() == null ? "" : solicitud.codigo().trim().toUpperCase(Locale.ROOT);
        if (codigo.isEmpty()) {
            return error(HttpStatus.BAD_REQUEST, "Escribe el código de invitación.");
        }
        if (!FORMATO.matcher(codigo).matches()) {
            return error(HttpStatus.BAD_REQUEST, "El código de invitación no es correcto. Revísalo con tu docente.");
        }

        CursoDto curso;
        try {
            // La validez la decide admin-api, dueño de los cursos: aquí no se leen sus tablas (ADR-1).
            curso = admin.cursoPorCodigo(codigo);
        } catch (CodigoInvalidoException e) {
            return error(HttpStatus.BAD_REQUEST, "El código de invitación no es correcto. Revísalo con tu docente.");
        } catch (CodigoVencidoException e) {
            return error(HttpStatus.BAD_REQUEST, "El código de invitación venció. Pide uno nuevo a tu docente.");
        } catch (AdminNoDisponibleException e) {
            return error(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo validar el código en este momento. Intenta de nuevo en un momento.");
        }

        Optional<Fila> fila = inscripciones.crear(estudiante.getId(), curso.id());
        if (fila.isEmpty()) {
            return error(HttpStatus.CONFLICT, "Ya estás inscrito en este curso.");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new Inscrita(fila.get().id(), curso.id(), curso.nombre(), curso.lenguaje(), fila.get().fecha()));
    }

    private static ResponseEntity<Detalle> error(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(new Detalle(mensaje));
    }
}
