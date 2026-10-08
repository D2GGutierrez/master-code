package com.mastercode.userapi;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import com.mastercode.userapi.perfil.Estudiante;

@RestController
public class YoController {

    // Records: Jackson los convierte a JSON con los campos en este orden.
    record Perfil(Long id) {
    }

    record Yo(String email, String rol, String institucion, String cliente, Perfil perfil) {
    }

    // El perfil lo deja AsegurarPerfilFiltro en la petición, así no se consulta otra vez.
    @GetMapping("/api/yo")
    public Yo yo(@AuthenticationPrincipal Jwt jwt, @RequestAttribute("estudiante") Estudiante estudiante) {
        return new Yo(
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("rol"),
                jwt.getClaimAsString("institucion"),
                jwt.getClaimAsString("cliente"),
                new Perfil(estudiante.getId()));
    }
}
