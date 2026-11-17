package com.mastercode.userapi.racha;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.mastercode.userapi.perfil.Estudiante;

// HU49-T2 (ADR-13, D23): la racha del estudiante para la pantalla principal (HU49-T3) y el aviso diario (HU52-T2).
@RestController
public class RachaController {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record RachaDeLenguaje(String lenguaje, int rachaActual, int rachaMaxima, boolean practicoHoy) {
    }

    // racha_actual es la más alta entre los lenguajes (la que se muestra en la pantalla principal); practico_hoy, si
    // practicó hoy en cualquiera.
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record RachaDelEstudiante(int rachaActual, boolean practicoHoy, List<RachaDeLenguaje> porLenguaje) {
    }

    private final RachaServicio rachas;

    public RachaController(RachaServicio rachas) {
        this.rachas = rachas;
    }

    @GetMapping("/api/yo/racha")
    public RachaDelEstudiante racha(@RequestAttribute("estudiante") Estudiante estudiante) {
        List<RachaServicio.RachaDelDia> dias = rachas.delDia(estudiante.getId());
        int actual = dias.stream().mapToInt(RachaServicio.RachaDelDia::rachaActual).max().orElse(0);
        boolean hoy = dias.stream().anyMatch(RachaServicio.RachaDelDia::practicoHoy);
        return new RachaDelEstudiante(actual, hoy, dias.stream()
                .map(d -> new RachaDeLenguaje(d.lenguaje(), d.rachaActual(), d.rachaMaxima(), d.practicoHoy()))
                .toList());
    }
}
