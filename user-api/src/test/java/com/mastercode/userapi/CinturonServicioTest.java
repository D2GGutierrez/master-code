package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import com.mastercode.userapi.cinturon.CinturonRepository;
import com.mastercode.userapi.cinturon.CinturonServicio;
import com.mastercode.userapi.cinturon.PuntosPorEjercicio;
import com.mastercode.userapi.perfil.EstudianteRepository;

// HU50-T2: sumar puntos al cinturón del lenguaje al resolver un ejercicio. Con la base real (V11 y V13); sin admin-api.
@SpringBootTest
@Transactional
class CinturonServicioTest {

    @Autowired
    private CinturonServicio servicio;

    @Autowired
    private CinturonRepository cinturones;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private EstudianteRepository estudiantes;

    private long crearEstudiante() {
        String email = "prueba-" + UUID.randomUUID() + "@ejemplo.edu.pe";
        estudiantes.insertarSiNoExiste(email, "ejemplo.edu.pe");
        return estudiantes.findByEmail(email).orElseThrow().getId();
    }

    private CinturonRepository.Cinturon cinturon(long estudiante, String lenguaje) {
        return cinturones.buscar(estudiante, lenguaje).orElseThrow();
    }

    @Test
    void los_puntos_provisionales_por_tipo_son_los_acordados_y_un_tipo_desconocido_no_da_ninguno() {
        assertThat(PuntosPorEjercicio.de("escritura_codigo")).isEqualTo(30);
        assertThat(PuntosPorEjercicio.de("prediccion_salida")).isEqualTo(10);
        assertThat(PuntosPorEjercicio.de("opcion_multiple")).isEqualTo(10);
        assertThat(PuntosPorEjercicio.de("completar_espacio")).isEqualTo(10);
        assertThat(PuntosPorEjercicio.de("otro")).isZero();
        assertThat(PuntosPorEjercicio.de(null)).isZero();
    }

    @Test
    void el_primer_acierto_crea_el_cinturon_blanco_con_los_puntos_del_tipo() {
        long estudiante = crearEstudiante();

        int sumados = servicio.sumarPorAcierto(estudiante, "python", 7, "prediccion_salida");

        assertThat(sumados).isEqualTo(10);
        assertThat(cinturon(estudiante, "python")).isEqualTo(new CinturonRepository.Cinturon(estudiante, "python", 1, 10));
    }

    @Test
    void cada_tipo_suma_sus_puntos() {
        long estudiante = crearEstudiante();

        servicio.sumarPorAcierto(estudiante, "python", 1, "escritura_codigo");
        servicio.sumarPorAcierto(estudiante, "python", 2, "opcion_multiple");
        servicio.sumarPorAcierto(estudiante, "python", 3, "completar_espacio");
        servicio.sumarPorAcierto(estudiante, "python", 4, "prediccion_salida");

        assertThat(cinturon(estudiante, "python").puntos()).isEqualTo(30 + 10 + 10 + 10);
    }

    @Test
    void repetir_un_ejercicio_ya_resuelto_no_suma_otra_vez() {
        long estudiante = crearEstudiante();

        assertThat(servicio.sumarPorAcierto(estudiante, "python", 7, "prediccion_salida")).isEqualTo(10);
        assertThat(servicio.sumarPorAcierto(estudiante, "python", 7, "prediccion_salida")).isZero();
        assertThat(servicio.sumarPorAcierto(estudiante, "python", 7, "prediccion_salida")).isZero();

        assertThat(cinturon(estudiante, "python").puntos()).isEqualTo(10);
    }

    @Test
    void los_puntos_van_al_cinturon_del_lenguaje_del_ejercicio() {
        long estudiante = crearEstudiante();

        servicio.sumarPorAcierto(estudiante, "python", 1, "escritura_codigo");
        servicio.sumarPorAcierto(estudiante, "java", 2, "opcion_multiple");

        assertThat(cinturon(estudiante, "python").puntos()).isEqualTo(30);
        assertThat(cinturon(estudiante, "java").puntos()).isEqualTo(10);
        assertThat(cinturones.buscar(estudiante, "javascript")).isEmpty();
    }

