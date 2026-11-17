package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.mastercode.userapi.cinturon.CinturonRepository;
import com.mastercode.userapi.cinturon.CinturonServicio;
import com.mastercode.userapi.perfil.EstudianteRepository;

// HU50-T4 (CA-50.2, CA-50.3): con los puntos de docs/puntos-cinturon.md, el nivel sube justo al llegar a cada umbral.
// Se resuelven ejercicios de verdad (CinturonServicio) y se mira el cinturón guardado en cada paso.
@SpringBootTest
@Transactional
class CinturonSubidaDeNivelTest {

    @Autowired
    private CinturonServicio servicio;

    @Autowired
    private CinturonRepository cinturones;

    @Autowired
    private EstudianteRepository estudiantes;

    private long crearEstudiante() {
        String email = "prueba-" + UUID.randomUUID() + "@ejemplo.edu.pe";
        estudiantes.insertarSiNoExiste(email, "ejemplo.edu.pe");
        return estudiantes.findByEmail(email).orElseThrow().getId();
    }

    private int nivel(long estudiante) {
        return cinturones.buscar(estudiante, "python").orElseThrow().nivel();
    }

    // Una racha de ejercicios del celular de 10 puntos, cada uno distinto, hasta tener «puntos» en total.
    private void sumarEnEjerciciosDeDiez(long estudiante, int desde, int hasta) {
        for (int i = desde; i <= hasta; i++) {
            servicio.sumarPorAcierto(estudiante, "python", i, "prediccion_salida");
        }
    }

    @Test
    void con_ejercicios_de_diez_puntos_cada_cinturon_llega_justo_en_su_umbral() {
        long yo = crearEstudiante();
        // umbral -> nivel que se tiene al llegar a él
        int[][] umbrales = { { 100, 2 }, { 250, 3 }, { 500, 4 }, { 900, 5 }, { 1400, 6 }, { 2000, 7 } };
        int hechos = 0;
        int nivelPrevio = 1;

        for (int[] umbral : umbrales) {
            int ejerciciosParaElUmbral = umbral[0] / 10;
            // Un ejercicio antes del umbral todavía es el cinturón anterior.
            sumarEnEjerciciosDeDiez(yo, hechos + 1, ejerciciosParaElUmbral - 1);
            assertThat(nivel(yo)).as("nivel con %s puntos", umbral[0] - 10).isEqualTo(nivelPrevio);
            // El que completa el umbral sube el nivel.
            sumarEnEjerciciosDeDiez(yo, ejerciciosParaElUmbral, ejerciciosParaElUmbral);
            assertThat(nivel(yo)).as("nivel con %s puntos", umbral[0]).isEqualTo(umbral[1]);
            hechos = ejerciciosParaElUmbral;
            nivelPrevio = umbral[1];
        }
        assertThat(cinturones.buscar(yo, "python").orElseThrow().puntos()).isEqualTo(2000);
    }

    @Test
    void un_ejercicio_de_codigo_de_treinta_puntos_sube_el_nivel_al_cruzar_el_umbral() {
        long yo = crearEstudiante();
        // 3 ejercicios de código = 90 puntos: blanco. El cuarto lleva a 120: amarillo.
        for (int i = 1; i <= 3; i++) {
            servicio.sumarPorAcierto(yo, "python", i, "escritura_codigo");
        }
        assertThat(nivel(yo)).isEqualTo(1);

        servicio.sumarPorAcierto(yo, "python", 4, "escritura_codigo");

        assertThat(cinturones.buscar(yo, "python").orElseThrow().puntos()).isEqualTo(120);
        assertThat(nivel(yo)).isEqualTo(2);
    }

    @Test
    void mezclando_los_cuatro_tipos_el_total_es_la_suma_de_la_tabla() {
        long yo = crearEstudiante();

        servicio.sumarPorAcierto(yo, "python", 1, "escritura_codigo");
        servicio.sumarPorAcierto(yo, "python", 2, "prediccion_salida");
        servicio.sumarPorAcierto(yo, "python", 3, "opcion_multiple");
        servicio.sumarPorAcierto(yo, "python", 4, "completar_espacio");

        assertThat(cinturones.buscar(yo, "python").orElseThrow().puntos()).isEqualTo(60);
        assertThat(nivel(yo)).isEqualTo(1);
    }

    @Test
    void repetir_ejercicios_no_acerca_al_siguiente_cinturon() {
        long yo = crearEstudiante();

        for (int vez = 0; vez < 20; vez++) {
            servicio.sumarPorAcierto(yo, "python", 1, "escritura_codigo");
        }

        assertThat(cinturones.buscar(yo, "python").orElseThrow().puntos()).isEqualTo(30);
        assertThat(nivel(yo)).isEqualTo(1);
    }

    @Test
    void el_nivel_no_baja_aunque_lleguen_mas_ejercicios() {
        long yo = crearEstudiante();
        sumarEnEjerciciosDeDiez(yo, 1, 25);
        assertThat(nivel(yo)).isEqualTo(3);

        sumarEnEjerciciosDeDiez(yo, 26, 30);

        assertThat(nivel(yo)).isEqualTo(3);
    }

    @Test
    void cada_lenguaje_sube_de_nivel_por_separado() {
        long yo = crearEstudiante();
        for (int i = 1; i <= 10; i++) {
            servicio.sumarPorAcierto(yo, "python", i, "prediccion_salida");
        }
        servicio.sumarPorAcierto(yo, "java", 50, "prediccion_salida");

        assertThat(cinturones.buscar(yo, "python").orElseThrow().nivel()).isEqualTo(2);
        assertThat(cinturones.buscar(yo, "java").orElseThrow().nivel()).isEqualTo(1);
    }
}
