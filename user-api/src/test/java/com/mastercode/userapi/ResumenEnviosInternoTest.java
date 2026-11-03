package com.mastercode.userapi;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

// HU44-T1 y D10: admin-api pide por la API interna cuántos envíos tiene cada problema de un curso, con X-Internal-Key.
// Los ids de problema van aparte de cualquier base de desarrollo: se siembran con ids altos y propios de esta clase.
class ResumenEnviosInternoTest extends PruebaDeApi {

    private static final String RUTA = "/internal/envios/resumen";
    private static final long P1 = 880_001;
    private static final long P2 = 880_002;
    private static final long P3 = 880_003;

    private ResultActions pedir(String problemas) throws Exception {
        return mvc.perform(get(RUTA).param("problemas", problemas).header("X-Internal-Key", CLAVE_INTERNA));
    }

    @Test
    void cuenta_los_intentos_y_los_correctos_de_cada_problema() throws Exception {
        long ana = crearEstudiante(correoUnico());
        long beto = crearEstudiante(correoUnico());
        registrarEnvio(ana, P1, "web", "incorrecto");
        registrarEnvio(ana, P1, "web", "correcto");
        registrarEnvio(beto, P1, "movil", "parcial");
        registrarEnvio(beto, P2, "web", "correcto");

        pedir(P1 + "," + P2)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].problema_id").value(P1))
                .andExpect(jsonPath("$[0].intentos").value(3))
                .andExpect(jsonPath("$[0].correctos").value(1))
                .andExpect(jsonPath("$[1].problema_id").value(P2))
                .andExpect(jsonPath("$[1].intentos").value(1))
                .andExpect(jsonPath("$[1].correctos").value(1));
    }

    @Test
    void un_envio_sin_calificar_cuenta_como_intento_y_no_como_correcto() throws Exception {
        registrarEnvio(crearEstudiante(correoUnico()), P1, "web", null);

        pedir(String.valueOf(P1))
                .andExpect(jsonPath("$[0].intentos").value(1))
                .andExpect(jsonPath("$[0].correctos").value(0));
    }

    @Test
    void solo_cuenta_los_problemas_pedidos() throws Exception {
        long ana = crearEstudiante(correoUnico());
        registrarEnvio(ana, P1, "web", "correcto");
        registrarEnvio(ana, P3, "web", "correcto");

        pedir(String.valueOf(P1))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].problema_id").value(P1));
    }

    @Test
    void un_problema_sin_envios_no_aparece() throws Exception {
        pedir(String.valueOf(P3)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void sin_problemas_responde_lista_vacia() throws Exception {
        mvc.perform(get(RUTA).header("X-Internal-Key", CLAVE_INTERNA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        pedir("").andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void un_id_que_no_es_numerico_da_400() throws Exception {
        pedir("1,abc").andExpect(status().isBadRequest());
    }

    @Test
    void mas_de_200_problemas_da_400() throws Exception {
        StringBuilder ids = new StringBuilder("1");
        for (int i = 2; i <= 201; i++) {
            ids.append(',').append(i);
        }

        pedir(ids.toString()).andExpect(status().isBadRequest());
    }

    // Solo cifras: ni correo de estudiantes ni código ni nada más que el problema y sus dos cuentas.
    @Test
    void la_respuesta_trae_solo_el_problema_y_sus_dos_cuentas() throws Exception {
        String correo = correoUnico();
        registrarEnvio(crearEstudiante(correo), P1, "web", "correcto");

        pedir(String.valueOf(P1))
                .andExpect(jsonPath("$[0].*", hasSize(3)))
                .andExpect(content().string(Matchers.not(Matchers.containsString(correo))));
    }

    @Test
    void sin_clave_o_con_clave_equivocada_da_403() throws Exception {
        mvc.perform(get(RUTA).param("problemas", String.valueOf(P1))).andExpect(status().isForbidden());
        mvc.perform(get(RUTA).param("problemas", String.valueOf(P1)).header("X-Internal-Key", "otra"))
                .andExpect(status().isForbidden());
    }

    @Test
    void un_token_de_estudiante_no_reemplaza_la_clave() throws Exception {
        String email = correoUnico();
        crearEstudiante(email);

        mvc.perform(get(RUTA).param("problemas", String.valueOf(P1)).with(tokenDe(email, "estudiante")))
                .andExpect(status().isForbidden());
    }
}
