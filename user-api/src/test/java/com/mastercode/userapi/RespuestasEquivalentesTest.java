package com.mastercode.userapi;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;

// HU47-T4 (CA-47.3, CA-47.4): respuestas escritas de forma distinta pero equivalentes se aceptan, y las que de verdad
// son otra cosa no. La regla es la de la normalización: sin espacios sobrantes, un salto de línea o tabulador como
// espacio, y sin distinguir mayúsculas. Cada caso lo responde un estudiante nuevo: no se mezclan los 3 intentos.
class RespuestasEquivalentesTest extends EjerciciosMovilesBase {

    private void comprobar(Ejercicio ejercicio, List<String> equivalentes, List<String> distintas) throws Exception {
        publicar(ejercicio);
        for (String escrita : equivalentes) {
            responder(alumno(), ejercicio.id(), escrita).andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultado").value("correcto"));
        }
        for (String escrita : distintas) {
            responder(alumno(), ejercicio.id(), escrita).andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultado").value("incorrecto"));
        }
    }

    @Test
    void completar_el_espacio_acepta_mayusculas_y_espacios_sobrantes() throws Exception {
        comprobar(completar(7, "facil", "Cadenas", "len"),
                List.of("len", "LEN", "Len", "  len", "len  ", "   len   ", "\tlen\n", "len "),
                List.of("length", "l en", "le", "len()", "len len"));
    }

    @Test
    void predecir_la_salida_acepta_espacios_repetidos_saltos_de_linea_y_mayusculas() throws Exception {
        comprobar(prediccion(8, "facil", "Texto", "Hola mundo"),
                List.of("Hola mundo", "hola mundo", "HOLA MUNDO", "  Hola   mundo ", "Hola\nmundo", "hola\tmundo",
                        "HOLA mundo"),
                List.of("hola, mundo", "holamundo", "Hola mundo!", "mundo hola", "Hola mundo 2"));
    }

    @Test
    void una_salida_de_varias_lineas_se_acepta_con_otros_saltos_y_sangrias() throws Exception {
        comprobar(prediccion(9, "facil", "Bucles", "1\n2\n3"),
                List.of("1 2 3", "1\n2\n3", "1\r\n2\r\n3", "  1\n   2\n 3  "),
                List.of("123", "1 2", "1,2,3", "3 2 1"));
    }

    @Test
    void los_numeros_y_los_simbolos_cuentan_tal_cual() throws Exception {
        comprobar(prediccion(10, "facil", "Operaciones", "7.5"),
                List.of("7.5", " 7.5 "),
                List.of("7,5", "7.50", "7", "7.5.0"));
    }

    @Test
    void una_respuesta_en_blanco_no_se_acepta_como_equivalente_de_nada() throws Exception {
        publicar(completar(7, "facil", "Cadenas", "len"));

        responder(alumno(), 7, "   ").andExpect(status().isBadRequest());
        responder(alumno(), 7, "\n\t").andExpect(status().isBadRequest());
    }
}
