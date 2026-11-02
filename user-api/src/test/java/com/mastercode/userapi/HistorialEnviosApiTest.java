package com.mastercode.userapi;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.mastercode.userapi.admin.AdminNoDisponibleException;
import com.mastercode.userapi.admin.ProblemaDto;
import com.mastercode.userapi.admin.ProblemaNoVisibleException;

// HU41-T1: GET /api/problemas/{id}/envios, el historial del estudiante en un problema. admin-api es un mock que dice
// qué problema es visible y de qué curso; los envíos se siembran directo en la base con fechas distintas.
class HistorialEnviosApiTest extends PruebaDeApi {

    private static final long CURSO = 10;
    private static final long PROBLEMA = 7;
    private static final long OTRO_PROBLEMA = 8;
    // Centinela: texto que solo aparecería en la respuesta si viajara el código del envío.
    private static final String CODIGO_SECRETO = "CODIGO-DEL-ENVIO-5521";

    private static ProblemaDto problema(long id, long curso) {
        return new ProblemaDto(id, "Suma de dos", "Lee dos enteros e imprime su suma.", "python", "facil", "web",
                "escritura_codigo", "", List.of(new ProblemaDto.Caso("2 3", "5")), curso);
    }

    private record Alumno(String email, long id) {
    }

    private Alumno alumnoInscrito() {
        String email = correoUnico();
        long id = crearEstudiante(email);
        inscribir(id, CURSO);
        return new Alumno(email, id);
    }

    // Sembrado directo: dentro de una transacción now() no cambia, así que cada envío lleva su fecha.
    // resultado y puntaje null = envío sin calificar; el tipo explícito evita que la base no sepa de qué tipo es el null.
    private void sembrar(long estudianteId, long problemaId, int intento, String fecha, String resultado,
            Double puntaje) {
        jdbc.sql("""
                INSERT INTO app_schema.app_envio
                    (estudiante_id, problema_id, plataforma, fecha_envio, codigo, resultado, puntaje, numero_intento)
                VALUES (:estudiante, :problema, 'web', :fecha, :codigo, :resultado, :puntaje, :intento)
                """)
                .param("estudiante", estudianteId)
                .param("problema", problemaId)
                .param("fecha", OffsetDateTime.parse(fecha))
                .param("codigo", CODIGO_SECRETO)
                .param("resultado", resultado, Types.VARCHAR)
                .param("puntaje", puntaje, Types.NUMERIC)
                .param("intento", intento)
                .update();
    }

    private ResultActions historial(String email, long problemaId) throws Exception {
        return mvc.perform(get("/api/problemas/" + problemaId + "/envios").with(tokenDe(email, "estudiante")));
    }

