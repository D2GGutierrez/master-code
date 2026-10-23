package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.mastercode.userapi.admin.AdminNoDisponibleException;
import com.mastercode.userapi.admin.CodigoInvalidoException;
import com.mastercode.userapi.admin.CodigoVencidoException;
import com.mastercode.userapi.admin.CursoDto;

// HU26-T3 (CA-26.2 a CA-26.5): inscripción con el código de invitación.
class InscripcionApiTest extends PruebaDeApi {

    private static final CursoDto CURSO = new CursoDto(10, "Algoritmos I", "python", "ejemplo.edu.pe");

    private ResultActions pedirInscripcion(String email, String cuerpo) throws Exception {
        return mvc.perform(post("/api/inscripciones")
                .with(tokenDe(email, "estudiante"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    @Test
    void un_codigo_valido_inscribe_y_devuelve_201_con_el_curso() throws Exception {
        String email = correoUnico();
        long id = crearEstudiante(email);
        when(admin.cursoPorCodigo("ABCD2345")).thenReturn(CURSO);

        // Minúsculas y espacios: se normalizan antes de preguntar a admin-api.
        pedirInscripcion(email, "{\"codigo\":\"  abcd2345 \"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.curso_id").value(10))
                .andExpect(jsonPath("$.nombre").value("Algoritmos I"))
                .andExpect(jsonPath("$.lenguaje").value("python"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.fecha").exists());

        verify(admin).cursoPorCodigo("ABCD2345");
        assertThat(inscripcionesDe(id)).isEqualTo(1);
    }

    @Test
    void un_codigo_incorrecto_da_400_con_mensaje_y_no_inscribe() throws Exception {
        String email = correoUnico();
        long id = crearEstudiante(email);
        when(admin.cursoPorCodigo("ZZZZ9999")).thenThrow(new CodigoInvalidoException());

        pedirInscripcion(email, "{\"codigo\":\"ZZZZ9999\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El código de invitación no es correcto. Revísalo con tu docente."));

        assertThat(inscripcionesDe(id)).isZero();
    }

    @Test
    void un_codigo_vencido_da_400_con_mensaje_y_no_inscribe() throws Exception {
        String email = correoUnico();
        long id = crearEstudiante(email);
        when(admin.cursoPorCodigo("VIEJO234")).thenThrow(new CodigoVencidoException());

        pedirInscripcion(email, "{\"codigo\":\"VIEJO234\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El código de invitación venció. Pide uno nuevo a tu docente."));

        assertThat(inscripcionesDe(id)).isZero();
    }

    @Test
    void un_codigo_con_caracteres_raros_se_rechaza_sin_llamar_a_admin_api() throws Exception {
        String email = correoUnico();
        crearEstudiante(email);

        pedirInscripcion(email, "{\"codigo\":\"../admin\"}").andExpect(status().isBadRequest());

        verifyNoInteractions(admin);
    }

    @Test
    void un_codigo_vacio_o_ausente_da_400_sin_llamar_a_admin_api() throws Exception {
        String email = correoUnico();
        crearEstudiante(email);

        pedirInscripcion(email, "{\"codigo\":\"   \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Escribe el código de invitación."));
        pedirInscripcion(email, "{}").andExpect(status().isBadRequest());

        verifyNoInteractions(admin);
    }

    @Test
    void inscribirse_dos_veces_al_mismo_curso_da_409_y_deja_una_sola_inscripcion() throws Exception {
        String email = correoUnico();
        long id = crearEstudiante(email);
        when(admin.cursoPorCodigo("ABCD2345")).thenReturn(CURSO);

        pedirInscripcion(email, "{\"codigo\":\"ABCD2345\"}").andExpect(status().isCreated());
        pedirInscripcion(email, "{\"codigo\":\"ABCD2345\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya estás inscrito en este curso."));

        assertThat(inscripcionesDe(id)).isEqualTo(1);
    }

    @Test
    void si_admin_api_no_responde_da_503_y_no_inscribe() throws Exception {
        String email = correoUnico();
        long id = crearEstudiante(email);
        when(admin.cursoPorCodigo("ABCD2345")).thenThrow(new AdminNoDisponibleException("sin conexión", null));

        pedirInscripcion(email, "{\"codigo\":\"ABCD2345\"}").andExpect(status().isServiceUnavailable());

        assertThat(inscripcionesDe(id)).isZero();
    }

    @Test
    void un_token_de_docente_recibe_403_y_no_llega_a_admin_api() throws Exception {
        mvc.perform(post("/api/inscripciones")
                        .with(tokenDe(correoUnico(), "docente"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"ABCD2345\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(admin);
    }

    @Test
    void sin_token_da_401() throws Exception {
        mvc.perform(post("/api/inscripciones").contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"ABCD2345\"}"))
                .andExpect(status().isUnauthorized());
    }
}
