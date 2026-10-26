package com.mastercode.userapi;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.mastercode.userapi.admin.ProblemaResumenDto;

// HU30-T1 (filtros de dificultad y de estado) y HU30-T2 (estado de cada problema a partir de los envíos).
// Los envíos se siembran en app_envio; admin-api se reemplaza por un mock que entrega los problemas visibles.
class ProblemasFiltrosYEstadoApiTest extends PruebaDeApi {

    private static final OffsetDateTime FECHA = OffsetDateTime.parse("2026-10-28T15:00:00Z");

    // Tres problemas del curso 10, uno por dificultad.
    private static final ProblemaResumenDto FACIL = new ProblemaResumenDto(7, "Suma", "python", "facil", "web",
            "escritura_codigo", FECHA);
    private static final ProblemaResumenDto MEDIO = new ProblemaResumenDto(8, "¿Qué imprime?", "python", "medio",
            "movil", "prediccion_salida", FECHA);
    private static final ProblemaResumenDto DIFICIL = new ProblemaResumenDto(9, "Ordenar", "python", "dificil", "web",
            "escritura_codigo", FECHA);

    private static final String LISTADO = "/api/cursos/10/problemas";

    // ---------- estado (HU30-T2, CA-30.3) ----------

    @Test
    void cada_problema_sale_como_resuelto_intentado_o_sin_abrir() throws Exception {
        String email = correoUnico();
        long yo = crearEstudiante(email);
        inscribir(yo, 10);
        registrarEnvio(yo, 7, "web", "correcto");
        registrarEnvio(yo, 8, "movil", "incorrecto");
        when(admin.problemasDelCurso(10, null)).thenReturn(List.of(FACIL, MEDIO, DIFICIL));

        mvc.perform(get(LISTADO).with(tokenDe(email, "estudiante")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].estado").value("resuelto"))
                .andExpect(jsonPath("$[1].id").value(8))
                .andExpect(jsonPath("$[1].estado").value("intentado"))
                .andExpect(jsonPath("$[2].id").value(9))
                .andExpect(jsonPath("$[2].estado").value("sin_abrir"));
    }

    @Test
    void un_envio_correcto_basta_aunque_haya_otros_que_fallaron() throws Exception {
        String email = correoUnico();
        long yo = crearEstudiante(email);
        inscribir(yo, 10);
        registrarEnvio(yo, 7, "web", "incorrecto");
        registrarEnvio(yo, 7, "web", "correcto");
        registrarEnvio(yo, 7, "web", "incorrecto");
        when(admin.problemasDelCurso(10, null)).thenReturn(List.of(FACIL));

        mvc.perform(get(LISTADO).with(tokenDe(email, "estudiante")))
                .andExpect(jsonPath("$[0].estado").value("resuelto"));
    }

    @Test
    void un_envio_sin_calificar_cuenta_como_intento_y_no_como_resuelto() throws Exception {
        String email = correoUnico();
        long yo = crearEstudiante(email);
        inscribir(yo, 10);
        registrarEnvio(yo, 7, "web", null);
        when(admin.problemasDelCurso(10, null)).thenReturn(List.of(FACIL));

        mvc.perform(get(LISTADO).with(tokenDe(email, "estudiante")))
                .andExpect(jsonPath("$[0].estado").value("intentado"));
    }

    @Test
    void los_envios_de_web_y_de_movil_cuentan_igual_porque_son_de_la_misma_cuenta() throws Exception {
        String email = correoUnico();
        long yo = crearEstudiante(email);
        inscribir(yo, 10);
        registrarEnvio(yo, 8, "movil", "correcto");
        when(admin.problemasDelCurso(10, null)).thenReturn(List.of(MEDIO));

        mvc.perform(get(LISTADO).with(tokenDe(email, "estudiante")))
                .andExpect(jsonPath("$[0].estado").value("resuelto"));
    }

    @Test
    void los_envios_de_otro_estudiante_no_cambian_mi_estado() throws Exception {
        String email = correoUnico();
        long yo = crearEstudiante(email);
        long otro = crearEstudiante(correoUnico());
        inscribir(yo, 10);
        registrarEnvio(otro, 7, "web", "correcto");
        when(admin.problemasDelCurso(10, null)).thenReturn(List.of(FACIL));

        mvc.perform(get(LISTADO).with(tokenDe(email, "estudiante")))
                .andExpect(jsonPath("$[0].estado").value("sin_abrir"));
    }

    @Test
    void un_curso_sin_problemas_responde_lista_vacia() throws Exception {
        String email = correoUnico();
        inscribir(crearEstudiante(email), 10);
        when(admin.problemasDelCurso(10, null)).thenReturn(List.of());

        mvc.perform(get(LISTADO).with(tokenDe(email, "estudiante")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ---------- filtros (HU30-T1, CA-30.1, CA-30.2 y CA-30.5) ----------

    // El estudiante tiene el 7 resuelto, el 8 intentado y el 9 sin abrir.
    private String estudianteConEstados() {
        String email = correoUnico();
        long yo = crearEstudiante(email);
        inscribir(yo, 10);
        registrarEnvio(yo, 7, "web", "correcto");
        registrarEnvio(yo, 8, "movil", "incorrecto");
        when(admin.problemasDelCurso(10, null)).thenReturn(List.of(FACIL, MEDIO, DIFICIL));
        return email;
    }

    @Test
    void filtra_por_dificultad() throws Exception {
        String email = estudianteConEstados();

        mvc.perform(get(LISTADO + "?dificultad=medio").with(tokenDe(email, "estudiante")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(8));
    }

    @Test
    void filtra_por_estado() throws Exception {
        String email = estudianteConEstados();

        mvc.perform(get(LISTADO + "?estado=resuelto").with(tokenDe(email, "estudiante")))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(7));
        mvc.perform(get(LISTADO + "?estado=intentado").with(tokenDe(email, "estudiante")))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(8));
        mvc.perform(get(LISTADO + "?estado=sin_abrir").with(tokenDe(email, "estudiante")))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(9));
    }

    @Test
    void combina_dificultad_y_estado() throws Exception {
        String email = estudianteConEstados();

        mvc.perform(get(LISTADO + "?dificultad=facil&estado=resuelto").with(tokenDe(email, "estudiante")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].estado").value("resuelto"));
    }

    @Test
    void una_combinacion_sin_resultados_responde_lista_vacia_y_no_un_error() throws Exception {
        String email = estudianteConEstados();

        // El problema medio está intentado, no resuelto: ninguno cumple las dos condiciones.
        mvc.perform(get(LISTADO + "?dificultad=medio&estado=resuelto").with(tokenDe(email, "estudiante")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void los_filtros_se_combinan_con_la_plataforma() throws Exception {
        String email = correoUnico();
        long yo = crearEstudiante(email);
        inscribir(yo, 10);
        registrarEnvio(yo, 7, "web", "correcto");
        when(admin.problemasDelCurso(10, "web")).thenReturn(List.of(FACIL, DIFICIL));

        mvc.perform(get(LISTADO + "?plataforma=web&estado=sin_abrir").with(tokenDe(email, "estudiante")))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(9));

        // La plataforma la filtra admin-api; dificultad y estado se aplican aquí y no viajan a admin-api.
        verify(admin).problemasDelCurso(10, "web");
    }

    @Test
    void sin_filtros_o_con_filtros_vacios_salen_todos() throws Exception {
        String email = estudianteConEstados();

        mvc.perform(get(LISTADO).with(tokenDe(email, "estudiante")))
                .andExpect(jsonPath("$", hasSize(3)));
        mvc.perform(get(LISTADO + "?dificultad=&estado=").with(tokenDe(email, "estudiante")))
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void un_valor_de_filtro_que_no_existe_da_400_sin_llamar_a_admin_api() throws Exception {
        String email = correoUnico();
        inscribir(crearEstudiante(email), 10);

        mvc.perform(get(LISTADO + "?dificultad=imposible").with(tokenDe(email, "estudiante")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("dificultad debe ser facil, medio o dificil."));
        mvc.perform(get(LISTADO + "?estado=terminado").with(tokenDe(email, "estudiante")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("estado debe ser sin_abrir, intentado o resuelto."));
        // Los textos son exactos: sin mayúsculas ni espacios en lugar de guion bajo.
        mvc.perform(get(LISTADO + "?estado=RESUELTO").with(tokenDe(email, "estudiante")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(admin);
    }

    @Test
    void un_estudiante_no_inscrito_recibe_403_aunque_mande_filtros() throws Exception {
        String email = correoUnico();
        crearEstudiante(email);

        mvc.perform(get(LISTADO + "?dificultad=facil&estado=resuelto").with(tokenDe(email, "estudiante")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(admin);
    }
}
