package com.mastercode.userapi.notificacion;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.mastercode.userapi.Detalle;
import com.mastercode.userapi.perfil.Estudiante;

// HU52-T1 (ADR-9, D16): la app registra aquí el token que Firebase Cloud Messaging le dio a ese teléfono. Es un PUT
// porque repetirlo no cambia nada: la app lo manda cada vez que abre sesión o que Firebase renueva el token. El
// estudiante sale del token de sesión, nunca del cuerpo: nadie puede registrar un dispositivo a nombre de otro.
@RestController
public class DispositivosController {

    // Los tokens de FCM miden unos 150 a 200 caracteres; este tope solo evita guardar un cuerpo enorme.
    private static final int MAX_TOKEN = 1_024;

    record Solicitud(String token) {
    }

    private final DispositivoRepository dispositivos;

    public DispositivosController(DispositivoRepository dispositivos) {
        this.dispositivos = dispositivos;
    }

    @PutMapping("/api/dispositivo")
    public ResponseEntity<?> registrar(@RequestBody Solicitud solicitud,
            @RequestAttribute("estudiante") Estudiante estudiante) {
        String token = solicitud.token() == null ? "" : solicitud.token().strip();
        if (token.isEmpty()) {
            return ResponseEntity.badRequest().body(new Detalle("Falta el token del dispositivo."));
        }
        if (token.length() > MAX_TOKEN) {
            return ResponseEntity.badRequest().body(new Detalle("El token del dispositivo no es válido."));
        }
        dispositivos.registrar(estudiante.getId(), token);
        return ResponseEntity.noContent().build();
    }
}
