package com.mastercode.userapi;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mastercode.userapi.admin.ProblemaDto;

// HU46-T2: GET /api/problemas/{id} entrega las alternativas de un ejercicio de opción múltiple mezcladas en cada
// apertura y sin indicar cuál es la correcta (CA-46.3 y CA-46.4).
class AlternativasMovilApiTest extends PruebaDeApi {

    private static final long CURSO = 10;
    private static final long PROBLEMA = 8;

    // Lo que admin-api manda por la ruta del estudiante, con un campo es_correcta de más a propósito: si admin-api se
    // equivocara y lo mandara, user-api no debe reenviarlo.
    private static final String JSON_DE_ADMIN = """
            {"id":8,"titulo":"Elige","enunciado":"¿Cuánto es 1 + 1?","lenguaje":"python","dificultad":"facil",
             "plataforma":"movil","tipo_ejercicio":"opcion_multiple","codigo_base":"","casos_prueba":[],"curso_id":10,
             "opciones":[{"id":81,"texto":"Uno","es_correcta":false},{"id":82,"texto":"Dos","es_correcta":true},
                         {"id":83,"texto":"Tres","es_correcta":false},{"id":84,"texto":"Cuatro","es_correcta":false}]}
            """;

    @Autowired
    private ObjectMapper json;

    private String alumnoInscrito() {
        String email = correoUnico();
        inscribir(crearEstudiante(email), CURSO);
        return email;
    }

    private String abrir(String email) throws Exception {
        return mvc.perform(get("/api/problemas/" + PROBLEMA).with(tokenDeCliente(email, "mobile")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void trae_todas_las_alternativas_con_id_y_texto_y_ninguna_marca_de_correcta() throws Exception {
        String yo = alumnoInscrito();
        when(admin.problema(PROBLEMA)).thenReturn(json.readValue(JSON_DE_ADMIN, ProblemaDto.class));

        mvc.perform(get("/api/problemas/" + PROBLEMA).with(tokenDeCliente(yo, "mobile")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.opciones", hasSize(4)))
                .andExpect(jsonPath("$.opciones[?(@.id==82)].texto").value("Dos"))
                .andExpect(content().string(not(containsString("es_correcta"))))
                .andExpect(content().string(not(containsString("correcta"))));
    }

    @Test
    void cada_alternativa_lleva_solo_id_y_texto() throws Exception {
        String yo = alumnoInscrito();
        when(admin.problema(PROBLEMA)).thenReturn(json.readValue(JSON_DE_ADMIN, ProblemaDto.class));

        mvc.perform(get("/api/problemas/" + PROBLEMA).with(tokenDeCliente(yo, "mobile")))
                .andExpect(jsonPath("$.opciones[0].id").exists())
                .andExpect(jsonPath("$.opciones[0].texto").exists())
                .andExpect(jsonPath("$.opciones[0].length()").value(2));
    }

    @Test
    void el_orden_cambia_de_una_apertura_a_otra() throws Exception {
        String yo = alumnoInscrito();
        when(admin.problema(PROBLEMA)).thenReturn(json.readValue(JSON_DE_ADMIN, ProblemaDto.class));

        // Con 4 alternativas hay 24 órdenes posibles: que 40 aperturas den siempre el mismo es prácticamente imposible
        // si se mezcla, y seguro si no se mezcla.
        Set<String> ordenes = new HashSet<>();
        for (int i = 0; i < 40; i++) {
            ordenes.add(json.readTree(abrir(yo)).get("opciones").findValuesAsText("id").toString());
        }

        org.assertj.core.api.Assertions.assertThat(ordenes.size()).isGreaterThan(1);
    }

    @Test
    void un_ejercicio_que_no_es_de_opcion_multiple_trae_la_lista_vacia() throws Exception {
        String yo = alumnoInscrito();
        when(admin.problema(PROBLEMA)).thenReturn(new ProblemaDto(PROBLEMA, "Suma", "Suma dos números.", "python",
                "facil", "web", "escritura_codigo", "", List.of(), CURSO));

        mvc.perform(get("/api/problemas/" + PROBLEMA).with(tokenDeCliente(yo, "mobile")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.opciones", hasSize(0)));
    }

    @Test
    void si_admin_api_no_manda_alternativas_trae_la_lista_vacia() throws Exception {
        String yo = alumnoInscrito();
        when(admin.problema(PROBLEMA)).thenReturn(json.readValue(
                "{\"id\":8,\"titulo\":\"x\",\"enunciado\":\"y\",\"lenguaje\":\"python\",\"dificultad\":\"facil\","
                        + "\"plataforma\":\"movil\",\"tipo_ejercicio\":\"opcion_multiple\",\"codigo_base\":\"\","
                        + "\"casos_prueba\":[],\"curso_id\":10}",
                ProblemaDto.class));

        mvc.perform(get("/api/problemas/" + PROBLEMA).with(tokenDeCliente(yo, "mobile")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.opciones", hasSize(0)));
    }
}
