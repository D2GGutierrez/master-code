package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import com.mastercode.userapi.evaluacion.PlazoDelIntento;

// HU64-T3: el plazo del intento es una regla pura, así que se prueba solo con fechas (sin Spring ni base de datos).
class PlazoDelIntentoTest {

    private static final OffsetDateTime VENTANA_INICIO = OffsetDateTime.parse("2026-11-12T14:00:00Z");
    private static final OffsetDateTime VENTANA_FIN = OffsetDateTime.parse("2026-11-12T18:00:00Z");

    @Test
    void el_plazo_es_el_inicio_mas_el_limite_de_minutos() {
        OffsetDateTime inicio = VENTANA_INICIO.plusMinutes(10);

        PlazoDelIntento plazo = PlazoDelIntento.de(inicio, 90, VENTANA_FIN);

        assertThat(plazo.fechaLimite()).isEqualTo(inicio.plusMinutes(90));
        assertThat(plazo.inicio()).isEqualTo(inicio);
    }

    @Test
    void si_la_ventana_termina_antes_que_el_limite_manda_el_fin_de_la_ventana() {
        OffsetDateTime inicio = VENTANA_FIN.minusMinutes(30);

        PlazoDelIntento plazo = PlazoDelIntento.de(inicio, 90, VENTANA_FIN);

        assertThat(plazo.fechaLimite()).isEqualTo(VENTANA_FIN);
    }

    @Test
    void antes_del_limite_no_esta_vencido_y_a_la_hora_exacta_o_despues_si() {
        PlazoDelIntento plazo = PlazoDelIntento.de(VENTANA_INICIO, 60, VENTANA_FIN);
        OffsetDateTime limite = VENTANA_INICIO.plusMinutes(60);

        assertThat(plazo.vencido(VENTANA_INICIO)).isFalse();
        assertThat(plazo.vencido(limite.minusNanos(1))).isFalse();
        assertThat(plazo.vencido(limite)).isTrue();
        assertThat(plazo.vencido(limite.plusSeconds(1))).isTrue();
    }

    @Test
    void funciona_con_fechas_en_zonas_horarias_distintas() {
        OffsetDateTime inicioEnLima = OffsetDateTime.parse("2026-11-12T09:00:00-05:00");

        PlazoDelIntento plazo = PlazoDelIntento.de(inicioEnLima, 60, VENTANA_FIN);

        // 09:00 en Lima son las 14:00 UTC: a las 14:59 UTC todavía le queda un minuto.
        assertThat(plazo.vencido(OffsetDateTime.parse("2026-11-12T14:59:00Z"))).isFalse();
        assertThat(plazo.vencido(OffsetDateTime.parse("2026-11-12T15:00:00Z"))).isTrue();
    }
}
