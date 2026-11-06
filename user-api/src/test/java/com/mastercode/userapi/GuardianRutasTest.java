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
            // NO APLICA: solo los envíos del propio estudiante por id, sin contenido.
            "GET /api/avance",
            // OK: la lista sale de /internal/cursos/{id}/problemas/ de admin-api (visibles_para_estudiante()).
            "GET /api/cursos/{cursoId}/problemas",
            // OK: el problema sale de /internal/problemas/{id}/ de admin-api; sin aprobar da el mismo 404.
            "GET /api/problemas/{problemaId}",
            // OK: el problema sale de /internal/problemas/{id}/calificacion/ de admin-api (visibles_para_estudiante()),
            // sin aprobar da el mismo 404 y no se ejecuta nada. Califica con los casos ocultos pero a quien envía solo
            // le devuelve si cada uno pasó o falló (HU39); lo comprueba EnviosApiTest.
            "POST /api/problemas/{problemaId}/envios",
            // OK: los problemas salen de /internal/cursos/{id}/problemas/ de admin-api (visibles_para_estudiante()), igual
            // que el listado, y exige estar inscrito. Devuelve solo cuatro cuentas, sin contenido; lo comprueba
            // AvanceDelCursoApiTest (HU43-T1).
            "GET /api/cursos/{cursoId}/avance",
            // OK: el problema sale de /internal/problemas/{id}/ de admin-api (visibles_para_estudiante()); sin aprobar da
            // el mismo 404 y no se consulta ningún envío. Devuelve solo fecha, resultado y puntaje de los envíos propios,
            // sin enunciado, casos ni código; lo comprueba HistorialEnviosApiTest (HU41-T1).
            "GET /api/problemas/{problemaId}/envios",
            // NO APLICA: devuelve el envío (con su código) solo si es del estudiante del token; el de otro da 404 y no
            // incluye enunciado ni casos. Lo comprueba DetalleEnvioApiTest (HU42-T2).
            "GET /api/envios/{envioId}",
            // OK: la evaluación sale de /internal/evaluaciones/{id}/ de admin-api, que solo entrega ids de problemas
            // visibles (visibles_para_estudiante()). Fuera de la ventana da 403 sin ningún problema, y sin estar
            // inscrito en el curso da el mismo 404 que una evaluación inexistente; con el límite de tiempo vencido da 403
            // sin problemas. Lo comprueban EvaluacionAccesoApiTest (HU64-T2) y EvaluacionIntentoApiTest (HU64-T3).
            "GET /api/evaluaciones/{evaluacionId}",
            // NO APLICA: devuelve solo el código que el propio estudiante guardó (del token); no incluye enunciado ni
            // casos y sin borrador responde null. Lo comprueba BorradorApiTest (HU35-T2).
            "GET /api/problemas/{problemaId}/borrador",
            // OK: el problema sale de /internal/problemas/{id}/ de admin-api (visibles_para_estudiante()); sin aprobar o
            // sin estar inscrito da el mismo 404 y no se guarda nada. Lo comprueba BorradorApiTest (HU35-T2).
            "PUT /api/problemas/{problemaId}/borrador",
            // NO APLICA: la usa admin-api con X-Internal-Key; solo cuenta envíos por problema (intentos y correctos), sin
            // estudiantes, código ni casos. Lo comprueba ResumenEnviosInternoTest (HU44-T1).
            "GET /internal/envios/resumen",
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
