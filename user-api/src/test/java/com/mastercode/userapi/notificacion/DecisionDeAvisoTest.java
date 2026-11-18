package com.mastercode.userapi.notificacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.mastercode.userapi.racha.RachaRepository.Racha;

// HU52-T2 (CA-52.1): la decisión de avisar es una función pura de las rachas, el día local y la hora elegida. Sin base de
// datos ni reloj: cada caso pasa lo que mira.
class DecisionDeAvisoTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 5);
    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private static Racha racha(String lenguaje, int actual, LocalDate ultima) {
        return new Racha(lenguaje, actual, actual, ultima);
    }

    private static ZonedDateTime lima(int hora, int minuto) {
        return LocalDateTime.of(2026, 10, 5, hora, minuto).atZone(LIMA);
    }

    // ---------- qué racha está en peligro ----------

    @Test
    void una_racha_practicada_ayer_y_no_hoy_esta_en_peligro() {
        assertThat(AvisoDeRachaServicio.rachaEnPeligro(List.of(racha("python", 4, HOY.minusDays(1))), HOY)).contains(4);
    }

    @Test
    void quien_ya_practico_hoy_no_tiene_nada_que_perder() {
        assertThat(AvisoDeRachaServicio.rachaEnPeligro(List.of(racha("python", 4, HOY)), HOY)).isEmpty();
    }

    @Test
    void practicar_hoy_en_otro_lenguaje_tambien_libra_a_todos_los_lenguajes() {
        List<Racha> rachas = List.of(racha("python", 4, HOY.minusDays(1)), racha("java", 1, HOY));

        assertThat(AvisoDeRachaServicio.rachaEnPeligro(rachas, HOY)).isEmpty();
    }

    @Test
    void una_racha_ya_perdida_no_se_avisa() {
        assertThat(AvisoDeRachaServicio.rachaEnPeligro(List.of(racha("python", 4, HOY.minusDays(2))), HOY)).isEmpty();
    }

    @Test
    void sin_rachas_o_con_racha_en_cero_no_hay_aviso() {
        assertThat(AvisoDeRachaServicio.rachaEnPeligro(List.of(), HOY)).isEmpty();
        assertThat(AvisoDeRachaServicio.rachaEnPeligro(List.of(racha("python", 0, HOY.minusDays(1))), HOY)).isEmpty();
        assertThat(AvisoDeRachaServicio.rachaEnPeligro(List.of(racha("python", 3, null)), HOY)).isEmpty();
    }

    @Test
    void con_varias_rachas_en_peligro_se_avisa_la_mayor() {
        List<Racha> rachas = List.of(racha("python", 2, HOY.minusDays(1)), racha("java", 9, HOY.minusDays(1)),
                racha("javascript", 30, HOY.minusDays(4)));

        assertThat(AvisoDeRachaServicio.rachaEnPeligro(rachas, HOY)).contains(9);
    }

    @Test
    void una_fecha_de_actividad_posterior_a_hoy_cuenta_como_haber_practicado_hoy() {
        // El estudiante viajó a una zona donde su día local retrocedió (RachaRepository): no se le avisa.
        assertThat(AvisoDeRachaServicio.rachaEnPeligro(List.of(racha("python", 4, HOY.plusDays(1))), HOY)).isEmpty();
    }

    // ---------- cuándo toca avisar ----------

    @Test
    void se_avisa_desde_la_hora_elegida_y_no_antes() {
        LocalTime ochoPM = LocalTime.of(20, 0);

        assertThat(AvisoDeRachaServicio.tocaAvisar(lima(19, 59), ochoPM, null)).isFalse();
        assertThat(AvisoDeRachaServicio.tocaAvisar(lima(20, 0), ochoPM, null)).isTrue();
        assertThat(AvisoDeRachaServicio.tocaAvisar(lima(23, 59), ochoPM, null)).isTrue();
    }

    @Test
    void se_avisa_una_sola_vez_por_dia_local() {
        LocalTime hora = LocalTime.of(20, 0);

        assertThat(AvisoDeRachaServicio.tocaAvisar(lima(21, 0), hora, HOY)).isFalse();
        assertThat(AvisoDeRachaServicio.tocaAvisar(lima(21, 0), hora, HOY.minusDays(1))).isTrue();
    }

    @Test
    void el_dia_local_depende_de_la_zona_horaria() {
        // 01:30 UTC del día 6 todavía es 20:30 del día 5 en Lima, y ya es 10:30 del día 6 en Tokio.
        ZonedDateTime instante = LocalDateTime.of(2026, 10, 6, 1, 30).atZone(ZoneId.of("UTC"));
        LocalTime hora = LocalTime.of(20, 0);

        assertThat(AvisoDeRachaServicio.tocaAvisar(instante.withZoneSameInstant(LIMA), hora, null)).isTrue();
        assertThat(AvisoDeRachaServicio.tocaAvisar(instante.withZoneSameInstant(ZoneId.of("Asia/Tokyo")), hora, null)).isFalse();
    }

    // ---------- el texto ----------

    @Test
    void el_mensaje_dice_cuantos_dias_lleva() {
        assertThat(AvisoDeRachaServicio.mensaje(1)).contains("1 día seguido").doesNotContain("días");
        assertThat(AvisoDeRachaServicio.mensaje(12)).contains("12 días seguidos");
    }
}
