package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import com.mastercode.userapi.cinturon.CinturonRepository;

// HU51-T4 (CA-51.1, CA-51.4): los puntos que faltan para el siguiente cinturón, en cada nivel y justo a los lados de cada
// umbral (docs/puntos-cinturon.md). Se pasa por GET /api/yo/cinturones para probar también lo que ve la app. La función
// pura la prueba AvanceDeCinturonTest; aquí se comprueba con la tabla real de la base.
class CinturonesPorNivelApiTest extends EjerciciosMovilesBase {

    // Umbrales de la tabla de niveles: nombre del cinturón al llegar a ellos.
    private static final int[] UMBRALES = { 0, 100, 250, 500, 900, 1400, 2000 };
    private static final String[] NOMBRES = { "blanco", "amarillo", "naranja", "verde", "azul", "marron", "negro" };

    @Autowired
    private CinturonRepository cinturones;

    private ResultActions consultar(String email) throws Exception {
        return mvc.perform(get("/api/yo/cinturones").with(tokenDeCliente(email, "mobile")));
    }

    // Un estudiante con exactamente «puntos» en python (con 0, no tiene cinturón todavía).
    private String conPuntos(int puntos) {
        String email = correoUnico();
        long yo = crearEstudiante(email);
        if (puntos > 0) {
            cinturones.sumar(yo, "python", puntos);
        }
        return email;
    }

    @Test
    void justo_en_cada_umbral_se_tiene_el_cinturon_nuevo_y_falta_lo_del_siguiente() throws Exception {
        // El blanco (0 puntos) no existe como fila hasta el primer acierto: se prueba aparte.
        for (int i = 1; i < UMBRALES.length; i++) {
            String email = conPuntos(UMBRALES[i]);

            ResultActions r = consultar(email).andExpect(status().isOk())
                    .andExpect(jsonPath("$.cinturones[0].nivel").value(i + 1))
                    .andExpect(jsonPath("$.cinturones[0].nombre").value(NOMBRES[i]))
                    .andExpect(jsonPath("$.cinturones[0].puntos").value(UMBRALES[i]));
            if (i < UMBRALES.length - 1) {
                r.andExpect(jsonPath("$.cinturones[0].siguiente.nombre").value(NOMBRES[i + 1]))
                        .andExpect(jsonPath("$.cinturones[0].siguiente.puntos_requeridos").value(UMBRALES[i + 1]))
                        .andExpect(jsonPath("$.cinturones[0].puntos_faltantes").value(UMBRALES[i + 1] - UMBRALES[i]));
            }
        }
    }

    @Test
    void un_punto_antes_de_cada_umbral_todavia_es_el_cinturon_anterior_y_falta_uno() throws Exception {
        for (int i = 1; i < UMBRALES.length; i++) {
            String email = conPuntos(UMBRALES[i] - 1);

            consultar(email).andExpect(status().isOk())
                    .andExpect(jsonPath("$.cinturones[0].nivel").value(i))
                    .andExpect(jsonPath("$.cinturones[0].nombre").value(NOMBRES[i - 1]))
                    .andExpect(jsonPath("$.cinturones[0].siguiente.nombre").value(NOMBRES[i]))
                    .andExpect(jsonPath("$.cinturones[0].puntos_faltantes").value(1));
        }
    }

    @Test
    void con_un_primer_acierto_es_blanco_y_faltan_los_puntos_del_amarillo() throws Exception {
        String email = conPuntos(10);

        consultar(email).andExpect(status().isOk())
                .andExpect(jsonPath("$.cinturones[0].nombre").value("blanco"))
                .andExpect(jsonPath("$.cinturones[0].puntos_faltantes").value(90));
    }

    @Test
    void en_medio_de_cada_tramo_faltan_los_puntos_hasta_el_siguiente_umbral() throws Exception {
        for (int i = 0; i < UMBRALES.length - 1; i++) {
            int puntos = UMBRALES[i] + 30;
            String email = conPuntos(puntos);

            consultar(email).andExpect(status().isOk())
                    .andExpect(jsonPath("$.cinturones[0].nombre").value(NOMBRES[i]))
                    .andExpect(jsonPath("$.cinturones[0].puntos_faltantes").value(UMBRALES[i + 1] - puntos));
        }
    }

    @Test
    void el_negro_es_el_ultimo_y_no_tiene_siguiente_ni_puntos_faltantes() throws Exception {
        String email = conPuntos(2000);

        consultar(email).andExpect(status().isOk())
                .andExpect(jsonPath("$.cinturones[0].nombre").value("negro"))
                .andExpect(jsonPath("$.cinturones[0].siguiente").value((Object) null))
                .andExpect(jsonPath("$.cinturones[0].puntos_faltantes").value((Object) null));
    }

    @Test
    void pasarse_del_umbral_del_negro_sigue_siendo_negro_sin_faltantes() throws Exception {
        String email = conPuntos(2500);

        consultar(email).andExpect(status().isOk())
                .andExpect(jsonPath("$.cinturones[0].nombre").value("negro"))
                .andExpect(jsonPath("$.cinturones[0].puntos").value(2500))
                .andExpect(jsonPath("$.cinturones[0].puntos_faltantes").value((Object) null));
    }

    @Test
    void los_puntos_faltantes_nunca_son_negativos_ni_cero_antes_del_negro() throws Exception {
        for (int puntos = 10; puntos < 2000; puntos += 90) {
            String email = conPuntos(puntos);

            Integer faltan = com.jayway.jsonpath.JsonPath.read(
                    consultar(email).andReturn().getResponse().getContentAsString(), "$.cinturones[0].puntos_faltantes");
            assertThat(faltan).as("faltantes con %s puntos", puntos).isPositive();
        }
    }

    // Los umbrales de este test son los de la tabla de la base: si alguien cambia un valor, falla aquí y en
    // PuntosCinturonDocTest, no en silencio.
    @Test
    void los_umbrales_de_la_prueba_son_los_de_la_tabla_de_niveles() {
        var niveles = cinturones.niveles();

        assertThat(niveles).hasSize(UMBRALES.length);
        for (int i = 0; i < UMBRALES.length; i++) {
            assertThat(niveles.get(i).nombre()).isEqualTo(NOMBRES[i]);
            assertThat(niveles.get(i).puntosRequeridos()).isEqualTo(UMBRALES[i]);
        }
    }
}
