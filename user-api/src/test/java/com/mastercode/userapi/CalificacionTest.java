package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.mastercode.userapi.envio.Calificacion;

// HU38: la regla de calificación es pura, así que se prueba solo con números y textos (sin Spring ni base de datos).
class CalificacionTest {

    // ---------- comparación de la salida ----------

    @Test
    void los_saltos_de_linea_y_los_espacios_del_final_no_cuentan() {
        assertThat(Calificacion.coincide("5", "5")).isTrue();
        assertThat(Calificacion.coincide("5", "5\n")).isTrue();
        assertThat(Calificacion.coincide("5", "5  \n\n")).isTrue();
        assertThat(Calificacion.coincide("5\n", "5")).isTrue();
        // Windows termina las líneas con CRLF; es la misma salida.
        assertThat(Calificacion.coincide("hola\nmundo", "hola\r\nmundo\r\n")).isTrue();
        assertThat(Calificacion.coincide("a\nb", "a   \nb  \n")).isTrue();
    }

    @Test
    void una_diferencia_real_si_cuenta() {
        assertThat(Calificacion.coincide("5", "6")).isFalse();
        // Mayúsculas: un programa debe imprimir justo lo pedido.
        assertThat(Calificacion.coincide("Hola", "hola")).isFalse();
        // Espacios del medio y del principio.
        assertThat(Calificacion.coincide("a b", "a  b")).isFalse();
        assertThat(Calificacion.coincide("5", " 5")).isFalse();
        // Un salto de línea de menos o de más entre dos líneas.
        assertThat(Calificacion.coincide("a\nb", "ab")).isFalse();
        assertThat(Calificacion.coincide("a\nb", "a\n\nb")).isFalse();
        assertThat(Calificacion.coincide("5", "")).isFalse();
    }

    @Test
    void una_salida_vacia_coincide_con_una_esperada_vacia() {
        assertThat(Calificacion.coincide("", "")).isTrue();
        assertThat(Calificacion.coincide("", null)).isTrue();
        assertThat(Calificacion.coincide(null, "\n")).isTrue();
    }

    // ---------- puntaje ----------

    @Test
    void el_puntaje_es_proporcional_a_los_casos_superados() {
        assertThat(Calificacion.puntaje(3, 4)).isEqualByComparingTo("75.00");
        assertThat(Calificacion.puntaje(4, 4)).isEqualByComparingTo("100.00");
        assertThat(Calificacion.puntaje(0, 5)).isEqualByComparingTo("0.00");
        assertThat(Calificacion.puntaje(1, 2)).isEqualByComparingTo("50.00");
    }

    @Test
    void el_puntaje_se_redondea_a_dos_decimales() {
        assertThat(Calificacion.puntaje(1, 3)).isEqualByComparingTo("33.33");
        assertThat(Calificacion.puntaje(2, 3)).isEqualByComparingTo("66.67");
        assertThat(Calificacion.puntaje(1, 6)).isEqualByComparingTo("16.67");
    }

    @Test
    void sin_casos_no_se_puede_calificar() {
        assertThatThrownBy(() -> Calificacion.puntaje(0, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- resultado ----------

    @Test
    void correcto_parcial_o_incorrecto_segun_los_casos_superados() {
        assertThat(Calificacion.resultado(4, 4)).isEqualTo("correcto");
        assertThat(Calificacion.resultado(1, 1)).isEqualTo("correcto");
        assertThat(Calificacion.resultado(3, 4)).isEqualTo("parcial");
        assertThat(Calificacion.resultado(1, 4)).isEqualTo("parcial");
        assertThat(Calificacion.resultado(0, 4)).isEqualTo("incorrecto");
        assertThat(Calificacion.resultado(0, 1)).isEqualTo("incorrecto");
    }
}
