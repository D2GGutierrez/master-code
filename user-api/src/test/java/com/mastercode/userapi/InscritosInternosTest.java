package com.mastercode.userapi;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

// HU28-T1 y D3: admin-api pide los inscritos de un curso por la API interna, con X-Internal-Key.
class InscritosInternosTest extends PruebaDeApi {

    private static final String RUTA = "/internal/cursos/{id}/inscritos";

    @Test
    void con_la_clave_correcta_devuelve_los_inscritos_del_curso_en_orden() throws Exception {
        String ana = correoUnico();
        String beto = correoUnico();
        String otroCurso = correoUnico();
        inscribir(crearEstudiante(ana), 10);
        inscribir(crearEstudiante(beto), 10);
        inscribir(crearEstudiante(otroCurso), 11);

        mvc.perform(get(RUTA, 10).header("X-Internal-Key", CLAVE_INTERNA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].email").value(ana))
                .andExpect(jsonPath("$[0].fecha").exists())
                .andExpect(jsonPath("$[1].email").value(beto));
    }

    @Test
    void un_curso_sin_inscritos_devuelve_lista_vacia() throws Exception {
        mvc.perform(get(RUTA, 99999).header("X-Internal-Key", CLAVE_INTERNA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void sin_clave_o_con_clave_equivocada_da_403() throws Exception {
        mvc.perform(get(RUTA, 10)).andExpect(status().isForbidden());
        mvc.perform(get(RUTA, 10).header("X-Internal-Key", "otra")).andExpect(status().isForbidden());
    }

    @Test
    void un_token_de_estudiante_no_reemplaza_la_clave() throws Exception {
        String email = correoUnico();
        crearEstudiante(email);

        mvc.perform(get(RUTA, 10).with(tokenDe(email, "estudiante"))).andExpect(status().isForbidden());
    }
}
