package com.mastercode.userapi;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.mastercode.userapi.perfil.EstudianteRepository;

// HU24: el avance es de la cuenta, no del dispositivo. Se mide lo enviado o intentado; qué problemas
// están resueltos lo calcula el listado de problemas (HU30-T2, EstadoProblemasRepository).
// @Transactional: cada prueba deshace al final lo que creó, también lo que crea el filtro del perfil.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AvanceMultiplataformaTest {

    private static final String WEB = "web";
    private static final String MOVIL = "movil";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EstudianteRepository estudiantes;

    @Autowired
    private JdbcClient jdbc;

    // Correo único por prueba: no choca con datos de la base de desarrollo ni con otras corridas.
    private static String correoUnico() {
        return "prueba-" + UUID.randomUUID() + "@ejemplo.edu.pe";
    }

    private long crearEstudiante(String email) {
        estudiantes.insertarSiNoExiste(email, "ejemplo.edu.pe");
        return estudiantes.findByEmail(email).orElseThrow().getId();
    }

    private void enviar(long estudianteId, long problemaId, String plataforma) {
        jdbc.sql("INSERT INTO app_schema.app_envio (estudiante_id, problema_id, plataforma) VALUES (?, ?, ?)")
                .params(estudianteId, problemaId, plataforma)
                .update();
    }

    // jwt() arma el token en la prueba, sin admin-api: mismos claims que lee AsegurarPerfilFiltro.
    private static RequestPostProcessor tokenDe(String email, String rol) {
        return jwt()
                .jwt(token -> token.claim("email", email).claim("institucion", "ejemplo.edu.pe").claim("rol", rol))
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol.toUpperCase()));
    }

    @Test
    void lo_enviado_desde_movil_y_web_se_suma_en_la_misma_cuenta() throws Exception {
        String email = correoUnico();
        long id = crearEstudiante(email);
        enviar(id, 101, MOVIL);
        enviar(id, 101, WEB);
        enviar(id, 102, MOVIL);

        mvc.perform(get("/api/avance").with(tokenDe(email, "estudiante")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.problemas_intentados").value(2))
                .andExpect(jsonPath("$.total_envios").value(3))
                .andExpect(jsonPath("$.desde_web").value(1))
                .andExpect(jsonPath("$.desde_movil").value(2))
                .andExpect(jsonPath("$.problemas", hasSize(2)))
                .andExpect(jsonPath("$.problemas[0].problema_id").value(101))
                .andExpect(jsonPath("$.problemas[0].envios").value(2))
                .andExpect(jsonPath("$.problemas[0].desde_web").value(1))
                .andExpect(jsonPath("$.problemas[0].desde_movil").value(1))
                .andExpect(jsonPath("$.problemas[1].problema_id").value(102))
                .andExpect(jsonPath("$.problemas[1].envios").value(1))
                .andExpect(jsonPath("$.problemas[1].desde_web").value(0))
                .andExpect(jsonPath("$.problemas[1].desde_movil").value(1));
    }

    @Test
    void lo_enviado_solo_desde_movil_aparece_en_el_avance() throws Exception {
        String email = correoUnico();
        enviar(crearEstudiante(email), 201, MOVIL);

        mvc.perform(get("/api/avance").with(tokenDe(email, "estudiante")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.problemas_intentados").value(1))
                .andExpect(jsonPath("$.desde_movil").value(1))
                .andExpect(jsonPath("$.desde_web").value(0))
                .andExpect(jsonPath("$.problemas[0].problema_id").value(201));
    }

    @Test
    void los_envios_de_otra_cuenta_no_aparecen_en_el_avance() throws Exception {
        String emailA = correoUnico();
        String emailB = correoUnico();
        enviar(crearEstudiante(emailA), 301, WEB);
        long idB = crearEstudiante(emailB);
        enviar(idB, 302, MOVIL);
        enviar(idB, 303, MOVIL);

        // El estudiante sale del email del token: A solo ve su problema 301.
        mvc.perform(get("/api/avance").with(tokenDe(emailA, "estudiante")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_envios").value(1))
                .andExpect(jsonPath("$.desde_movil").value(0))
                .andExpect(jsonPath("$.problemas", hasSize(1)))
                .andExpect(jsonPath("$.problemas[0].problema_id").value(301));
    }

    @Test
    void sin_token_responde_401() throws Exception {
        mvc.perform(get("/api/avance")).andExpect(status().isUnauthorized());
    }

    @Test
    void un_docente_recibe_403() throws Exception {
        mvc.perform(get("/api/avance").with(tokenDe(correoUnico(), "docente")))
                .andExpect(status().isForbidden());
    }
}
