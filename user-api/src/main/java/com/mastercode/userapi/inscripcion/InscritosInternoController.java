package com.mastercode.userapi.inscripcion;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.mastercode.userapi.inscripcion.InscripcionRepository.Inscrito;

// API interna (D3): la consume admin-api con X-Internal-Key; no es para estudiantes ni para el navegador.
// La clave se comprueba en ClaveInternaFiltro, antes de llegar aquí.
@RestController
public class InscritosInternoController {

    private final InscripcionRepository inscripciones;

    public InscritosInternoController(InscripcionRepository inscripciones) {
        this.inscripciones = inscripciones;
    }

    // Quién es dueño del curso lo comprueba admin-api antes de preguntar: aquí solo se entregan los datos.
    @GetMapping("/internal/cursos/{cursoId}/inscritos")
    public List<Inscrito> inscritos(@PathVariable long cursoId) {
        return inscripciones.inscritosDeCurso(cursoId);
    }
}
