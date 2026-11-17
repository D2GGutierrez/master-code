package com.mastercode.userapi.cinturon;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.mastercode.userapi.perfil.Estudiante;

// HU51-T1 (CA-51.1, CA-51.2): el perfil del estudiante con sus cinturones y lo que le falta para el siguiente. Uno por
// lenguaje en el que ya acertó algún ejercicio; sin ninguno, la lista va vacía. El estudiante sale del token.
@RestController
public class CinturonesController {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.ALWAYS)
    record Cinturones(List<AvanceDeCinturon> cinturones) {
    }

    private final CinturonRepository cinturones;

    public CinturonesController(CinturonRepository cinturones) {
        this.cinturones = cinturones;
    }

    @GetMapping("/api/yo/cinturones")
    public Cinturones cinturones(@RequestAttribute("estudiante") Estudiante estudiante) {
        List<CinturonRepository.Nivel> niveles = cinturones.niveles();
        return new Cinturones(cinturones.listar(estudiante.getId()).stream()
                .map(c -> AvanceDeCinturon.de(c, niveles))
                .toList());
    }
}
