package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.mastercode.userapi.ejercicio.CorrectorPrediccionSalida;
import com.mastercode.userapi.ejercicio.TextoNormalizado;

// HU45-T1: «predecir la salida» se corrige comparando texto normalizado. Sin Spring: es una comparación de textos.
class CorrectorPrediccionSalidaTest {

    @Test
    void la_respuesta_igual_a_la_esperada_es_correcta() {
        assertThat(CorrectorPrediccionSalida.esCorrecta("Hola mundo", "Hola mundo")).isTrue();
    }

    @Test
    void una_respuesta_distinta_es_incorrecta() {
        assertThat(CorrectorPrediccionSalida.esCorrecta("Hola mundo", "Hola")).isFalse();
        assertThat(CorrectorPrediccionSalida.esCorrecta("5", "6")).isFalse();
    }

    @Test
    void ignora_los_espacios_al_inicio_y_al_final() {
        assertThat(CorrectorPrediccionSalida.esCorrecta("5", "  5 ")).isTrue();
    }

    @Test
    void reduce_los_espacios_repetidos_a_uno() {
        assertThat(CorrectorPrediccionSalida.esCorrecta("a b c", "a    b   c")).isTrue();
    }

    @Test
    void no_distingue_mayusculas_de_minusculas() {
        assertThat(CorrectorPrediccionSalida.esCorrecta("True", "true")).isTrue();
        assertThat(CorrectorPrediccionSalida.esCorrecta("HOLA", "hola")).isTrue();
    }

    @Test
    void un_salto_de_linea_cuenta_como_espacio() {
        assertThat(CorrectorPrediccionSalida.esCorrecta("1\n2\n3", "1 2 3")).isTrue();
        assertThat(CorrectorPrediccionSalida.esCorrecta("1 2 3", "1\r\n2\t3")).isTrue();
    }

    @Test
    void el_espacio_duro_que_pone_el_teclado_tambien_cuenta_como_espacio() {
        assertThat(CorrectorPrediccionSalida.esCorrecta("a b", "a b")).isTrue();
    }

    @Test
    void no_borra_lo_que_no_es_espacio() {
        // Los signos importan: «Hola, mundo» no es «Hola mundo».
        assertThat(CorrectorPrediccionSalida.esCorrecta("Hola, mundo", "Hola mundo")).isFalse();
        assertThat(CorrectorPrediccionSalida.esCorrecta("ab", "a b")).isFalse();
    }

    @Test
    void una_respuesta_en_blanco_nunca_acierta() {
        assertThat(CorrectorPrediccionSalida.esCorrecta("5", "")).isFalse();
        assertThat(CorrectorPrediccionSalida.esCorrecta("5", "   ")).isFalse();
        assertThat(CorrectorPrediccionSalida.esCorrecta("5", null)).isFalse();
    }

    @Test
    void un_ejercicio_sin_respuesta_esperada_no_se_puede_acertar() {
        assertThat(CorrectorPrediccionSalida.esCorrecta("", "")).isFalse();
        assertThat(CorrectorPrediccionSalida.esCorrecta("  ", "  ")).isFalse();
        assertThat(CorrectorPrediccionSalida.esCorrecta(null, null)).isFalse();
    }

    @Test
    void el_texto_normalizado_trata_el_nulo_como_vacio() {
        assertThat(TextoNormalizado.de(null)).isEmpty();
        assertThat(TextoNormalizado.de("  A   B ")).isEqualTo("a b");
    }
}