    @Test
    void devuelve_los_envios_del_mas_reciente_al_mas_antiguo_con_fecha_resultado_y_puntaje() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.problema(PROBLEMA)).thenReturn(problema(PROBLEMA, CURSO));
        // Se siembran desordenados a propósito: el orden lo pone la consulta, no el orden de inserción.
        sembrar(yo.id(), PROBLEMA, 2, "2026-11-03T10:00:00Z", "parcial", 50.0);
        sembrar(yo.id(), PROBLEMA, 3, "2026-11-03T11:00:00Z", "correcto", 100.0);
        sembrar(yo.id(), PROBLEMA, 1, "2026-11-03T09:00:00Z", "incorrecto", 0.0);

        historial(yo.email(), PROBLEMA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].numero_intento").value(3))
                .andExpect(jsonPath("$[0].resultado").value("correcto"))
                .andExpect(jsonPath("$[0].puntaje").value(100.0))
                .andExpect(jsonPath("$[0].fecha_envio").exists())
                .andExpect(jsonPath("$[1].numero_intento").value(2))
                .andExpect(jsonPath("$[1].resultado").value("parcial"))
                .andExpect(jsonPath("$[1].puntaje").value(50.0))
                .andExpect(jsonPath("$[2].numero_intento").value(1))
                .andExpect(jsonPath("$[2].resultado").value("incorrecto"));
    }

    @Test
    void con_la_misma_fecha_el_ultimo_guardado_va_primero() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.problema(PROBLEMA)).thenReturn(problema(PROBLEMA, CURSO));
        sembrar(yo.id(), PROBLEMA, 1, "2026-11-03T09:00:00Z", "incorrecto", 0.0);
        sembrar(yo.id(), PROBLEMA, 2, "2026-11-03T09:00:00Z", "correcto", 100.0);

        historial(yo.email(), PROBLEMA)
                .andExpect(jsonPath("$[0].numero_intento").value(2))
                .andExpect(jsonPath("$[1].numero_intento").value(1));
    }

    @Test
    void solo_trae_los_envios_de_ese_problema() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.problema(PROBLEMA)).thenReturn(problema(PROBLEMA, CURSO));
        sembrar(yo.id(), PROBLEMA, 1, "2026-11-03T09:00:00Z", "correcto", 100.0);
        sembrar(yo.id(), OTRO_PROBLEMA, 1, "2026-11-03T10:00:00Z", "incorrecto", 0.0);

        historial(yo.email(), PROBLEMA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].resultado").value("correcto"));
    }

    // CA-41.5: el estudiante sale del token y la ruta no lleva un id de estudiante, así que no hay cómo pedir los de otro.
    @Test
    void cada_estudiante_ve_solo_sus_propios_envios() throws Exception {
        Alumno yo = alumnoInscrito();
        Alumno otro = alumnoInscrito();
        when(admin.problema(PROBLEMA)).thenReturn(problema(PROBLEMA, CURSO));
        sembrar(yo.id(), PROBLEMA, 1, "2026-11-03T09:00:00Z", "correcto", 100.0);
        sembrar(otro.id(), PROBLEMA, 1, "2026-11-03T09:30:00Z", "incorrecto", 0.0);
        sembrar(otro.id(), PROBLEMA, 2, "2026-11-03T09:45:00Z", "parcial", 50.0);

        historial(yo.email(), PROBLEMA)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].resultado").value("correcto"));
        historial(otro.email(), PROBLEMA)
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].resultado").value("parcial"));
    }

    @Test
    void sin_envios_responde_lista_vacia() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.problema(PROBLEMA)).thenReturn(problema(PROBLEMA, CURSO));

        historial(yo.email(), PROBLEMA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void un_envio_sin_calificar_aparece_con_resultado_y_puntaje_nulos() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.problema(PROBLEMA)).thenReturn(problema(PROBLEMA, CURSO));
        sembrar(yo.id(), PROBLEMA, 1, "2026-11-03T09:00:00Z", null, null);

        historial(yo.email(), PROBLEMA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].resultado").value(nullValue()))
                .andExpect(jsonPath("$[0].puntaje").value(nullValue()));
    }

    @Test
    void un_problema_no_visible_da_404() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.problema(99)).thenThrow(new ProblemaNoVisibleException());

        historial(yo.email(), 99).andExpect(status().isNotFound());
    }

    @Test
    void un_problema_de_un_curso_donde_no_esta_inscrito_da_el_mismo_404() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.problema(PROBLEMA)).thenReturn(problema(PROBLEMA, 55));
        sembrar(yo.id(), PROBLEMA, 1, "2026-11-03T09:00:00Z", "correcto", 100.0);

        historial(yo.email(), PROBLEMA).andExpect(status().isNotFound());
    }

    @Test
    void si_admin_api_no_responde_da_503() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.problema(PROBLEMA)).thenThrow(new AdminNoDisponibleException("sin conexión", null));

        historial(yo.email(), PROBLEMA).andExpect(status().isServiceUnavailable());
    }

    @Test
    void sin_token_da_401_y_con_token_de_docente_403() throws Exception {
        mvc.perform(get("/api/problemas/" + PROBLEMA + "/envios")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/problemas/" + PROBLEMA + "/envios").with(tokenDe(correoUnico(), "docente")))
                .andExpect(status().isForbidden());
    }

    // El código completo de un envío es de HU42-T2 (detalle): aquí solo viajan los datos de la tabla del historial.
    @Test
    void la_respuesta_no_incluye_el_codigo_del_envio() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.problema(PROBLEMA)).thenReturn(problema(PROBLEMA, CURSO));
        sembrar(yo.id(), PROBLEMA, 1, "2026-11-03T09:00:00Z", "correcto", 100.0);

        historial(yo.email(), PROBLEMA)
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(CODIGO_SECRETO))))
                .andExpect(jsonPath("$[0].codigo").doesNotExist());
    }
}
