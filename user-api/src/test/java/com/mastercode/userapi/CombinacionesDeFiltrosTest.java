package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;

import com.jayway.jsonpath.JsonPath;
import com.mastercode.userapi.admin.ProblemaResumenDto;

// HU30-T4 (CA-030.1, 030.2, 030.3 y 030.5): combinaciones de filtros del listado. Dificultad y estado se aplican aquí,
// en user-api, sobre la lista que entrega admin-api (que ya trae solo los visibles, ver HU31-T4): por eso el mock de
// AdminCliente es correcto en esta capa. Completa a ProblemasFiltrosYEstadoApiTest, que usa un problema por dificultad:
// con dos por dificultad y estados mezclados, un AND y un OR ya no dan lo mismo.
class CombinacionesDeFiltrosTest extends PruebaDeApi {

    private static final OffsetDateTime FECHA = OffsetDateTime.parse("2026-10-28T15:00:00Z");

    // Ids altos y al azar: no chocan con datos de la base de desarrollo.
    private final long curso = ThreadLocalRandom.current().nextLong(1_000_000_000L, 1_000_000_000_000L);
    private final long base = ThreadLocalRandom.current().nextLong(1_000_000_000L, 1_000_000_000_000L);
    private final long facilA = base + 1;
    private final long facilB = base + 2;
    private final long medioA = base + 3;
    private final long medioB = base + 4;
    private final long dificilA = base + 5;
    private final long dificilB = base + 6;

    private ProblemaResumenDto problema(long id, String dificultad, String plataforma) {
        return new ProblemaResumenDto(id, "Problema " + id, "python", dificultad, plataforma, "escritura_codigo", FECHA);
    }

    private List<ProblemaResumenDto> seisProblemas() {
        return List.of(problema(facilA, "facil", "web"), problema(facilB, "facil", "movil"),
                problema(medioA, "medio", "web"), problema(medioB, "medio", "movil"),
                problema(dificilA, "dificil", "web"), problema(dificilB, "dificil", "movil"));
    }

    // Estados de este estudiante: facilA resuelto, medioA intentado, medioB resuelto, dificilB intentado;
    // facilB y dificilA sin abrir.
    private String estudianteConEnvios() {
        String email = correoUnico();
        long id = crearEstudiante(email);
        inscribir(id, curso);
        registrarEnvio(id, facilA, "web", "correcto");
        registrarEnvio(id, medioA, "web", "incorrecto");
        registrarEnvio(id, medioB, "movil", "incorrecto");
        registrarEnvio(id, medioB, "movil", "correcto");
        registrarEnvio(id, dificilB, "movil", null);
        return email;
    }

    // Los ids que devuelve el listado con esos filtros, en el orden en que llegan.
    private List<Long> ids(String email, String filtros) throws Exception {
        String cuerpo = mvc.perform(get("/api/cursos/{id}/problemas?" + filtros, curso).with(tokenDe(email, "estudiante")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Number> ids = JsonPath.read(cuerpo, "$[*].id");
        return ids.stream().map(Number::longValue).toList();
    }

    @Test
    void cada_dificultad_devuelve_exactamente_sus_problemas() throws Exception {
        String email = estudianteConEnvios();
        when(admin.problemasDelCurso(curso, null)).thenReturn(seisProblemas());

        assertThat(ids(email, "dificultad=facil")).containsExactly(facilA, facilB);
        assertThat(ids(email, "dificultad=medio")).containsExactly(medioA, medioB);
        assertThat(ids(email, "dificultad=dificil")).containsExactly(dificilA, dificilB);
    }

    @Test
    void dificultad_y_estado_dan_la_interseccion_y_no_la_union() throws Exception {
        String email = estudianteConEnvios();
        when(admin.problemasDelCurso(curso, null)).thenReturn(seisProblemas());

        // Con OR saldrían también facilB (fácil) y medioB (resuelto).
        assertThat(ids(email, "dificultad=facil&estado=resuelto")).containsExactly(facilA);
        assertThat(ids(email, "dificultad=medio&estado=intentado")).containsExactly(medioA);
        assertThat(ids(email, "dificultad=dificil&estado=sin_abrir")).containsExactly(dificilA);
        // Un envío sin resultado cuenta como intento (V4): dificilB es intentado y no sin abrir.
        assertThat(ids(email, "dificultad=dificil&estado=intentado")).containsExactly(dificilB);
    }

    @Test
    void un_solo_filtro_sin_coincidencias_responde_200_con_lista_vacia() throws Exception {
        // Un estudiante sin envíos y un curso sin problemas difíciles.
        String email = correoUnico();
        inscribir(crearEstudiante(email), curso);
        when(admin.problemasDelCurso(curso, null)).thenReturn(List.of(problema(facilA, "facil", "web"),
                problema(medioA, "medio", "web")));

        for (String filtro : List.of("estado=resuelto", "estado=intentado", "dificultad=dificil")) {
            String cuerpo = mvc.perform(get("/api/cursos/{id}/problemas?" + filtro, curso)
                            .with(tokenDe(email, "estudiante")))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            assertThat(cuerpo).as(filtro).isEqualTo("[]");
        }
        // Control: sin filtro sí están los dos.
        assertThat(ids(email, "")).containsExactly(facilA, medioA);
    }

    @Test
    void los_envios_de_otro_estudiante_no_cambian_el_filtro_por_estado() throws Exception {
        String yo = estudianteConEnvios();
        String otroEmail = correoUnico();
        long otro = crearEstudiante(otroEmail);
        inscribir(otro, curso);
        // El otro resolvió justo los que yo no abrí.
        registrarEnvio(otro, facilB, "web", "correcto");
        registrarEnvio(otro, dificilA, "web", "correcto");
        when(admin.problemasDelCurso(curso, null)).thenReturn(seisProblemas());

        assertThat(ids(yo, "estado=resuelto")).containsExactly(facilA, medioB);
        assertThat(ids(yo, "estado=sin_abrir")).containsExactly(facilB, dificilA);
        assertThat(ids(otroEmail, "estado=resuelto")).containsExactly(facilB, dificilA);
    }

    @Test
    void dificultad_y_plataforma_tambien_dan_la_interseccion() throws Exception {
        String email = estudianteConEnvios();
        // La plataforma la filtra admin-api: aquí entrega solo los de web.
        when(admin.problemasDelCurso(curso, "web")).thenReturn(List.of(problema(facilA, "facil", "web"),
                problema(medioA, "medio", "web"), problema(dificilA, "dificil", "web")));

        assertThat(ids(email, "plataforma=web&dificultad=medio")).containsExactly(medioA);
        assertThat(ids(email, "plataforma=web&dificultad=facil&estado=sin_abrir")).isEmpty();
        verify(admin, times(2)).problemasDelCurso(curso, "web");
    }
}
