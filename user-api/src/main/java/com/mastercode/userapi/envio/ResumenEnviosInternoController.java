package com.mastercode.userapi.envio;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mastercode.userapi.Detalle;

// API interna (D3, D10): la consume admin-api con X-Internal-Key para la vista del docente (HU44); no es para
// estudiantes ni para el navegador. La clave se comprueba en ClaveInternaFiltro, antes de llegar aquí.
@RestController
public class ResumenEnviosInternoController {

    // Tope de ids por consulta, como el de admin-api al pedir cursos por ids.
    private static final int MAX_PROBLEMAS = 200;

    private final EnvioRepository envios;

    public ResumenEnviosInternoController(EnvioRepository envios) {
        this.envios = envios;
    }

    // Qué problemas son del curso, y que el curso sea del docente, lo comprueba admin-api antes de preguntar: aquí
    // solo se cuentan los envíos de los problemas que llegan. Solo devuelve cifras por problema, sin estudiantes ni código.
    @GetMapping("/internal/envios/resumen")
    public ResponseEntity<?> resumen(@RequestParam(name = "problemas", required = false) List<Long> problemas) {
        if (problemas == null || problemas.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        if (problemas.size() > MAX_PROBLEMAS) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Detalle("Máximo " + MAX_PROBLEMAS + " problemas por consulta."));
        }
        return ResponseEntity.ok(envios.resumenPorProblema(problemas));
    }
}
