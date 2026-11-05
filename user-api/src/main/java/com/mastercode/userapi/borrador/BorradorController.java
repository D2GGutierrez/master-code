package com.mastercode.userapi.borrador;

import java.time.OffsetDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.mastercode.userapi.Detalle;
import com.mastercode.userapi.admin.AdminCliente;
import com.mastercode.userapi.admin.AdminNoDisponibleException;
import com.mastercode.userapi.admin.ProblemaDto;
import com.mastercode.userapi.admin.ProblemaNoVisibleException;
import com.mastercode.userapi.inscripcion.InscripcionRepository;
import com.mastercode.userapi.perfil.Estudiante;

// HU35-T2: el borrador del código del estudiante en un problema, para que no pierda lo escrito al cerrar el editor
// (CA-35.1) y lo recupere al volver (CA-35.4). No es un envío: no se ejecuta ni cuenta como intento.
@RestController
public class BorradorController {

    // Mismo tope que «Ejecutar» y el envío: un borrador no puede ser mayor que lo que luego se puede enviar.
    private static final int MAX_CODIGO = 50_000;

    record Solicitud(String codigo) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record Guardado(OffsetDateTime fechaActualizacion) {
    }

    private final AdminCliente admin;
    private final InscripcionRepository inscripciones;
    private final BorradorRepository borradores;

    public BorradorController(AdminCliente admin, InscripcionRepository inscripciones, BorradorRepository borradores) {
        this.admin = admin;
        this.inscripciones = inscripciones;
        this.borradores = borradores;
    }

    // Sin borrador responde 200 con codigo y fecha_actualizacion en null (no 404): «no hay borrador» es un estado normal
    // al abrir un problema por primera vez. Un borrador vacío ("") es distinto de no tener: el editor abre vacío (D8).
    // El estudiante sale del token y la consulta filtra por él. Es un dato del propio estudiante: no consulta a
    // admin-api, igual que el detalle de un envío (HU42-T2).
    @GetMapping("/api/problemas/{problemaId}/borrador")
    public BorradorRepository.Borrador obtener(@PathVariable long problemaId,
            @RequestAttribute("estudiante") Estudiante estudiante) {
        return borradores.deEstudiante(estudiante.getId(), problemaId)
                .orElse(new BorradorRepository.Borrador(null, null));
    }

    // El acceso es el de abrir un problema (HU31-T2): uno no visible, o de un curso donde no está inscrito, da el mismo
    // 404 y no guarda nada. Cada guardado vuelve a preguntar a admin-api; cuando el autoguardado de la pantalla
    // (HU35-T3) necesite menos llamadas, se podrá cachear ahí (ADR-8 D9).
    @PutMapping("/api/problemas/{problemaId}/borrador")
    public ResponseEntity<?> guardar(@PathVariable long problemaId, @RequestBody Solicitud solicitud,
            @RequestAttribute("estudiante") Estudiante estudiante) {
        String codigo = solicitud.codigo();
        if (codigo == null) {
            return error(HttpStatus.BAD_REQUEST, "Falta el código del borrador.");
        }
        if (codigo.length() > MAX_CODIGO) {
            return error(HttpStatus.BAD_REQUEST, "El código es demasiado largo (máximo " + MAX_CODIGO + " caracteres).");
        }

        ProblemaDto problema;
        try {
            problema = admin.problema(problemaId);
        } catch (ProblemaNoVisibleException e) {
            return noEncontrado();
        } catch (AdminNoDisponibleException e) {
            return error(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo cargar el problema en este momento. Intenta de nuevo.");
        }
        if (!inscripciones.estaInscrito(estudiante.getId(), problema.cursoId())) {
            return noEncontrado();
        }
        // Sin caracteres NUL: PostgreSQL no los admite en un texto y el editor podría mandarlos al pegar contenido raro.
        OffsetDateTime fecha = borradores.guardar(estudiante.getId(), problemaId, codigo.replace("\0", ""));
        return ResponseEntity.ok(new Guardado(fecha));
    }

    private static ResponseEntity<Detalle> noEncontrado() {
        return error(HttpStatus.NOT_FOUND, "Problema no encontrado.");
    }

    private static ResponseEntity<Detalle> error(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(new Detalle(mensaje));
    }
}
