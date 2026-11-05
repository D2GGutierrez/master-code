package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.mastercode.userapi.admin.AdminNoDisponibleException;
import com.mastercode.userapi.admin.ProblemaCalificableDto;
import com.mastercode.userapi.admin.ProblemaResumenDto;
import com.mastercode.userapi.problema.AvanceDelCursoServicio;
import com.mastercode.userapi.sandbox.EjecucionResultado;
import com.mastercode.userapi.sandbox.Estado;
import com.mastercode.userapi.sandbox.SandboxNoDisponibleException;

// HU43-T3 (CA-43.1): el avance de un curso se guarda en caché y se descarta con cada envío nuevo del estudiante. admin-api
// es un mock: cuántas veces se le pregunta dice si el avance salió de la caché o se calculó de nuevo. Los envíos se
// siembran directo en la base (no invalidan, a propósito) o se hacen por la API (sí invalidan).
class AvanceCacheApiTest extends PruebaDeApi {

    private static final long CURSO = 10;
    private static final long OTRO_CURSO = 11;
    private static final OffsetDateTime FECHA = OffsetDateTime.parse("2026-10-28T15:00:00Z");
    private static final ProblemaResumenDto SUMA = new ProblemaResumenDto(7, "Suma", "python", "facil", "ambas",
            "escritura_codigo", FECHA);

    @Autowired
    private AvanceDelCursoServicio servicio;

    @Autowired
    private CacheManager cacheManager;

    private record Alumno(String email, long id) {
    }

    private Alumno alumnoInscrito() {
        String email = correoUnico();
        long id = crearEstudiante(email);
        inscribir(id, CURSO);
        return new Alumno(email, id);
    }

    private ResultActions avance(Alumno alumno, long curso, String plataforma) throws Exception {
        String consulta = plataforma == null ? "" : "?plataforma=" + plataforma;
        return mvc.perform(get("/api/cursos/" + curso + "/avance" + consulta).with(tokenDe(alumno.email(), "estudiante")));
    }

    private ResultActions avance(Alumno alumno) throws Exception {
        return avance(alumno, CURSO, null);
    }

    // El programa del estudiante imprime siempre 5: correcto para el caso «2 3» -> «5».
    private ResultActions enviarCorrecto(Alumno alumno, long problemaId, long curso) throws Exception {
        when(admin.problemaParaCalificar(problemaId)).thenReturn(new ProblemaCalificableDto(problemaId, curso, "python",
                "escritura_codigo", List.of(new ProblemaCalificableDto.Caso(1, "2 3", "5", true))));
        when(sandbox.ejecutar(any())).thenReturn(new EjecucionResultado(Estado.OK, "5", "", 5L));
        return mvc.perform(post("/api/problemas/" + problemaId + "/envios").with(tokenDe(alumno.email(), "estudiante"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\": \"print(5)\"}"));
    }

    @Test
    void la_segunda_consulta_sale_de_la_cache_y_no_vuelve_a_preguntar_a_admin_api() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.problemasDelCurso(CURSO, null)).thenReturn(List.of(SUMA));

        avance(yo).andExpect(status().isOk()).andExpect(jsonPath("$.pendientes").value(1));
        avance(yo).andExpect(status().isOk()).andExpect(jsonPath("$.pendientes").value(1));

