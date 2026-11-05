package com.mastercode.userapi;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.mastercode.userapi.admin.ProblemaDto;

// HU41-T4: un estudiante solo ve sus propios envíos. Lo que ya cubren HistorialEnviosApiTest y DetalleEnvioApiTest
// (A y B en el mismo problema, detalle ajeno = 404 igual al inexistente, 401 sin token) no se repite aquí.
class EnviosAislamientoTest extends PruebaDeApi {

    private static final long CURSO = 10;
    private static final long PROBLEMA = 7;
    // Marca inventada: solo aparecería en una respuesta de A si viajara un envío de B.
    private static final String CODIGO_DE_B = "CODIGO-DE-B-123";

    private record Alumno(String email, long id) {
    }

    private Alumno alumnoInscrito() {
        String email = correoUnico();
        long id = crearEstudiante(email);
        inscribir(id, CURSO);
        return new Alumno(email, id);
    }

    private long sembrar(long estudianteId, String codigo, String resultado, int intento) {
        return jdbc.sql("""
                INSERT INTO app_schema.app_envio
                    (estudiante_id, problema_id, plataforma, codigo, resultado, puntaje, numero_intento)
                VALUES (?, ?, 'web', ?, ?, 0, ?)
                RETURNING id
                """)
                .params(estudianteId, PROBLEMA, codigo, resultado, intento)
                .query(Long.class)
                .single();
    }

    // CA-41.5: el estudiante sale del token. Ningún id de otro que llegue por parámetro, cuerpo o cabecera cambia
    // de quién son los envíos que se devuelven.
    @Test
    void inyectar_otro_estudiante_en_el_historial_no_cambia_el_resultado() throws Exception {
        Alumno a = alumnoInscrito();
        Alumno b = alumnoInscrito();
        when(admin.problema(PROBLEMA)).thenReturn(new ProblemaDto(PROBLEMA, "Suma", "Suma dos enteros.", "python",
                "facil", "web", "escritura_codigo", "", List.of(), CURSO));
        sembrar(a.id(), "print(1)", "correcto", 1);
        sembrar(b.id(), CODIGO_DE_B, "incorrecto", 1);
        sembrar(b.id(), CODIGO_DE_B, "parcial", 2);

        String ruta = "/api/problemas/" + PROBLEMA + "/envios";
        String otro = String.valueOf(b.id());
        List<MockHttpServletRequestBuilder> intentos = List.of(
                get(ruta).param("estudiante_id", otro),
                get(ruta).param("estudianteId", otro),
                get(ruta).contentType(MediaType.APPLICATION_JSON).content("{\"estudiante_id\": " + otro + "}"),
                get(ruta).header("X-Estudiante-Id", otro));

        for (MockHttpServletRequestBuilder intento : intentos) {
            mvc.perform(intento.with(tokenDe(a.email(), "estudiante")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].resultado").value("correcto"))
                    .andExpect(content().string(not(containsString(CODIGO_DE_B))));
        }
    }

    // Comportamiento actual (HU42-T2): el detalle no consulta inscripciones, así que el estudiante sigue abriendo su
    // propio envío aunque ya no esté inscrito en el curso. Si eso cambia, esta prueba debe cambiar con la decisión.
    @Test
    void sin_inscripcion_el_estudiante_sigue_abriendo_su_propio_envio() throws Exception {
        Alumno a = alumnoInscrito();
        long envio = sembrar(a.id(), "print(2)", "correcto", 1);
        jdbc.sql("DELETE FROM app_schema.app_inscripcion WHERE estudiante_id = ?").param(a.id()).update();

        mvc.perform(get("/api/envios/" + envio).with(tokenDe(a.email(), "estudiante")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("print(2)"));
    }
}
