package com.mastercode.userapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

// HU56-T4 (CA-56.4 y CA-56.5 del resumen): una sesión perfecta y una sin aciertos, jugadas de punta a punta contra la API.
// Complementa a DojoDificultadApiTest, que prueba el resumen con una sesión mezclada. La respuesta correcta es la que
// publica cada ejercicio; cualquier otra falla.
class DojoResumenApiTest extends EjerciciosMovilesBase {

    private ResultActions terminar(String email, long sesion) throws Exception {
        return mvc.perform(post("/api/dojo/sesiones/" + sesion + "/terminar").with(tokenDeCliente(email, "mobile")));
    }

    // Juega la sesión entera: pide un ejercicio tras otro y responde bien o mal según «acertar». Devuelve cuántos jugó.
    private int jugar(String email, long sesion, boolean acertar) throws Exception {
        int jugados = 0;
        while (true) {
            ResultActions siguiente = pedirSiguiente(email, sesion);
            String texto = siguiente.andReturn().getResponse().getContentAsString();
            if (siguiente.andReturn().getResponse().getStatus() == 204) {
                return jugados;
            }
            long problema = Long.parseLong(texto.replaceAll(".*?\"id\":(\\d+).*", "$1"));
            enviar(email, problema, acertar ? "ok" : "equivocada", null, sesion, null).andExpect(status().isOk());
            jugados++;
        }
    }

    private void publicarVarios(int cuantos, String tema) {
        List<Ejercicio> ejercicios = new ArrayList<>();
        for (int i = 1; i <= cuantos; i++) {
            ejercicios.add(prediccion(i, "facil", tema, "ok"));
        }
        publicar(ejercicios.toArray(new Ejercicio[0]));
    }

    // Una sesión perfecta: todo acertado. Sin temas para repasar, todos los puntos y el cinturón con ellos.
    @Test
    void una_sesion_perfecta_trae_todos_los_aciertos_sin_temas_a_repasar_y_con_los_puntos_en_el_cinturon() throws Exception {
        publicarVarios(5, "Bucles");
        String yo = alumno();
        long sesion = sesion(yo);

        int jugados = jugar(yo, sesion, true);

        org.assertj.core.api.Assertions.assertThat(jugados).isEqualTo(5);
        terminar(yo, sesion).andExpect(status().isOk())
                .andExpect(jsonPath("$.respondidos").value(5))
                .andExpect(jsonPath("$.aciertos").value(5))
                .andExpect(jsonPath("$.puntos_sumados").value(50))
                .andExpect(jsonPath("$.temas_mas_fallados.length()").value(0))
                .andExpect(jsonPath("$.cinturon.nivel").value(1))
                .andExpect(jsonPath("$.cinturon.puntos").value(50));
    }

    @Test
    void una_sesion_perfecta_sube_el_nivel_de_dificultad_alcanzado() throws Exception {
        publicar(prediccion(1, "facil", "Bucles", "ok"), prediccion(2, "facil", "Bucles", "ok"),
                prediccion(3, "medio", "Bucles", "ok"), prediccion(4, "medio", "Bucles", "ok"),
                prediccion(5, "dificil", "Bucles", "ok"));
        String yo = alumno();
        long sesion = sesion(yo);

        jugar(yo, sesion, true);

        // Dos aciertos seguidos suben un nivel (docs/regla-dificultad.md): facil -> medio -> dificil.
        terminar(yo, sesion).andExpect(status().isOk())
                .andExpect(jsonPath("$.aciertos").value(5))
                .andExpect(jsonPath("$.nivel_alcanzado").value("dificil"));
    }

    // Una sesión sin aciertos: todo mal. Cero aciertos, cero puntos, sin cinturón, y los temas donde falló.
    @Test
    void una_sesion_sin_aciertos_trae_cero_puntos_sin_cinturon_y_los_temas_fallados() throws Exception {
        publicar(prediccion(1, "facil", "Bucles", "ok"), prediccion(2, "facil", "Bucles", "ok"),
                prediccion(3, "facil", "Listas", "ok"));
        String yo = alumno();
        long sesion = sesion(yo);

        int jugados = jugar(yo, sesion, false);

        org.assertj.core.api.Assertions.assertThat(jugados).isEqualTo(3);
        terminar(yo, sesion).andExpect(status().isOk())
                .andExpect(jsonPath("$.respondidos").value(3))
                .andExpect(jsonPath("$.aciertos").value(0))
                .andExpect(jsonPath("$.puntos_sumados").value(0))
                .andExpect(jsonPath("$.cinturon").value((Object) null))
                .andExpect(jsonPath("$.temas_mas_fallados.length()").value(2))
                .andExpect(jsonPath("$.temas_mas_fallados[0].tema").value("Bucles"))
                .andExpect(jsonPath("$.temas_mas_fallados[0].fallos").value(2))
                .andExpect(jsonPath("$.temas_mas_fallados[1].tema").value("Listas"))
                .andExpect(jsonPath("$.temas_mas_fallados[1].fallos").value(1));
    }

    @Test
    void una_sesion_sin_aciertos_deja_el_nivel_en_facil_aunque_siga_fallando() throws Exception {
        publicarVarios(4, "Bucles");
        String yo = alumno();
        long sesion = sesion(yo);

        jugar(yo, sesion, false);

        terminar(yo, sesion).andExpect(status().isOk()).andExpect(jsonPath("$.nivel_alcanzado").value("facil"));
    }

    @Test
    void una_sesion_sin_aciertos_no_crea_cinturon_en_la_base() throws Exception {
        publicarVarios(2, "Bucles");
        String yo = alumno();
        long sesion = sesion(yo);
        jugar(yo, sesion, false);
        terminar(yo, sesion).andExpect(status().isOk());

        org.assertj.core.api.Assertions.assertThat(jdbc.sql("SELECT count(*) FROM app_schema.app_cinturon WHERE estudiante_id = ?")
                .param(idDe(yo)).query(Integer.class).single()).isZero();
    }

    @Test
    void los_temas_del_resumen_son_como_maximo_tres() throws Exception {
        publicar(prediccion(1, "facil", "A", "ok"), prediccion(2, "facil", "B", "ok"), prediccion(3, "facil", "C", "ok"),
                prediccion(4, "facil", "D", "ok"));
        String yo = alumno();
        long sesion = sesion(yo);
        jugar(yo, sesion, false);

        terminar(yo, sesion).andExpect(status().isOk()).andExpect(jsonPath("$.temas_mas_fallados.length()").value(3));
    }
}
