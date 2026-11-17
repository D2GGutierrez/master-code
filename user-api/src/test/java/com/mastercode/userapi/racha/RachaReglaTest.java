package com.mastercode.userapi.racha;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

// HU49-T4 (CA-49.4): cuándo una racha guardada sigue viva y cuándo ya se cortó, vista desde un día «hoy». Regla pura, sin
// base de datos ni Spring.
class RachaReglaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 5);

    private static RachaServicio.RachaDelDia ver(int actual, int maxima, LocalDate ultimo) {
        return RachaServicio.delDia(new RachaRepository.Racha("python", actual, maxima, ultimo), HOY);
    }

    @Test
    void practicar_hoy_mantiene_la_racha_y_marca_que_ya_practico() {
        RachaServicio.RachaDelDia dia = ver(4, 6, HOY);

        assertThat(dia.rachaActual()).isEqualTo(4);
        assertThat(dia.practicoHoy()).isTrue();
    }

    @Test
    void si_el_ultimo_acierto_fue_ayer_la_racha_sigue_viva_y_todavia_puede_sumar_hoy() {
        RachaServicio.RachaDelDia dia = ver(4, 6, HOY.minusDays(1));

        assertThat(dia.rachaActual()).isEqualTo(4);
        assertThat(dia.practicoHoy()).isFalse();
    }

    @Test
    void saltarse_un_dia_completo_corta_la_racha_pero_la_maxima_se_conserva() {
        RachaServicio.RachaDelDia dia = ver(4, 6, HOY.minusDays(2));

        assertThat(dia.rachaActual()).isZero();
        assertThat(dia.rachaMaxima()).isEqualTo(6);
        assertThat(dia.practicoHoy()).isFalse();
    }

    @Test
    void una_racha_muy_vieja_tambien_vale_cero() {
        assertThat(ver(30, 30, HOY.minusDays(200)).rachaActual()).isZero();
    }

    @Test
    void sin_ninguna_actividad_no_hay_racha() {
        RachaServicio.RachaDelDia dia = ver(0, 0, null);

        assertThat(dia.rachaActual()).isZero();
        assertThat(dia.practicoHoy()).isFalse();
    }

    // El estudiante viajó hacia el oeste: su día local retrocedió y la fecha guardada quedó «en el futuro». Cuenta como hoy.
    @Test
    void una_fecha_guardada_posterior_a_hoy_se_trata_como_el_mismo_dia() {
        RachaServicio.RachaDelDia dia = ver(4, 6, HOY.plusDays(1));

        assertThat(dia.rachaActual()).isEqualTo(4);
        assertThat(dia.practicoHoy()).isTrue();
    }

    @Test
    void cruzar_de_mes_o_de_anio_cuenta_dia_a_dia() {
        RachaServicio.RachaDelDia dia = RachaServicio.delDia(
                new RachaRepository.Racha("java", 3, 3, LocalDate.of(2026, 12, 31)), LocalDate.of(2027, 1, 1));

        assertThat(dia.rachaActual()).isEqualTo(3);
        assertThat(dia.practicoHoy()).isFalse();
    }
}
