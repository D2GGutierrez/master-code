package com.mastercode.userapi;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import com.mastercode.userapi.cinturon.CinturonRepository;

// HU51-T1 (CA-51.1, CA-51.2): GET /api/yo/cinturones, el cinturón de cada lenguaje con lo que falta para el siguiente.
class CinturonesApiTest extends EjerciciosMovilesBase {

    @Autowired
    private CinturonRepository cinturones;

    private ResultActions consultar(String email) throws Exception {
        return mvc.perform(get("/api/yo/cinturones").with(tokenDeCliente(email, "mobile")));
    }

    @Test
    void sin_ningun_acierto_la_lista_va_vacia() throws Exception {
        consultar(correoUnico()).andExpect(status().isOk()).andExpect(jsonPath("$.cinturones", hasSize(0)));
    }

    @Test
    void despues_de_un_acierto_muestra_el_cinturon_blanco_y_lo_que_falta() throws Exception {
        publicar(prediccion(1, "facil", "bucles", "42"));
        String email = alumno();

        responder(email, 1, "42").andExpect(status().isOk());

        consultar(email).andExpect(status().isOk())
                .andExpect(jsonPath("$.cinturones", hasSize(1)))
                .andExpect(jsonPath("$.cinturones[0].lenguaje").value("python"))
                .andExpect(jsonPath("$.cinturones[0].nivel").value(1))
                .andExpect(jsonPath("$.cinturones[0].nombre").value("blanco"))
                .andExpect(jsonPath("$.cinturones[0].puntos").value(10))
                .andExpect(jsonPath("$.cinturones[0].siguiente.nivel").value(2))
                .andExpect(jsonPath("$.cinturones[0].siguiente.nombre").value("amarillo"))
                .andExpect(jsonPath("$.cinturones[0].siguiente.puntos_requeridos").value(100))
                .andExpect(jsonPath("$.cinturones[0].puntos_faltantes").value(90));
    }

    @Test
    void los_puntos_faltantes_bajan_con_cada_ejercicio_nuevo_y_no_con_uno_repetido() throws Exception {
        publicar(prediccion(1, "facil", "bucles", "42"), completar(2, "facil", "bucles", "x"));
        String email = alumno();

        responder(email, 1, "42").andExpect(status().isOk());
        consultar(email).andExpect(jsonPath("$.cinturones[0].puntos_faltantes").value(90));

        responder(email, 2, "x").andExpect(status().isOk());
        consultar(email).andExpect(jsonPath("$.cinturones[0].puntos_faltantes").value(80));

        // Repetir el primero no suma (D18).
        responder(email, 1, "42").andExpect(status().isOk());
        consultar(email).andExpect(jsonPath("$.cinturones[0].puntos_faltantes").value(80));
    }

    @Test
    void un_cinturon_por_lenguaje_ordenados_por_lenguaje() throws Exception {
        String email = correoUnico();
        long yo = crearEstudiante(email);
        cinturones.sumar(yo, "python", 130);
        cinturones.sumar(yo, "java", 5);

        consultar(email).andExpect(status().isOk())
                .andExpect(jsonPath("$.cinturones", hasSize(2)))
                .andExpect(jsonPath("$.cinturones[0].lenguaje").value("java"))
                .andExpect(jsonPath("$.cinturones[0].puntos_faltantes").value(95))
                .andExpect(jsonPath("$.cinturones[1].lenguaje").value("python"))
                .andExpect(jsonPath("$.cinturones[1].nombre").value("amarillo"))
                .andExpect(jsonPath("$.cinturones[1].siguiente.nombre").value("naranja"))
                .andExpect(jsonPath("$.cinturones[1].puntos_faltantes").value(120));
    }

    // CA-51.2: con el negro no hay siguiente; las claves siguen en la respuesta, con null.
    @Test
    void el_cinturon_negro_no_tiene_siguiente_ni_faltantes() throws Exception {
        String email = correoUnico();
        cinturones.sumar(crearEstudiante(email), "python", 2300);

        consultar(email).andExpect(status().isOk())
                .andExpect(jsonPath("$.cinturones[0].nombre").value("negro"))
                .andExpect(jsonPath("$.cinturones[0].nivel").value(7))
                .andExpect(jsonPath("$.cinturones[0].puntos").value(2300))
                .andExpect(jsonPath("$.cinturones[0].siguiente").value(nullValue()))
                .andExpect(jsonPath("$.cinturones[0].puntos_faltantes").value(nullValue()))
                .andExpect(jsonPath("$.cinturones[0].siguiente").hasJsonPath())
                .andExpect(jsonPath("$.cinturones[0].puntos_faltantes").hasJsonPath());
    }

    @Test
    void solo_muestra_los_cinturones_del_estudiante_de_la_sesion() throws Exception {
        cinturones.sumar(crearEstudiante(correoUnico()), "python", 500);

        consultar(correoUnico()).andExpect(jsonPath("$.cinturones", hasSize(0)));
    }

    @Test
    void sin_sesion_o_sin_ser_estudiante_no_hay_acceso() throws Exception {
        mvc.perform(get("/api/yo/cinturones")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/yo/cinturones").with(tokenDe(correoUnico(), "docente"))).andExpect(status().isForbidden());
    }
}
