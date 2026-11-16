package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.mastercode.userapi.admin.RespuestaDelProblemaDto;
import com.mastercode.userapi.ejercicio.CorrectorDeEjercicios;
import com.mastercode.userapi.ejercicio.RespuestaInvalidaException;
import com.mastercode.userapi.ejercicio.Veredicto;

// H2: el corrector elige el método según el tipo del ejercicio. Sin Spring: recibe el problema ya armado.
class CorrectorDeEjerciciosTest {

    private final CorrectorDeEjercicios corrector = new CorrectorDeEjercicios();

    private static RespuestaDelProblemaDto prediccion(String esperada) {
        return new RespuestaDelProblemaDto(1, 10, "prediccion_salida", esperada, List.of());
    }

    private static RespuestaDelProblemaDto completar(String esperada) {
        return new RespuestaDelProblemaDto(4, 10, "completar_espacio", esperada, List.of());
    }

    private static RespuestaDelProblemaDto opcionMultiple() {
        return new RespuestaDelProblemaDto(2, 10, "opcion_multiple", "", List.of(
                new RespuestaDelProblemaDto.Opcion(21, "Uno", false),
                new RespuestaDelProblemaDto.Opcion(22, "Dos", true),
                new RespuestaDelProblemaDto.Opcion(23, "Tres", false)));
    }

    @Test
    void una_prediccion_acertada_no_trae_la_respuesta_correcta() {
        Veredicto v = corrector.corregir(prediccion("Hola mundo"), "  hola   MUNDO ", null);

        assertThat(v.correcta()).isTrue();
        assertThat(v.respuestaCorrecta()).isNull();
    }

    @Test
    void una_prediccion_fallada_trae_la_respuesta_correcta() {
        Veredicto v = corrector.corregir(prediccion("Hola mundo"), "Adiós", null);

        assertThat(v.correcta()).isFalse();
        assertThat(v.respuestaCorrecta()).isEqualTo("Hola mundo");
    }

    @Test
    void una_prediccion_sin_respuesta_o_demasiado_larga_es_invalida() {
        assertThatThrownBy(() -> corrector.corregir(prediccion("5"), null, null))
                .isInstanceOf(RespuestaInvalidaException.class).hasMessageContaining("Escribe");
        assertThatThrownBy(() -> corrector.corregir(prediccion("5"), "   ", null))
                .isInstanceOf(RespuestaInvalidaException.class);
        assertThatThrownBy(() -> corrector.corregir(prediccion("5"), "x".repeat(1_001), null))
                .isInstanceOf(RespuestaInvalidaException.class).hasMessageContaining("demasiado larga");
    }

    @Test
    void la_alternativa_correcta_acierta() {
        Veredicto v = corrector.corregir(opcionMultiple(), null, 22L);

        assertThat(v.correcta()).isTrue();
        assertThat(v.respuestaCorrecta()).isNull();
    }

    @Test
    void una_alternativa_equivocada_falla_y_trae_el_texto_de_la_correcta() {
        Veredicto v = corrector.corregir(opcionMultiple(), null, 21L);

        assertThat(v.correcta()).isFalse();
        assertThat(v.respuestaCorrecta()).isEqualTo("Dos");
    }

    @Test
    void sin_alternativa_o_con_una_de_otro_ejercicio_es_invalida() {
        assertThatThrownBy(() -> corrector.corregir(opcionMultiple(), null, null))
                .isInstanceOf(RespuestaInvalidaException.class).hasMessageContaining("Elige");
        assertThatThrownBy(() -> corrector.corregir(opcionMultiple(), null, 99L))
                .isInstanceOf(RespuestaInvalidaException.class).hasMessageContaining("no es de este ejercicio");
    }

    // HU47-T2 (CA-47.3, CA-47.4)
    @Test
    void un_espacio_completado_con_espacios_y_mayusculas_de_mas_es_correcto() {
        for (String escrita : new String[] { "len", "  len  ", "LEN", " Len\n" }) {
            Veredicto v = corrector.corregir(completar("len"), escrita, null);

            assertThat(v.correcta()).as(escrita).isTrue();
            assertThat(v.respuestaCorrecta()).isNull();
        }
    }

    @Test
    void un_espacio_con_varias_palabras_ignora_los_espacios_repetidos() {
        assertThat(corrector.corregir(completar("range(10)  + 1"), "RANGE(10) +   1", null).correcta()).isTrue();
    }

    @Test
    void un_espacio_mal_completado_trae_la_respuesta_correcta() {
        Veredicto v = corrector.corregir(completar("len"), "length", null);

        assertThat(v.correcta()).isFalse();
        assertThat(v.respuestaCorrecta()).isEqualTo("len");
    }

    @Test
    void lo_que_no_es_espacio_no_se_borra_al_comparar() {
        // «range(10)» y «range (10)» no son lo mismo: solo se juntan los espacios repetidos, no se quitan los de en medio.
        assertThat(corrector.corregir(completar("range(10)"), "range (10)", null).correcta()).isFalse();
    }

    @Test
    void un_espacio_sin_respuesta_o_demasiado_larga_es_invalido() {
        assertThatThrownBy(() -> corrector.corregir(completar("len"), null, null))
                .isInstanceOf(RespuestaInvalidaException.class).hasMessageContaining("Escribe");
        assertThatThrownBy(() -> corrector.corregir(completar("len"), "  ", null))
                .isInstanceOf(RespuestaInvalidaException.class).hasMessageContaining("Escribe");
        assertThatThrownBy(() -> corrector.corregir(completar("len"), "x".repeat(1_001), null))
                .isInstanceOf(RespuestaInvalidaException.class).hasMessageContaining("demasiado larga");
    }

    @Test
    void un_ejercicio_de_codigo_no_se_corrige_aqui() {
        assertThatThrownBy(() -> corrector.corregir(
                new RespuestaDelProblemaDto(3, 10, "escritura_codigo", "", List.of()), "x", null))
                .isInstanceOf(RespuestaInvalidaException.class).hasMessageContaining("código");
    }
}
