package com.mastercode.userapi.dojo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;

import com.mastercode.userapi.admin.AdminCliente;
import com.mastercode.userapi.admin.AdminNoDisponibleException;
import com.mastercode.userapi.admin.CursoNoExisteException;
import com.mastercode.userapi.admin.ProblemaDto;
import com.mastercode.userapi.admin.ProblemaNoVisibleException;
import com.mastercode.userapi.admin.ProblemaResumenDto;
import com.mastercode.userapi.inscripcion.InscripcionRepository;

// HU53-T2 (CA-53.2, CA-53.3): elige el siguiente ejercicio de una sesión del Dojo. Candidatos: los ejercicios del celular
// (plataforma móvil o ambas, nunca de escribir código) que admin-api entrega como visibles en los cursos donde el
// estudiante está inscrito, del lenguaje de la sesión y que no se hayan entregado ya en ella. Entre ellos, uno al azar.
// La elección de la regla de dificultad (HU54-T2) vendrá después, sobre esta misma lista de candidatos.
@Service
public class SiguienteEjercicioServicio {

    private static final String ESCRITURA_CODIGO = "escritura_codigo";

    private final AdminCliente admin;
    private final InscripcionRepository inscripciones;
    private final SesionDojoRepository sesiones;

    public SiguienteEjercicioServicio(AdminCliente admin, InscripcionRepository inscripciones,
            SesionDojoRepository sesiones) {
        this.admin = admin;
        this.inscripciones = inscripciones;
        this.sesiones = sesiones;
    }

    // vacío: ya no quedan ejercicios por entregar en esta sesión. Lanza AdminNoDisponibleException si admin-api no
    // responde: en ese caso no se entrega ni se apunta nada.
    public Optional<ProblemaDto> siguiente(SesionDojoRepository.Sesion sesion) {
        List<Long> candidatos = new ArrayList<>(candidatos(sesion));
        Collections.shuffle(candidatos, ThreadLocalRandom.current());
        for (long problemaId : candidatos) {
            ProblemaDto problema;
            try {
                problema = admin.problema(problemaId);
            } catch (ProblemaNoVisibleException e) {
                // Dejó de ser visible entre el listado y ahora: se prueba con otro.
                continue;
            }
            // false si otra petición a la vez ya lo entregó o tomó ese lugar: se prueba con otro.
            if (sesiones.registrarEntrega(sesion.id(), problemaId)) {
                return Optional.of(problema);
            }
        }
        return Optional.empty();
    }

    private Set<Long> candidatos(SesionDojoRepository.Sesion sesion) {
        Set<Long> yaEntregados = sesiones.entregados(sesion.id());
        Set<Long> candidatos = new LinkedHashSet<>();
        for (InscripcionRepository.Fila inscripcion : inscripciones.deEstudiante(sesion.estudianteId())) {
            List<ProblemaResumenDto> delCurso;
            try {
                delCurso = admin.problemasDelCurso(inscripcion.cursoId(), "movil");
            } catch (CursoNoExisteException e) {
                // Un curso que admin-api ya no tiene no aporta ejercicios; no es un error de la sesión.
                continue;
            }
            for (ProblemaResumenDto problema : delCurso) {
                boolean delLenguaje = problema.lenguaje() != null && problema.lenguaje().equalsIgnoreCase(sesion.lenguaje());
                boolean delCelular = !ESCRITURA_CODIGO.equals(problema.tipoEjercicio());
                if (delLenguaje && delCelular && !yaEntregados.contains(problema.id())) {
                    candidatos.add(problema.id());
                }
            }
        }
        return candidatos;
    }
}
