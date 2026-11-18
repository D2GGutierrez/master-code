package com.mastercode.userapi.notificacion;

import java.time.LocalTime;
import java.util.regex.Pattern;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.mastercode.userapi.Detalle;
import com.mastercode.userapi.perfil.Estudiante;

// HU52-T2 (CA-52.3, CA-52.4; ADR-15): los ajustes del aviso de racha viven en el teléfono, pero el aviso lo decide el
// servidor, así que la app los manda aquí cada vez que el estudiante los cambia. Es un PUT: repetirlo no cambia nada. El
// estudiante sale del token de sesión, nunca del cuerpo.
@RestController
public class AvisosController {

    private static final Pattern HORA = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

    // hora: «HH:mm» en la hora local del estudiante; si no llega, queda la que ya tenía.
    record Solicitud(Boolean activados, String hora) {
    }

    private final AvisoAjusteRepository ajustes;

    public AvisosController(AvisoAjusteRepository ajustes) {
        this.ajustes = ajustes;
    }

    @PutMapping("/api/yo/avisos")
    public ResponseEntity<?> guardar(@RequestBody Solicitud solicitud, @RequestAttribute("estudiante") Estudiante estudiante) {
        if (solicitud.activados() == null) {
            return ResponseEntity.badRequest().body(new Detalle("activados es obligatorio (true o false)."));
        }
        LocalTime hora = null;
        if (solicitud.hora() != null) {
            if (!HORA.matcher(solicitud.hora()).matches()) {
                return ResponseEntity.badRequest().body(new Detalle("hora debe ser HH:mm, por ejemplo 20:00."));
            }
            hora = LocalTime.parse(solicitud.hora());
        }
        ajustes.guardar(estudiante.getId(), solicitud.activados(), hora);
        return ResponseEntity.noContent().build();
    }
}
