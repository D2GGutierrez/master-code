package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

// HU31-T3 (CA-031.3): guardián de la auditoría de rutas. Cada ruta de user-api se revisó a mano para que ninguna
// entregue un problema sin aprobar. Si alguien agrega una nueva, esta prueba falla hasta que se audite.
class GuardianRutasTest extends PruebaDeApi {

    // Rutas auditadas (HU31-T3), con el motivo de su veredicto.
    private static final Set<String> AUDITADAS = Set.of(
            // NO APLICA: sin datos de problemas.
            "GET /health",
            "GET /api/yo",
            "POST /api/inscripciones",
            "GET /api/mis-cursos",
            // NO APLICA: cursos publicados; «publicado» lo decide admin-api con visibles_para_estudiante().
            "GET /api/cursos",
            // NO APLICA: código y entrada libres, sin id de problema; no guarda nada.
            "POST /api/ejecuciones",
            // NO APLICA hoy: solo los envíos del propio estudiante por id, sin contenido. Nada escribe app_envio
            // todavía; cuando HU36 lo haga, el envío debe validar el id contra /internal/problemas/{id}/.
            "GET /api/avance",
            // OK: la lista sale de /internal/cursos/{id}/problemas/ de admin-api (visibles_para_estudiante()).
            "GET /api/cursos/{cursoId}/problemas",
            // OK: el problema sale de /internal/problemas/{id}/ de admin-api; sin aprobar da el mismo 404.
            "GET /api/problemas/{problemaId}",
            // NO APLICA: la usa admin-api con X-Internal-Key; correos de inscritos, sin problemas.
            "GET /internal/cursos/{cursoId}/inscritos");

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mapeos;

    @Test
    void cada_ruta_esta_auditada() {
        Set<String> rutas = new TreeSet<>();
        mapeos.getHandlerMethods().forEach((info, metodo) -> {
            // Solo los controladores propios: el /error de Spring no es una ruta del proyecto.
            if (!metodo.getBeanType().getPackageName().startsWith("com.mastercode")) {
                return;
            }
            info.getMethodsCondition().getMethods().forEach(verbo ->
                    info.getPathPatternsCondition().getPatternValues().forEach(ruta -> rutas.add(verbo + " " + ruta)));
        });

        assertThat(rutas)
                .as("Ruta nueva sin auditar: revisa que no entregue problemas sin aprobar (HU31-T3) y agrégala a AUDITADAS")
                .isSubsetOf(AUDITADAS);
        assertThat(AUDITADAS)
                .as("AUDITADAS tiene rutas que ya no existen: actualiza la lista")
                .isSubsetOf(rutas);
    }
}
