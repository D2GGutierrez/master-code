package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.mastercode.userapi.problema.EstadoProblema;

// HU30-T2 (CA-30.3): la regla que distingue resueltos, intentados y no abiertos. Sin Spring ni base de datos.
class EstadoProblemaTest {

    @Test
    void sin_envios_el_problema_esta_sin_abrir() {
        assertThat(EstadoProblema.de(null)).isEqualTo(EstadoProblema.SIN_ABRIR);
    }

    @Test
    void con_envios_y_ninguno_correcto_esta_intentado() {
        assertThat(EstadoProblema.de(false)).isEqualTo(EstadoProblema.INTENTADO);
    }

    @Test
    void con_algun_envio_correcto_esta_resuelto() {
        assertThat(EstadoProblema.de(true)).isEqualTo(EstadoProblema.RESUELTO);
    }

    @Test
    void los_textos_de_la_api_son_estos_tres() {
        assertThat(EstadoProblema.SIN_ABRIR.valor()).isEqualTo("sin_abrir");
        assertThat(EstadoProblema.INTENTADO.valor()).isEqualTo("intentado");
        assertThat(EstadoProblema.RESUELTO.valor()).isEqualTo("resuelto");
    }

    @Test
    void el_texto_del_filtro_se_reconoce_exacto() {
        assertThat(EstadoProblema.deTexto("sin_abrir")).contains(EstadoProblema.SIN_ABRIR);
        assertThat(EstadoProblema.deTexto("intentado")).contains(EstadoProblema.INTENTADO);
        assertThat(EstadoProblema.deTexto("resuelto")).contains(EstadoProblema.RESUELTO);
    }

    @Test
    void cualquier_otro_texto_no_se_reconoce() {
        assertThat(EstadoProblema.deTexto("RESUELTO")).isEmpty();
        assertThat(EstadoProblema.deTexto("sin abrir")).isEmpty();
        assertThat(EstadoProblema.deTexto("")).isEmpty();
        assertThat(EstadoProblema.deTexto(null)).isEmpty();
    }
}
