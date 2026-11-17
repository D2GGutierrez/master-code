package com.mastercode.userapi.cinturon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.mastercode.userapi.cinturon.CinturonRepository.Cinturon;
import com.mastercode.userapi.cinturon.CinturonRepository.Nivel;

// HU51-T1 (CA-51.1, CA-51.2): cuántos puntos faltan para el siguiente cinturón. Función pura, probada en los siete niveles
// con la misma tabla que siembra la migración V11.
class AvanceDeCinturonTest {

    private static final List<Nivel> NIVELES = List.of(
            new Nivel(1, "blanco", 0),
            new Nivel(2, "amarillo", 100),
            new Nivel(3, "naranja", 250),
            new Nivel(4, "verde", 500),
            new Nivel(5, "azul", 900),
            new Nivel(6, "marron", 1400),
            new Nivel(7, "negro", 2000));

    private static AvanceDeCinturon avance(int nivel, int puntos) {
        return AvanceDeCinturon.de(new Cinturon(1, "python", nivel, puntos), NIVELES);
    }

    @Test
    void con_diez_puntos_en_blanco_faltan_noventa_para_el_amarillo() {
        AvanceDeCinturon a = avance(1, 10);

        assertThat(a.nombre()).isEqualTo("blanco");
        assertThat(a.siguiente()).isEqualTo(new AvanceDeCinturon.Siguiente(2, "amarillo", 100));
        assertThat(a.puntosFaltantes()).isEqualTo(90);
    }

    @Test
    void en_cada_nivel_falta_lo_que_pide_el_siguiente_menos_los_puntos() {
        // nivel, puntos, siguiente, faltantes
        Object[][] casos = {
                { 1, 0, "amarillo", 100 },
                { 1, 99, "amarillo", 1 },
                { 2, 100, "naranja", 150 },
                { 2, 249, "naranja", 1 },
                { 3, 250, "verde", 250 },
                { 3, 499, "verde", 1 },
                { 4, 500, "azul", 400 },
                { 4, 899, "azul", 1 },
                { 5, 900, "marron", 500 },
                { 5, 1399, "marron", 1 },
                { 6, 1400, "negro", 600 },
                { 6, 1999, "negro", 1 },
        };
        for (Object[] caso : casos) {
            AvanceDeCinturon a = avance((int) caso[0], (int) caso[1]);

            assertThat(a.siguiente().nombre()).as("siguiente con %s puntos", caso[1]).isEqualTo(caso[2]);
            assertThat(a.puntosFaltantes()).as("faltantes con %s puntos", caso[1]).isEqualTo(caso[3]);
        }
    }

    @Test
    void el_siguiente_indica_los_puntos_desde_los_que_se_alcanza_para_dibujar_la_barra() {
        AvanceDeCinturon a = avance(3, 300);

        assertThat(a.siguiente().puntosRequeridos()).isEqualTo(500);
        assertThat(a.puntos()).isEqualTo(300);
    }

    // CA-51.2: con el cinturón negro no hay siguiente.
    @Test
    void el_negro_no_tiene_siguiente_ni_puntos_faltantes() {
        AvanceDeCinturon a = avance(7, 2150);

        assertThat(a.nombre()).isEqualTo("negro");
        assertThat(a.puntos()).isEqualTo(2150);
        assertThat(a.siguiente()).isNull();
        assertThat(a.puntosFaltantes()).isNull();
    }

    @Test
    void un_nivel_que_no_esta_en_la_tabla_es_un_error_y_no_un_dato_inventado() {
        assertThatThrownBy(() -> avance(9, 10)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void los_puntos_faltantes_nunca_son_negativos_si_el_nivel_guardado_quedo_atrasado() {
        // No debería pasar (el nivel se calcula al sumar), pero la cuenta no puede dar un número negativo.
        assertThat(avance(1, 150).puntosFaltantes()).isZero();
    }

    @Test
    void el_lenguaje_viaja_en_la_respuesta() {
        assertThat(AvanceDeCinturon.de(new Cinturon(1, "java", 1, 0), NIVELES).lenguaje()).isEqualTo("java");
    }
}
