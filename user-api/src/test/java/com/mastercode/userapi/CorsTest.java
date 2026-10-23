package com.mastercode.userapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

// D4: user-web (3001) llama a user-api (8081) desde el navegador, que antes envía un OPTIONS sin token.
@SpringBootTest
@AutoConfigureMockMvc
class CorsTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void el_preflight_desde_user_web_recibe_las_cabeceras_cors() throws Exception {
        mvc.perform(options("/api/yo")
                        .header("Origin", "http://localhost:3001")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3001"));
    }

    @Test
    void el_preflight_desde_un_origen_no_listado_se_rechaza() throws Exception {
        mvc.perform(options("/api/yo")
                        .header("Origin", "http://sitio-ajeno.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    // HU06-T4: sin esto, el navegador no deja que user-web lea el Retry-After del 429 de «Ejecutar».
    @Test
    void el_navegador_puede_leer_retry_after_en_las_respuestas() throws Exception {
        mvc.perform(get("/health").header("Origin", "http://localhost:3001"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Expose-Headers", "Retry-After"));
    }

    @Test
    void health_sigue_publico_sin_token() throws Exception {
        mvc.perform(get("/health")).andExpect(status().isOk());
    }
}
