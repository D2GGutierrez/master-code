package com.mastercode.userapi.perfil;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.mastercode.userapi.Detalle;

// H6 (ADR-13, D23): la app manda aquí la zona horaria del teléfono cada vez que abre con la sesión iniciada. Es un PUT
// porque repetirlo no cambia nada; si el estudiante viaja, el siguiente envío la actualiza. El estudiante sale del token,
// nunca del cuerpo.
@RestController
public class ZonaHorariaController {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record Solicitud(String zonaHoraria) {
    }

    private final ZonaHorariaRepository zonas;

    public ZonaHorariaController(ZonaHorariaRepository zonas) {
        this.zonas = zonas;
    }

    @PutMapping("/api/yo/zona-horaria")
    public ResponseEntity<?> guardar(@RequestBody Solicitud solicitud, @RequestAttribute("estudiante") Estudiante estudiante) {
        String zona = solicitud.zonaHoraria() == null ? "" : solicitud.zonaHoraria().strip();
        if (!ZonaHorariaRepository.esValida(zona)) {
            return ResponseEntity.badRequest()
                    .body(new Detalle("zona_horaria debe ser un nombre de región, por ejemplo America/Lima."));
        }
        zonas.guardar(estudiante.getId(), zona);
        return ResponseEntity.noContent().build();
    }
}