        verify(admin, times(1)).problemasDelCurso(CURSO, null);
    }

    // Prueba de que de verdad se lee de la caché: un envío sembrado en la base no la invalida, así que el avance no se
    // entera hasta que se descarta.
    @Test
    void mientras_no_se_invalide_se_sigue_devolviendo_lo_guardado() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.problemasDelCurso(CURSO, null)).thenReturn(List.of(SUMA));
        avance(yo).andExpect(jsonPath("$.pendientes").value(1));

        registrarEnvio(yo.id(), 7, "web", "correcto");

        avance(yo).andExpect(jsonPath("$.resueltos").value(0)).andExpect(jsonPath("$.pendientes").value(1));
        servicio.invalidar(yo.id(), CURSO);
        avance(yo).andExpect(jsonPath("$.resueltos").value(1)).andExpect(jsonPath("$.pendientes").value(0));
    }

    @Test
    void un_envio_nuevo_por_la_api_invalida_el_avance_del_curso() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.problemasDelCurso(CURSO, null)).thenReturn(List.of(SUMA));
        avance(yo).andExpect(jsonPath("$.resueltos").value(0)).andExpect(jsonPath("$.pendientes").value(1));

        enviarCorrecto(yo, 7, CURSO).andExpect(status().isCreated());

        avance(yo).andExpect(jsonPath("$.resueltos").value(1)).andExpect(jsonPath("$.pendientes").value(0));
        verify(admin, times(2)).problemasDelCurso(CURSO, null);
    }

    @Test
    void el_envio_nuevo_descarta_el_avance_de_todos_los_filtros_de_plataforma() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.problemasDelCurso(CURSO, null)).thenReturn(List.of(SUMA));
        when(admin.problemasDelCurso(CURSO, "web")).thenReturn(List.of(SUMA));
        when(admin.problemasDelCurso(CURSO, "movil")).thenReturn(List.of(SUMA));
        for (String plataforma : new String[] { null, "web", "movil" }) {
            avance(yo, CURSO, plataforma).andExpect(jsonPath("$.pendientes").value(1));
        }

        enviarCorrecto(yo, 7, CURSO).andExpect(status().isCreated());

        for (String plataforma : new String[] { null, "web", "movil" }) {
            avance(yo, CURSO, plataforma).andExpect(jsonPath("$.resueltos").value(1));
        }
    }

    @Test
    void cada_filtro_de_plataforma_tiene_su_propia_entrada() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.problemasDelCurso(CURSO, null)).thenReturn(List.of(SUMA, SUMA));
        when(admin.problemasDelCurso(CURSO, "web")).thenReturn(List.of(SUMA));

        avance(yo, CURSO, null).andExpect(jsonPath("$.total").value(2));
        avance(yo, CURSO, "web").andExpect(jsonPath("$.total").value(1));
        avance(yo, CURSO, null).andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void el_avance_guardado_de_un_estudiante_no_se_le_da_a_otro() throws Exception {
        Alumno yo = alumnoInscrito();
        Alumno otro = alumnoInscrito();
        registrarEnvio(otro.id(), 7, "web", "correcto");
        when(admin.problemasDelCurso(CURSO, null)).thenReturn(List.of(SUMA));

        avance(yo).andExpect(jsonPath("$.resueltos").value(0));
        avance(otro).andExpect(jsonPath("$.resueltos").value(1));
        avance(yo).andExpect(jsonPath("$.resueltos").value(0));
    }

    @Test
    void un_envio_en_otro_curso_no_descarta_el_avance_de_este() throws Exception {
        Alumno yo = alumnoInscrito();
        inscribir(yo.id(), OTRO_CURSO);
        when(admin.problemasDelCurso(CURSO, null)).thenReturn(List.of(SUMA));
        avance(yo).andExpect(status().isOk());

        enviarCorrecto(yo, 8, OTRO_CURSO).andExpect(status().isCreated());
        avance(yo).andExpect(status().isOk());

        verify(admin, times(1)).problemasDelCurso(CURSO, null);
    }

    // Si el envío no se guardó (el motor falló) el avance no cambió: no hay por qué descartarlo.
    @Test
    void un_envio_que_no_se_guarda_no_descarta_el_avance() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.problemasDelCurso(CURSO, null)).thenReturn(List.of(SUMA));
        avance(yo).andExpect(status().isOk());
        when(admin.problemaParaCalificar(7)).thenReturn(new ProblemaCalificableDto(7, CURSO, "python",
                "escritura_codigo", List.of(new ProblemaCalificableDto.Caso(1, "2 3", "5", true))));
        when(sandbox.ejecutar(any())).thenThrow(new SandboxNoDisponibleException("sin conexión", null));

        mvc.perform(post("/api/problemas/7/envios").with(tokenDe(yo.email(), "estudiante"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\": \"print(5)\"}"))
                .andExpect(status().isServiceUnavailable());
        avance(yo).andExpect(status().isOk());

        verify(admin, times(1)).problemasDelCurso(CURSO, null);
    }

    @Test
    void un_error_no_se_guarda_en_la_cache() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.problemasDelCurso(CURSO, null)).thenThrow(new AdminNoDisponibleException("sin conexión", null));
        avance(yo).andExpect(status().isServiceUnavailable());

        // doReturn y no when: when(...) llamaría al mock, que aún está programado para lanzar la excepción.
        doReturn(List.of(SUMA)).when(admin).problemasDelCurso(CURSO, null);

        avance(yo).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
    }

    // La caché vence sola: el docente puede aprobar o archivar problemas sin que user-api se entere (ADR-8 D9).
    @Test
    void la_cache_vence_a_los_cinco_minutos() {
        CaffeineCache cache = (CaffeineCache) cacheManager.getCache("avance");

        assertThat(cache.getNativeCache().policy().expireAfterWrite().orElseThrow().getExpiresAfter(TimeUnit.MINUTES))
                .isEqualTo(5);
    }
}