    @Test
    void el_lenguaje_se_normaliza_y_un_lenguaje_vacio_no_suma() {
        long estudiante = crearEstudiante();

        servicio.sumarPorAcierto(estudiante, "  Python ", 1, "prediccion_salida");
        assertThat(servicio.sumarPorAcierto(estudiante, " ", 2, "prediccion_salida")).isZero();
        assertThat(servicio.sumarPorAcierto(estudiante, null, 3, "prediccion_salida")).isZero();

        assertThat(cinturon(estudiante, "python").puntos()).isEqualTo(10);
    }

    @Test
    void un_tipo_sin_puntos_no_crea_el_cinturon_ni_gasta_el_ejercicio() {
        long estudiante = crearEstudiante();

        assertThat(servicio.sumarPorAcierto(estudiante, "python", 1, "tipo_nuevo")).isZero();

        assertThat(cinturones.buscar(estudiante, "python")).isEmpty();
        assertThat(jdbc.sql("SELECT count(*) FROM app_schema.app_puntos_ejercicio WHERE estudiante_id = ?")
                .param(estudiante).query(Integer.class).single()).isZero();
    }

    // CA-50.3: al llegar a los puntos que exige un cinturón, el nivel se actualiza.
    @Test
    void al_llegar_al_umbral_el_nivel_sube() {
        long estudiante = crearEstudiante();
        // 9 ejercicios de 10 = 90 puntos: todavía blanco.
        for (int i = 1; i <= 9; i++) {
            servicio.sumarPorAcierto(estudiante, "python", i, "prediccion_salida");
        }
        assertThat(cinturon(estudiante, "python")).extracting("puntos", "nivel").containsExactly(90, 1);

        servicio.sumarPorAcierto(estudiante, "python", 10, "prediccion_salida");

        // 100 puntos exactos: amarillo (nivel 2).
        assertThat(cinturon(estudiante, "python")).extracting("puntos", "nivel").containsExactly(100, 2);
    }

    @Test
    void un_solo_ejercicio_puede_subir_mas_de_un_nivel_si_pasa_varios_umbrales() {
        long estudiante = crearEstudiante();
        cinturones.sumar(estudiante, "python", 95);

        // 95 + 30 = 125 sube uno; y desde 245 con +30 = 275 pasa de amarillo (100) a naranja (250).
        servicio.sumarPorAcierto(estudiante, "python", 1, "escritura_codigo");
        assertThat(cinturon(estudiante, "python")).extracting("puntos", "nivel").containsExactly(125, 2);
        cinturones.sumar(estudiante, "python", 120);
        servicio.sumarPorAcierto(estudiante, "python", 2, "escritura_codigo");
        assertThat(cinturon(estudiante, "python")).extracting("puntos", "nivel").containsExactly(275, 3);

        // Y un salto grande de golpe cruza varios umbrales a la vez: 275 + 1725 = 2000 es negro.
        cinturones.sumar(estudiante, "python", 1725);
        assertThat(cinturon(estudiante, "python")).extracting("puntos", "nivel").containsExactly(2000, 7);
    }

    @Test
    void el_nivel_nunca_pasa_del_negro_aunque_los_puntos_sigan_subiendo() {
        long estudiante = crearEstudiante();

        cinturones.sumar(estudiante, "python", 5000);
        servicio.sumarPorAcierto(estudiante, "python", 1, "escritura_codigo");

        assertThat(cinturon(estudiante, "python")).extracting("puntos", "nivel").containsExactly(5030, 7);
    }

    @Test
    void sumar_un_cinturon_nuevo_con_muchos_puntos_arranca_en_el_nivel_que_corresponde() {
        long estudiante = crearEstudiante();

        assertThat(cinturones.sumar(estudiante, "java", 260).nivel()).isEqualTo(3);
    }

    @Test
    void cada_estudiante_lleva_su_propio_cinturon_y_su_propia_cuenta_de_ejercicios() {
        long uno = crearEstudiante();
        long otro = crearEstudiante();

        assertThat(servicio.sumarPorAcierto(uno, "python", 7, "prediccion_salida")).isEqualTo(10);
        assertThat(servicio.sumarPorAcierto(otro, "python", 7, "prediccion_salida")).isEqualTo(10);

        assertThat(cinturon(uno, "python").puntos()).isEqualTo(10);
        assertThat(cinturon(otro, "python").puntos()).isEqualTo(10);
    }
}
