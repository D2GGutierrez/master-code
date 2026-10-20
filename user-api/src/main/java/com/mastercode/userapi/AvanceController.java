package com.mastercode.userapi;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.mastercode.userapi.avance.AvanceRepository;
import com.mastercode.userapi.avance.AvanceRepository.ProblemaAvance;
import com.mastercode.userapi.perfil.Estudiante;

@RestController
public class AvanceController {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record Avance(int problemasIntentados, long totalEnvios, long desdeWeb, long desdeMovil,
            List<ProblemaAvance> problemas) {
    }

    private final AvanceRepository avance;

    public AvanceController(AvanceRepository avance) {
        this.avance = avance;
    }

    // El estudiante sale siempre del token (AsegurarPerfilFiltro): no se acepta un id por parámetro.
    @GetMapping("/api/avance")
    public Avance avance(@RequestAttribute("estudiante") Estudiante estudiante) {
        List<ProblemaAvance> problemas = avance.porProblema(estudiante.getId());
        // Los totales salen de sumar la lista, sin otra consulta.
        return new Avance(
                problemas.size(),
                problemas.stream().mapToLong(ProblemaAvance::envios).sum(),
                problemas.stream().mapToLong(ProblemaAvance::desdeWeb).sum(),
                problemas.stream().mapToLong(ProblemaAvance::desdeMovil).sum(),
                problemas);
    }
}
