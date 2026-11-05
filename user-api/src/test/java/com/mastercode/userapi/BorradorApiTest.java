package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mastercode.userapi.admin.AdminNoDisponibleException;
import com.mastercode.userapi.admin.ProblemaDto;
import com.mastercode.userapi.admin.ProblemaNoVisibleException;

// HU35-T2: GET y PUT /api/problemas/{id}/borrador. admin-api es un mock que dice qué problema es visible y de qué curso;
// los borradores se leen de la base para comprobar lo que realmente quedó guardado.
class BorradorApiTest extends PruebaDeApi {

    private static final long CURSO = 10;
    private static final long PROBLEMA = 7;
    private static final long OTRO_PROBLEMA = 8;

    private final ObjectMapper json = new ObjectMapper();

    private static ProblemaDto problema(long id, long curso) {
        return new ProblemaDto(id, "Suma de dos", "Lee dos enteros e imprime su suma.", "python", "facil", "web",
                "escritura_codigo", "", List.of(new ProblemaDto.Caso("2 3", "5")), curso);
    }

    private record Alumno(String email, long id) {
    }

    private Alumno alumnoInscrito() {
        String email = correoUnico();
        long id = crearEstudiante(email);
        inscribir(id, CURSO);
        return new Alumno(email, id);
    }

    private ResultActions guardar(String email, long problemaId, String codigo) throws Exception {
        return guardarCuerpo(email, problemaId, json.writeValueAsString(Map.of("codigo", codigo)));
    }

    private ResultActions guardarCuerpo(String email, long problemaId, String cuerpo) throws Exception {
        return mvc.perform(put("/api/problemas/" + problemaId + "/borrador").with(tokenDe(email, "estudiante"))
                .contentType(APPLICATION_JSON).content(cuerpo));
    }

    private ResultActions recuperar(String email, long problemaId) throws Exception {
        return mvc.perform(get("/api/problemas/" + problemaId + "/borrador").with(tokenDe(email, "estudiante")));
    }

    private int filas(long estudianteId) {
        return jdbc.sql("SELECT count(*) FROM app_schema.app_borrador WHERE estudiante_id = ?")
                .param(estudianteId).query(Integer.class).single();
    }

