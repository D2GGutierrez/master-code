package com.mastercode.userapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

// HU44-T4 (CA-44.4): sin la clave interna, el resumen de envíos no entrega nada. ResumenEnviosInternoTest ya comprueba
// el 403 y que solo se cuentan los problemas pedidos; aquí, que el 403 no lleva ninguna cifra aunque haya envíos.
class ResumenEnviosAislamientoTest extends PruebaDeApi {

    private static final String RUTA = "/internal/envios/resumen";
    // Id alto y propio de esta clase: no choca con la base de desarrollo.
    private static final long PROBLEMA = 880_101;

    @Test
    void sin_clave_o_con_clave_equivocada_da_403_con_el_cuerpo_vacio() throws Exception {
        registrarEnvio(crearEstudiante(correoUnico()), PROBLEMA, "web", "correcto");

        mvc.perform(get(RUTA).param("problemas", String.valueOf(PROBLEMA)))
                .andExpect(status().isForbidden())
                .andExpect(content().string(""));
        mvc.perform(get(RUTA).param("problemas", String.valueOf(PROBLEMA)).header("X-Internal-Key", "clave-inventada"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(""));
    }
}
