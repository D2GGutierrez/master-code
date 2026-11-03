package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Types;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

// HU42-T2: GET /api/envios/{id}, el detalle de un envío del propio estudiante. Los envíos se siembran directo en la
// base; admin-api es un mock al que esta ruta no debe llamar.
class DetalleEnvioApiTest extends PruebaDeApi {

    private static final long PROBLEMA = 7;

    private record Alumno(String email, long id) {
    }

    private Alumno alumno() {
        String email = correoUnico();
        return new Alumno(email, crearEstudiante(email));
    }

    // Devuelve el id del envío sembrado. codigo, resultado y puntaje null = envío sin ese dato.
    private long sembrar(long estudianteId, String codigo, String resultado, Double puntaje) {
        return jdbc.sql("""
                INSERT INTO app_schema.app_envio
                    (estudiante_id, problema_id, plataforma, fecha_envio, codigo, resultado, puntaje, numero_intento)
                VALUES (:estudiante, :problema, 'web', :fecha, :codigo, :resultado, :puntaje, 1)
                RETURNING id
                """)
                .param("estudiante", estudianteId)
                .param("problema", PROBLEMA)
                .param("fecha", OffsetDateTime.parse("2026-11-03T09:00:00Z"))
                .param("codigo", codigo, Types.VARCHAR)
                .param("resultado", resultado, Types.VARCHAR)
                .param("puntaje", puntaje, Types.NUMERIC)
                .query(Long.class)
                .single();
    }

    private ResultActions detalle(String email, long envioId) throws Exception {
        return mvc.perform(get("/api/envios/" + envioId).with(tokenDe(email, "estudiante")));
    }

    @Test
    void devuelve_el_envio_propio_con_el_codigo_exacto() throws Exception {
        Alumno yo = alumno();
        // Sangría, líneas en blanco, tildes y salto final: el código vuelve tal como se mandó.
        String codigo = "def suma(a, b):\n    # añade\n\n    return a + b\n";
        long id = sembrar(yo.id(), codigo, "parcial", 50.0);

        detalle(yo.email(), id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.problema_id").value(PROBLEMA))
                .andExpect(jsonPath("$.numero_intento").value(1))
                .andExpect(jsonPath("$.fecha_envio").exists())
                .andExpect(jsonPath("$.resultado").value("parcial"))
                .andExpect(jsonPath("$.puntaje").value(50.0))
                .andExpect(jsonPath("$.codigo").value(codigo));
    }

    @Test
    void un_envio_sin_calificar_trae_resultado_y_puntaje_nulos() throws Exception {
        Alumno yo = alumno();
        long id = sembrar(yo.id(), "print(1)", null, null);

        detalle(yo.email(), id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value(nullValue()))
                .andExpect(jsonPath("$.puntaje").value(nullValue()))
                .andExpect(jsonPath("$.codigo").value("print(1)"));
    }

    @Test
    void un_envio_sin_codigo_guardado_lo_devuelve_como_texto_vacio() throws Exception {
        Alumno yo = alumno();
        long id = sembrar(yo.id(), null, null, null);

        detalle(yo.email(), id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(""));
    }

    // CA-41.5 / CA-42: el envío de otro estudiante no se puede abrir, y no se distingue de uno que no existe.
    @Test
    void el_envio_de_otro_estudiante_da_404_sin_revelar_su_codigo() throws Exception {
        Alumno yo = alumno();
        Alumno otro = alumno();
        long ajeno = sembrar(otro.id(), "CODIGO-AJENO-8841", "correcto", 100.0);

        String respuestaAjeno = detalle(yo.email(), ajeno)
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("CODIGO-AJENO-8841"))))
                .andReturn().getResponse().getContentAsString();
        String respuestaInexistente = detalle(yo.email(), ajeno + 100_000)
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        assertThat(respuestaAjeno).isEqualTo(respuestaInexistente);
        // El dueño sí lo abre.
        detalle(otro.email(), ajeno).andExpect(status().isOk()).andExpect(jsonPath("$.codigo").value("CODIGO-AJENO-8841"));
    }

    @Test
    void un_id_que_no_es_numerico_da_400() throws Exception {
        mvc.perform(get("/api/envios/abc").with(tokenDe(correoUnico(), "estudiante")))
                .andExpect(status().isBadRequest());
    }

    // Es un dato del estudiante: no depende de que el problema siga visible, así que no consulta a admin-api.
    @Test
    void no_consulta_a_admin_api() throws Exception {
        Alumno yo = alumno();
        long id = sembrar(yo.id(), "print(1)", "correcto", 100.0);

        detalle(yo.email(), id).andExpect(status().isOk());

        verifyNoInteractions(admin);
    }

    @Test
    void sin_token_da_401_y_con_token_de_docente_403() throws Exception {
        mvc.perform(get("/api/envios/1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/envios/1").with(tokenDe(correoUnico(), "docente"))).andExpect(status().isForbidden());
    }
}
