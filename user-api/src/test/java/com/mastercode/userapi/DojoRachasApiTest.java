package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

// HU54-T5 (CA-54.5): rachas de aciertos y de errores sobre una sesión del Dojo de verdad. Cada paso comprueba dos cosas:
// el ejercicio que entrega el Dojo es del nivel que toca antes de responder, y el nivel de la sesión después de responder
// es el que dice docs/regla-dificultad.md. A = acierto, E = error. Hay 12 ejercicios de cada nivel: no se acaban.
class DojoRachasApiTest extends EjerciciosMovilesBase {

    private static final ObjectMapper JSON = new ObjectMapper();

    @org.junit.jupiter.api.BeforeEach
    void preparar() {
        publicarDoceDeCadaNivel();
    }

    private void publicarDoceDeCadaNivel() {
        List<Ejercicio> todos = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            todos.add(prediccion(100 + i, "facil", "Facil " + i, "ok"));
            todos.add(prediccion(200 + i, "medio", "Medio " + i, "ok"));
            todos.add(prediccion(300 + i, "dificil", "Dificil " + i, "ok"));
        }
        publicar(todos.toArray(Ejercicio[]::new));
    }

    // Juega la secuencia desde un nivel guardado y devuelve el nivel de la sesión tras cada respuesta; en cada paso
    // comprueba que el ejercicio entregado era del nivel de antes.
    private List<String> jugar(String inicio, String respuestas) throws Exception {
        String yo = alumno();
        guardarNivel(yo, inicio);
        long sesion = sesion(yo);
        List<String> niveles = new ArrayList<>();
        String nivelAntes = inicio;
        for (char letra : respuestas.toCharArray()) {
            String cuerpo = pedirSiguiente(yo, sesion).andExpect(status().isOk()).andReturn().getResponse()
                    .getContentAsString();
            assertThat(JSON.readTree(cuerpo).get("dificultad").asText())
                    .as("el ejercicio que entrega el Dojo es del nivel de la sesión").isEqualTo(nivelAntes);
            long problema = JSON.readTree(cuerpo).get("id").asLong();
            enviar(yo, problema, letra == 'A' ? "ok" : "mal", null, sesion, null).andExpect(status().isOk());
            nivelAntes = jdbc.sql("SELECT nivel FROM app_schema.app_sesion_dojo WHERE id = ?").param(sesion)
                    .query(String.class).single();
            niveles.add(nivelAntes);
        }
        return niveles;
    }

    @Test
    void una_racha_larga_de_aciertos_sube_un_nivel_cada_dos_y_se_queda_en_dificil() throws Exception {
        assertThat(jugar("facil", "AAAAAAAA")).containsExactly(
                "facil", "medio", "medio", "dificil", "dificil", "dificil", "dificil", "dificil");
    }

    @Test
    void una_racha_larga_de_errores_baja_un_nivel_cada_dos_y_se_queda_en_facil() throws Exception {
        assertThat(jugar("dificil", "EEEEEEEE")).containsExactly(
                "dificil", "medio", "medio", "facil", "facil", "facil", "facil", "facil");
    }

    @Test
    void acierto_y_error_alternados_nunca_cambian_el_nivel() throws Exception {
        assertThat(jugar("medio", "AEAEAEAEAE")).containsOnly("medio");
        assertThat(jugar("medio", "EAEAEAEAEA")).containsOnly("medio");
    }

    @Test
    void un_error_aislado_reinicia_la_racha_de_aciertos_y_viceversa() throws Exception {
        assertThat(jugar("medio", "AEAAEAAE")).containsExactly(
                "medio", "medio", "medio", "dificil", "dificil", "dificil", "dificil", "dificil");
        assertThat(jugar("medio", "EAEEAEEA")).containsExactly(
                "medio", "medio", "medio", "facil", "facil", "facil", "facil", "facil");
    }

    @Test
    void despues_de_subir_hacen_falta_otros_dos_aciertos_para_volver_a_subir() throws Exception {
        assertThat(jugar("facil", "AAAA")).containsExactly("facil", "medio", "medio", "dificil");
    }

    @Test
    void un_ciclo_de_subidas_y_bajadas_sigue_la_regla() throws Exception {
        assertThat(jugar("medio", "AAEEAAEE")).containsExactly(
                "medio", "dificil", "dificil", "medio", "medio", "dificil", "dificil", "medio");
    }

    @Test
    void las_rachas_quedan_guardadas_en_la_sesion_despues_de_cada_respuesta() throws Exception {
        String yo = alumno();
        guardarNivel(yo, "medio");
        long sesion = sesion(yo);

        enviar(yo, idDelSiguiente(yo, sesion), "ok", null, sesion, null).andExpect(status().isOk());
        assertThat(jdbc.sql("SELECT racha_aciertos || '/' || racha_errores FROM app_schema.app_sesion_dojo WHERE id = ?")
                .param(sesion).query(String.class).single()).isEqualTo("1/0");

        enviar(yo, idDelSiguiente(yo, sesion), "mal", null, sesion, null).andExpect(status().isOk());
        assertThat(jdbc.sql("SELECT racha_aciertos || '/' || racha_errores FROM app_schema.app_sesion_dojo WHERE id = ?")
                .param(sesion).query(String.class).single()).isEqualTo("0/1");
    }

    @Test
    void una_respuesta_invalida_no_toca_las_rachas() throws Exception {
        String yo = alumno();
        guardarNivel(yo, "medio");
        long sesion = sesion(yo);
        enviar(yo, idDelSiguiente(yo, sesion), "ok", null, sesion, null).andExpect(status().isOk());
        long segundo = idDelSiguiente(yo, sesion);

        enviar(yo, segundo, "", null, sesion, null).andExpect(status().isBadRequest());

        assertThat(jdbc.sql("SELECT racha_aciertos FROM app_schema.app_sesion_dojo WHERE id = ?").param(sesion)
                .query(Integer.class).single()).isEqualTo(1);
    }
}
