package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.mastercode.userapi.dojo.Nivel;
import com.mastercode.userapi.dojo.ReglaDificultad;
import com.mastercode.userapi.dojo.ReglaDificultad.Estado;

// HU54-T2: la regla de docs/regla-dificultad.md, probada como función pura (sin base de datos). A = acierto, E = error.
class ReglaDificultadTest {

    // Aplica la secuencia («AAE») desde un nivel y devuelve el nivel tras cada respuesta.
    private static List<Nivel> recorrer(Nivel inicio, String respuestas) {
        List<Nivel> niveles = new ArrayList<>();
        Estado estado = Estado.inicial(inicio);
        for (char c : respuestas.toCharArray()) {
            estado = ReglaDificultad.aplicar(estado, c == 'A');
            niveles.add(estado.nivel());
        }
        return niveles;
    }

    @Test
    void dos_aciertos_seguidos_suben_un_nivel() {
        assertThat(recorrer(Nivel.MEDIO, "AA")).containsExactly(Nivel.MEDIO, Nivel.DIFICIL);
        assertThat(recorrer(Nivel.FACIL, "AA")).containsExactly(Nivel.FACIL, Nivel.MEDIO);
    }

    @Test
    void dos_errores_seguidos_bajan_un_nivel() {
        assertThat(recorrer(Nivel.MEDIO, "EE")).containsExactly(Nivel.MEDIO, Nivel.FACIL);
        assertThat(recorrer(Nivel.DIFICIL, "EE")).containsExactly(Nivel.DIFICIL, Nivel.MEDIO);
    }

    @Test
    void acierto_y_error_alternados_nunca_cambian_el_nivel() {
        assertThat(recorrer(Nivel.MEDIO, "AEAEAE")).containsOnly(Nivel.MEDIO);
        assertThat(recorrer(Nivel.MEDIO, "EAEAEA")).containsOnly(Nivel.MEDIO);
    }

    @Test
    void un_error_aislado_reinicia_la_racha_de_aciertos() {
        // A E A: no hay dos aciertos seguidos; el siguiente acierto recién es el segundo de una racha nueva.
        assertThat(recorrer(Nivel.MEDIO, "AEAA")).containsExactly(Nivel.MEDIO, Nivel.MEDIO, Nivel.MEDIO, Nivel.DIFICIL);
        assertThat(recorrer(Nivel.MEDIO, "AAEAA")).containsExactly(
                Nivel.MEDIO, Nivel.DIFICIL, Nivel.DIFICIL, Nivel.DIFICIL, Nivel.DIFICIL);
    }

    @Test
    void un_acierto_aislado_reinicia_la_racha_de_errores() {
        assertThat(recorrer(Nivel.MEDIO, "EAEE")).containsExactly(Nivel.MEDIO, Nivel.MEDIO, Nivel.MEDIO, Nivel.FACIL);
    }

    @Test
    void en_el_tope_no_sube_mas_y_en_el_piso_no_baja_mas() {
        assertThat(recorrer(Nivel.MEDIO, "AAAA")).containsExactly(Nivel.MEDIO, Nivel.DIFICIL, Nivel.DIFICIL, Nivel.DIFICIL);
        assertThat(recorrer(Nivel.MEDIO, "EEEE")).containsExactly(Nivel.MEDIO, Nivel.FACIL, Nivel.FACIL, Nivel.FACIL);
    }

    @Test
    void en_los_extremos_las_rachas_vuelven_a_cero() {
        Estado enElTope = ReglaDificultad.aplicar(new Estado(Nivel.DIFICIL, 1, 0), true);
        Estado enElPiso = ReglaDificultad.aplicar(new Estado(Nivel.FACIL, 0, 1), false);

        assertThat(enElTope).isEqualTo(Estado.inicial(Nivel.DIFICIL));
        assertThat(enElPiso).isEqualTo(Estado.inicial(Nivel.FACIL));
    }

    @Test
    void despues_de_subir_hacen_falta_otros_dos_aciertos_para_volver_a_subir() {
        assertThat(recorrer(Nivel.FACIL, "AAAA")).containsExactly(Nivel.FACIL, Nivel.MEDIO, Nivel.MEDIO, Nivel.DIFICIL);
    }

    @Test
    void los_ejemplos_de_la_especificacion() {
        assertThat(recorrer(Nivel.MEDIO, "AAEE")).containsExactly(Nivel.MEDIO, Nivel.DIFICIL, Nivel.DIFICIL, Nivel.MEDIO);
    }

    @Test
    void cada_respuesta_mueve_las_rachas_como_dice_la_tabla() {
        Estado despuesDeAcierto = ReglaDificultad.aplicar(new Estado(Nivel.MEDIO, 0, 1), true);
        Estado despuesDeError = ReglaDificultad.aplicar(new Estado(Nivel.MEDIO, 1, 0), false);

        assertThat(despuesDeAcierto).isEqualTo(new Estado(Nivel.MEDIO, 1, 0));
        assertThat(despuesDeError).isEqualTo(new Estado(Nivel.MEDIO, 0, 1));
    }

    @Test
    void una_racha_larga_de_aciertos_o_errores_sigue_la_regla_sin_salirse_de_los_niveles() {
        List<Nivel> aciertos = recorrer(Nivel.FACIL, "A".repeat(50));
        List<Nivel> errores = recorrer(Nivel.DIFICIL, "E".repeat(50));

        assertThat(aciertos).last().isEqualTo(Nivel.DIFICIL);
        assertThat(errores).last().isEqualTo(Nivel.FACIL);
    }

    @Test
    void el_orden_de_busqueda_va_del_nivel_actual_al_mas_cercano_y_a_igual_distancia_el_mas_facil() {
        assertThat(ReglaDificultad.ordenDeBusqueda(Nivel.FACIL)).containsExactly(Nivel.FACIL, Nivel.MEDIO, Nivel.DIFICIL);
        assertThat(ReglaDificultad.ordenDeBusqueda(Nivel.MEDIO)).containsExactly(Nivel.MEDIO, Nivel.FACIL, Nivel.DIFICIL);
        assertThat(ReglaDificultad.ordenDeBusqueda(Nivel.DIFICIL)).containsExactly(Nivel.DIFICIL, Nivel.MEDIO, Nivel.FACIL);
    }

    @Test
    void el_nivel_se_lee_del_texto_de_la_base_de_datos() {
        assertThat(Nivel.de("Medio")).contains(Nivel.MEDIO);
        assertThat(Nivel.de(" dificil ")).contains(Nivel.DIFICIL);
        assertThat(Nivel.de("experto")).isEmpty();
        assertThat(Nivel.de(null)).isEmpty();
    }
}
