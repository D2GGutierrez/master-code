package com.mastercode.userapi.admin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// Cliente de la API interna de admin-api (D3): así user-api conoce los cursos sin leer admin_schema.
// La clave X-Internal-Key va en todas las peticiones (se pone al armar el RestClient, en AdminConfig).
public class AdminCliente {

    private final RestClient http;

    public AdminCliente(RestClient http) {
        this.http = http;
    }

    // El código debe llegar ya normalizado y validado: aquí va dentro de la ruta.
    public CursoDto cursoPorCodigo(String codigo) {
        CursoDto curso;
        try {
            curso = http.get()
                    .uri("/internal/cursos/por-codigo/{codigo}/", codigo)
                    .retrieve()
                    .onStatus(estado -> estado.value() == 404, (peticion, respuesta) -> {
                        throw new CodigoInvalidoException();
                    })
                    .onStatus(estado -> estado.value() == 410, (peticion, respuesta) -> {
                        throw new CodigoVencidoException();
                    })
                    .body(CursoDto.class);
        } catch (RestClientException e) {
            // Sin conexión, tiempo agotado, 5xx, 403 por clave mala o cuerpo ilegible.
            throw new AdminNoDisponibleException("admin-api no pudo validar el código", e);
        }
        if (curso == null) {
            throw new AdminNoDisponibleException("admin-api devolvió una respuesta vacía", null);
        }
        return curso;
    }

    // Devuelve los que existan; un curso eliminado simplemente no aparece.
    public List<CursoDto> cursosPorIds(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        String lista = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        try {
            List<CursoDto> cursos = http.get()
                    .uri("/internal/cursos/?ids={ids}", lista)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<CursoDto>>() {
                    });
            if (cursos == null) {
                throw new AdminNoDisponibleException("admin-api devolvió una respuesta vacía", null);
            }
            return cursos;
        } catch (RestClientException e) {
            throw new AdminNoDisponibleException("admin-api no pudo entregar los cursos", e);
        }
    }

    // HU25-T2 y HU25-T4: los cursos publicados (con al menos un problema visible). institucion y buscar son opcionales
    // (null no filtra). Los valores van como variables de la plantilla y no pegados a la ruta, para que RestClient
    // los codifique: un buscar con «&» o «#» no puede agregar ni cambiar otros parámetros.
    public List<CursoDto> cursosPublicados(String institucion, String buscar) {
        StringBuilder ruta = new StringBuilder("/internal/cursos/publicados/");
        List<Object> variables = new ArrayList<>();
        String separador = "?";
        if (institucion != null) {
            ruta.append(separador).append("institucion={institucion}");
            variables.add(institucion);
            separador = "&";
        }
        if (buscar != null) {
            ruta.append(separador).append("buscar={buscar}");
            variables.add(buscar);
        }
        try {
            List<CursoDto> cursos = http.get()
                    .uri(ruta.toString(), variables.toArray())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<CursoDto>>() {
                    });
            if (cursos == null) {
                throw new AdminNoDisponibleException("admin-api devolvió una respuesta vacía", null);
            }
            return cursos;
        } catch (RestClientException e) {
            throw new AdminNoDisponibleException("admin-api no pudo entregar los cursos publicados", e);
        }
    }

    // HU29-T1 y HU31-T1: los problemas que el estudiante puede ver en un curso; admin-api ya deja solo los aprobados.
    // plataforma "web" o "movil" (validada antes de llegar aquí) deja los de esa plataforma y los de ambas; null no filtra.
    public List<ProblemaResumenDto> problemasDelCurso(long cursoId, String plataforma) {
        String ruta = "/internal/cursos/{id}/problemas/" + (plataforma == null ? "" : "?plataforma={plataforma}");
        Object[] variables = plataforma == null ? new Object[] { cursoId } : new Object[] { cursoId, plataforma };
        List<ProblemaResumenDto> problemas;
        try {
            problemas = http.get()
                    .uri(ruta, variables)
                    .retrieve()
                    .onStatus(estado -> estado.value() == 404, (peticion, respuesta) -> {
                        throw new CursoNoExisteException();
                    })
                    .body(new ParameterizedTypeReference<List<ProblemaResumenDto>>() {
                    });
        } catch (RestClientException e) {
            throw new AdminNoDisponibleException("admin-api no pudo entregar los problemas del curso", e);
        }
        if (problemas == null) {
            throw new AdminNoDisponibleException("admin-api devolvió una respuesta vacía", null);
        }
        return problemas;
    }

    // HU31-T2 y HU32-T1: un problema por id. Lanza ProblemaNoVisibleException si admin-api responde 404 (no existe,
    // no está aprobado o fue descartado).
    public ProblemaDto problema(long problemaId) {
        ProblemaDto problema;
        try {
            problema = http.get()
                    .uri("/internal/problemas/{id}/", problemaId)
                    .retrieve()
                    .onStatus(estado -> estado.value() == 404, (peticion, respuesta) -> {
                        throw new ProblemaNoVisibleException();
                    })
                    .body(ProblemaDto.class);
        } catch (RestClientException e) {
            throw new AdminNoDisponibleException("admin-api no pudo entregar el problema", e);
        }
        if (problema == null) {
            throw new AdminNoDisponibleException("admin-api devolvió una respuesta vacía", null);
        }
        return problema;
    }
}