    // CA-35.1 y CA-35.4: lo que se guarda es lo que se recupera.
    @Test
    void guarda_el_borrador_y_lo_devuelve() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.problema(PROBLEMA)).thenReturn(problema(PROBLEMA, CURSO));

        guardar(yo.email(), PROBLEMA, "print(1 + 2)")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fecha_actualizacion").exists());

        recuperar(yo.email(), PROBLEMA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("print(1 + 2)"))
                .andExpect(jsonPath("$.fecha_actualizacion").exists());
    }

    @Test
    void un_segundo_guardado_reemplaza_al_primero_sin_crear_otra_fila() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.problema(PROBLEMA)).thenReturn(problema(PROBLEMA, CURSO));

        guardar(yo.email(), PROBLEMA, "primero").andExpect(status().isOk());
        guardar(yo.email(), PROBLEMA, "segundo").andExpect(status().isOk());

        recuperar(yo.email(), PROBLEMA).andExpect(jsonPath("$.codigo").value("segundo"));
        assertThat(filas(yo.id())).isEqualTo(1);
    }

    @Test
    void cada_problema_tiene_su_propio_borrador() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.problema(PROBLEMA)).thenReturn(problema(PROBLEMA, CURSO));
        when(admin.problema(OTRO_PROBLEMA)).thenReturn(problema(OTRO_PROBLEMA, CURSO));

        guardar(yo.email(), PROBLEMA, "uno").andExpect(status().isOk());
        guardar(yo.email(), OTRO_PROBLEMA, "dos").andExpect(status().isOk());

        recuperar(yo.email(), PROBLEMA).andExpect(jsonPath("$.codigo").value("uno"));
        recuperar(yo.email(), OTRO_PROBLEMA).andExpect(jsonPath("$.codigo").value("dos"));
    }

    @Test
    void sin_borrador_responde_200_con_codigo_y_fecha_nulos() throws Exception {
        Alumno yo = alumnoInscrito();

        recuperar(yo.email(), PROBLEMA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(nullValue()))
                .andExpect(jsonPath("$.fecha_actualizacion").value(nullValue()));
    }

    // D8: un borrador vacío gana al código base, así que no puede confundirse con «no hay borrador».
    @Test
    void un_borrador_vacio_se_guarda_y_se_devuelve_como_texto_vacio_no_como_nulo() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.problema(PROBLEMA)).thenReturn(problema(PROBLEMA, CURSO));

        guardar(yo.email(), PROBLEMA, "algo").andExpect(status().isOk());
        guardar(yo.email(), PROBLEMA, "").andExpect(status().isOk());

        recuperar(yo.email(), PROBLEMA)
                .andExpect(jsonPath("$.codigo").value(""))
                .andExpect(jsonPath("$.fecha_actualizacion").exists());
    }

    @Test
    void conserva_el_codigo_exacto_con_sangria_saltos_y_el_salto_final() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.problema(PROBLEMA)).thenReturn(problema(PROBLEMA, CURSO));
        String codigo = "def f(x):\n    if x:\n\treturn \"ñ\"  \n\n";

        guardar(yo.email(), PROBLEMA, codigo).andExpect(status().isOk());

        recuperar(yo.email(), PROBLEMA).andExpect(jsonPath("$.codigo").value(codigo));
    }

    // CA-35.4: el estudiante sale del token; el borrador de otro no se puede ver ni pisar.
    @Test
    void cada_estudiante_ve_solo_su_propio_borrador() throws Exception {
        Alumno yo = alumnoInscrito();
        Alumno otro = alumnoInscrito();
        when(admin.problema(PROBLEMA)).thenReturn(problema(PROBLEMA, CURSO));

        guardar(yo.email(), PROBLEMA, "mio").andExpect(status().isOk());
        guardar(otro.email(), PROBLEMA, "del otro").andExpect(status().isOk());

        recuperar(yo.email(), PROBLEMA).andExpect(jsonPath("$.codigo").value("mio"));
        recuperar(otro.email(), PROBLEMA).andExpect(jsonPath("$.codigo").value("del otro"));
        assertThat(filas(yo.id())).isEqualTo(1);
        assertThat(filas(otro.id())).isEqualTo(1);
    }

    // Es un dato propio del estudiante: leerlo no depende de que admin-api esté arriba.
    @Test
    void recuperar_no_consulta_a_admin_api() throws Exception {
        Alumno yo = alumnoInscrito();

        recuperar(yo.email(), PROBLEMA).andExpect(status().isOk());

        verifyNoInteractions(admin);
    }

    @Test
    void guardar_en_un_problema_no_visible_da_404_y_no_guarda_nada() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.problema(99)).thenThrow(new ProblemaNoVisibleException());

        guardar(yo.email(), 99, "codigo").andExpect(status().isNotFound());

        assertThat(filas(yo.id())).isZero();
    }

    @Test
    void guardar_en_un_curso_donde_no_esta_inscrito_da_el_mismo_404_y_no_guarda_nada() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.problema(PROBLEMA)).thenReturn(problema(PROBLEMA, 55));

        guardar(yo.email(), PROBLEMA, "codigo").andExpect(status().isNotFound());

        assertThat(filas(yo.id())).isZero();
    }

    @Test
    void si_admin_api_no_responde_al_guardar_da_503_y_no_guarda_nada() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.problema(PROBLEMA)).thenThrow(new AdminNoDisponibleException("sin conexión", null));

        guardar(yo.email(), PROBLEMA, "codigo").andExpect(status().isServiceUnavailable());

        assertThat(filas(yo.id())).isZero();
    }

    @Test
    void sin_el_campo_codigo_o_con_codigo_nulo_da_400() throws Exception {
        Alumno yo = alumnoInscrito();

        guardarCuerpo(yo.email(), PROBLEMA, "{}").andExpect(status().isBadRequest());
        guardarCuerpo(yo.email(), PROBLEMA, "{\"codigo\": null}").andExpect(status().isBadRequest());

        assertThat(filas(yo.id())).isZero();
        verify(admin, never()).problema(anyLong());
    }

    @Test
    void un_codigo_de_mas_de_50000_caracteres_da_400_y_uno_de_exactamente_50000_se_guarda() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.problema(PROBLEMA)).thenReturn(problema(PROBLEMA, CURSO));

        guardar(yo.email(), PROBLEMA, "a".repeat(50_001)).andExpect(status().isBadRequest());
        assertThat(filas(yo.id())).isZero();

        guardar(yo.email(), PROBLEMA, "a".repeat(50_000)).andExpect(status().isOk());
        assertThat(filas(yo.id())).isEqualTo(1);
    }

    // PostgreSQL no admite NUL en un texto: se quita en vez de responder 500.
    @Test
    void quita_los_caracteres_nul_en_vez_de_fallar() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.problema(PROBLEMA)).thenReturn(problema(PROBLEMA, CURSO));

        guardar(yo.email(), PROBLEMA, "a\0b").andExpect(status().isOk());

        recuperar(yo.email(), PROBLEMA).andExpect(jsonPath("$.codigo").value("ab"));
    }

    @Test
    void sin_token_da_401_y_con_token_de_docente_403() throws Exception {
        String cuerpo = "{\"codigo\": \"x\"}";
        mvc.perform(get("/api/problemas/" + PROBLEMA + "/borrador")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/problemas/" + PROBLEMA + "/borrador").contentType(APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/problemas/" + PROBLEMA + "/borrador").with(tokenDe(correoUnico(), "docente")))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/problemas/" + PROBLEMA + "/borrador").with(tokenDe(correoUnico(), "docente"))
                .contentType(APPLICATION_JSON).content(cuerpo)).andExpect(status().isForbidden());
    }
}
