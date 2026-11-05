package com.mastercode.userapi.problema;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.mastercode.userapi.admin.AdminCliente;
import com.mastercode.userapi.admin.ProblemaResumenDto;

// HU43-T1 y HU43-T3: el avance de un estudiante en un curso, con su caché (ADR-8 D9). Va en un bean aparte porque la
// caché de Spring funciona por un proxy: solo actúa cuando otro bean llama al método.
@Service
public class AvanceDelCursoServicio {

    static final String CACHE = "avance";

    // Las plataformas por las que se puede filtrar (null = sin filtro): cada una es una entrada distinta en la caché.
    private static final List<String> FILTROS = Arrays.asList(null, "web", "movil");

    // El avance de un estudiante en un curso (HU43): cuántos de sus problemas están resueltos, intentados y pendientes.
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AvanceDelCurso(long cursoId, int total, int resueltos, int intentados, int pendientes) {
    }

    private final AdminCliente admin;
    private final EstadoProblemasRepository estados;
    private final CacheManager cacheManager;

    public AvanceDelCursoServicio(AdminCliente admin, EstadoProblemasRepository estados, CacheManager cacheManager) {
        this.admin = admin;
        this.estados = estados;
        this.cacheManager = cacheManager;
    }

    // La clave lleva al estudiante: nadie lee el avance de otro. Una excepción (curso inexistente, admin-api caído) no
    // se guarda en caché: la siguiente consulta vuelve a calcular.
    public static String clave(long estudianteId, long cursoId, String plataforma) {
        return estudianteId + ":" + cursoId + ":" + (plataforma == null ? "" : plataforma);
    }

    // Cuenta los problemas que ese estudiante ve en el curso (los que entrega admin-api: aprobados y no descartados) con
    // el mismo estado que muestra el listado (HU30-T2). plataforma es null, «web» o «movil», ya validada.
    @Cacheable(cacheNames = CACHE,
            key = "T(com.mastercode.userapi.problema.AvanceDelCursoServicio).clave(#estudianteId, #cursoId, #plataforma)")
    public AvanceDelCurso calcular(long estudianteId, long cursoId, String plataforma) {
        List<ProblemaResumenDto> problemas = admin.problemasDelCurso(cursoId, plataforma);

        Map<Long, Boolean> resueltos = problemas.isEmpty() ? Map.of() : estados.resueltosPorProblema(estudianteId);
        int resuelto = 0;
        int intentado = 0;
        for (ProblemaResumenDto problema : problemas) {
            EstadoProblema estado = EstadoProblema.de(resueltos.get(problema.id()));
            if (estado == EstadoProblema.RESUELTO) {
                resuelto++;
            } else if (estado == EstadoProblema.INTENTADO) {
                intentado++;
            }
        }
        int total = problemas.size();
        return new AvanceDelCurso(cursoId, total, resuelto, intentado, total - resuelto - intentado);
    }

    // Se llama con cada envío nuevo del estudiante en ese curso: descarta el avance de todos los filtros de plataforma.
    // Lo que cambie sin pasar por aquí (el docente aprueba o archiva un problema) lo corrige el vencimiento de la caché.
    public void invalidar(long estudianteId, long cursoId) {
        Cache cache = cacheManager.getCache(CACHE);
        for (String plataforma : FILTROS) {
            cache.evict(clave(estudianteId, cursoId, plataforma));
        }
    }
}
