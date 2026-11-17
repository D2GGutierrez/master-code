package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

// HU46-T4 (CA-46.4): la alternativa correcta no viaja al celular antes de que el estudiante responda. Se mira lo que sale
// por las dos rutas que entregan un ejercicio (el detalle y el Dojo): cada alternativa trae solo id y texto, ninguna
// marca, y user-api ni siquiera le pide a admin-api cuál es la correcta hasta que el estudiante responde.
class AlternativaCorrectaNoViajaTest extends EjerciciosMovilesBase {

    private static final ObjectMapper JSON = new ObjectMapper();

    // Cada alternativa tiene exactamente id y texto: sin es_correcta, correcta ni nada parecido.
    private static void assertSoloIdYTexto(String cuerpo) throws Exception {
        JsonNode opciones = JSON.readTree(cuerpo).get("opciones");
        assertThat(opciones).isNotNull();
        assertThat(opciones).hasSize(3);
        for (JsonNode opcion : opciones) {
            assertThat(opcion.fieldNames()).toIterable().containsExactlyInAnyOrder("id", "texto");
        }
        assertThat(cuerpo).doesNotContain("correcta").doesNotContain("respuesta_esperada");
    }

    @Test
    void el_detalle_del_ejercicio_no_trae_la_marca_de_la_correcta_ni_la_pide_a_admin() throws Exception {
        String yo = alumno();
        publicar(multiple(8, "facil", "Listas"));

        String cuerpo = mvc.perform(get("/api/problemas/8").with(tokenDeCliente(yo, "mobile")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertSoloIdYTexto(cuerpo);
        verify(admin, never()).respuestaDelProblema(8);
    }

    @Test
    void el_ejercicio_que_entrega_el_dojo_tampoco_la_trae() throws Exception {
        String yo = alumno();
        publicar(multiple(8, "facil", "Listas"));

        String cuerpo = pedirSiguiente(yo, sesion(yo)).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();

        assertSoloIdYTexto(cuerpo);
        verify(admin, never()).respuestaDelProblema(8);
    }

    @Test
    void el_listado_de_ejercicios_del_curso_no_trae_alternativas() throws Exception {
        String yo = alumno();
        publicar(multiple(8, "facil", "Listas"));

        String cuerpo = mvc.perform(get("/api/cursos/10/problemas").with(tokenDeCliente(yo, "mobile")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(cuerpo).doesNotContain("opciones").doesNotContain("correcta").doesNotContain("alternativa");
    }

    @Test
    void la_correcta_solo_se_conoce_despues_de_responder_y_solo_si_fallo() throws Exception {
        String yo = alumno();
        publicar(multiple(8, "facil", "Listas"));

        // Falla: ahora sí se le dice cuál era (HU48-T2).
        enviar(yo, 8, null, 801L, null, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("incorrecto"))
                .andExpect(jsonPath("$.respuesta_correcta").value("La alternativa buena"));
        // Acierta: no hay nada que revelar.
        enviar(yo, 8, null, correctaDe(multiple(8, "facil", "Listas")), null, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("correcto"))
                .andExpect(jsonPath("$.respuesta_correcta").doesNotExist());
    }

    @Test
    void las_alternativas_cambian_de_orden_entre_una_apertura_y_otra() throws Exception {
        String yo = alumno();
        publicar(multiple(8, "facil", "Listas"));
        java.util.Set<String> ordenes = new java.util.HashSet<>();

        for (int i = 0; i < 40; i++) {
            String cuerpo = mvc.perform(get("/api/problemas/8").with(tokenDeCliente(yo, "mobile")))
                    .andReturn().getResponse().getContentAsString();
            ordenes.add(JSON.readTree(cuerpo).get("opciones").findValuesAsText("id").toString());
        }

        // 6 órdenes posibles: con 40 aperturas, que salga uno solo es prácticamente imposible si se mezclan.
        assertThat(ordenes.size()).isGreaterThan(1);
    }
}
