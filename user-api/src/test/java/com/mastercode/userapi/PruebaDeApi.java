package com.mastercode.userapi;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.mastercode.userapi.admin.AdminCliente;
import com.mastercode.userapi.perfil.EstudianteRepository;
import com.mastercode.userapi.sandbox.SandboxCliente;

// Base de las pruebas de la API de inscripciones (HU26, HU27, HU28). Todas comparten la misma
// configuración para que Spring arranque una sola vez. AdminCliente se reemplaza por un mock:
// ninguna prueba necesita admin-api ni ai-service encendidos.
// @Transactional: cada prueba deshace lo que insertó, también lo que crea el filtro del perfil.
@SpringBootTest(properties = "internal.api-key=" + PruebaDeApi.CLAVE_INTERNA)
@AutoConfigureMockMvc
@Transactional
abstract class PruebaDeApi {

    static final String CLAVE_INTERNA = "clave-interna-solo-para-pruebas";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected EstudianteRepository estudiantes;

    @Autowired
    protected JdbcClient jdbc;

    @MockBean
    protected AdminCliente admin;

    // Tampoco hace falta ai-service ni ningún motor para probar «Ejecutar».
    @MockBean
    protected SandboxCliente sandbox;

    // Correo único por prueba: no choca con datos de la base de desarrollo ni con otras corridas.
    protected static String correoUnico() {
        return "prueba-" + UUID.randomUUID() + "@ejemplo.edu.pe";
    }

    protected long crearEstudiante(String email) {
        estudiantes.insertarSiNoExiste(email, "ejemplo.edu.pe");
        return estudiantes.findByEmail(email).orElseThrow().getId();
    }

    protected void inscribir(long estudianteId, long cursoId) {
        jdbc.sql("INSERT INTO app_schema.app_inscripcion (estudiante_id, curso_id) VALUES (?, ?)")
                .params(estudianteId, cursoId)
                .update();
    }

    protected int inscripcionesDe(long estudianteId) {
        return jdbc.sql("SELECT count(*) FROM app_schema.app_inscripcion WHERE estudiante_id = ?")
                .param(estudianteId)
                .query(Integer.class)
                .single();
    }

    // jwt() arma el token en la prueba, sin admin-api: mismos claims que lee AsegurarPerfilFiltro.
    protected static RequestPostProcessor tokenDe(String email, String rol) {
        return jwt()
                .jwt(token -> token.claim("email", email).claim("institucion", "ejemplo.edu.pe").claim("rol", rol))
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol.toUpperCase()));
    }
}
