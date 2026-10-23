package com.mastercode.userapi.ejecucion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

// HU06-T4: ventana deslizante de 60 s por estudiante. Sin Spring ni base: es lógica pura con un reloj manual.
class LimiteEjecucionesTest {

    // Reloj que solo avanza cuando la prueba lo manda: ninguna prueba espera un minuto de verdad.
    static class RelojManual extends Clock {
        private long ms = 1_000_000;

        void avanzarSegundos(double segundos) {
            ms += Math.round(segundos * 1000);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(ms);
        }
    }

    private final RelojManual reloj = new RelojManual();

    private void gastar(LimiteEjecuciones limite, long estudiante, int veces) {
        for (int i = 0; i < veces; i++) {
            assertThat(limite.intentar(estudiante)).isZero();
        }
    }

    @Test
    void deja_pasar_hasta_el_limite_y_luego_dice_cuantos_segundos_esperar() {
        LimiteEjecuciones limite = new LimiteEjecuciones(10, reloj);

        gastar(limite, 1, 10);

        assertThat(limite.intentar(1)).isEqualTo(60);
        reloj.avanzarSegundos(20);
        assertThat(limite.intentar(1)).isEqualTo(40);
    }

    @Test
    void la_espera_se_redondea_hacia_arriba_y_nunca_es_cero() {
        LimiteEjecuciones limite = new LimiteEjecuciones(1, reloj);
        gastar(limite, 1, 1);

        reloj.avanzarSegundos(59.5);

        assertThat(limite.intentar(1)).isEqualTo(1);
    }

    @Test
    void pasados_60_segundos_se_libera_el_lugar() {
        LimiteEjecuciones limite = new LimiteEjecuciones(2, reloj);
        gastar(limite, 1, 2);

        reloj.avanzarSegundos(59.9);
        assertThat(limite.intentar(1)).isPositive();
        reloj.avanzarSegundos(0.1);

        assertThat(limite.intentar(1)).isZero();
    }

    @Test
    void la_ventana_se_desliza_y_no_se_reinicia_de_golpe() {
        LimiteEjecuciones limite = new LimiteEjecuciones(3, reloj);
        gastar(limite, 1, 1); // t = 0
        reloj.avanzarSegundos(30);
        gastar(limite, 1, 1); // t = 30
        reloj.avanzarSegundos(10);
        gastar(limite, 1, 1); // t = 40

        reloj.avanzarSegundos(10); // t = 50: lleno; el más viejo (t = 0) vence en 10 s
        assertThat(limite.intentar(1)).isEqualTo(10);

        reloj.avanzarSegundos(10); // t = 60: queda libre solo el lugar del t = 0
        assertThat(limite.intentar(1)).isZero();
        assertThat(limite.intentar(1)).isEqualTo(30); // el siguiente en vencer es el de t = 30
    }

    @Test
    void un_intento_rechazado_no_alarga_la_espera() {
        LimiteEjecuciones limite = new LimiteEjecuciones(1, reloj);
        gastar(limite, 1, 1);

        for (int i = 0; i < 5; i++) {
            reloj.avanzarSegundos(10);
            assertThat(limite.intentar(1)).isPositive();
        }
        reloj.avanzarSegundos(10); // 60 s desde la única ejecución aceptada

        assertThat(limite.intentar(1)).isZero();
    }

    @Test
    void cada_estudiante_tiene_su_propio_limite() {
        LimiteEjecuciones limite = new LimiteEjecuciones(2, reloj);
        gastar(limite, 1, 2);

        assertThat(limite.intentar(1)).isPositive();
        assertThat(limite.intentar(2)).isZero();
    }

    @Test
    void un_limite_menor_que_1_es_un_error_de_configuracion() {
        assertThatThrownBy(() -> new LimiteEjecuciones(0, reloj))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EJECUCIONES_POR_MINUTO");
        assertThatThrownBy(() -> new LimiteEjecuciones(-5, reloj)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void peticiones_simultaneas_del_mismo_estudiante_no_superan_el_limite() throws Exception {
        LimiteEjecuciones limite = new LimiteEjecuciones(10, reloj);
        int hilos = 50;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Long>> resultados = new ArrayList<>();
        try {
            for (int i = 0; i < hilos; i++) {
                resultados.add(pool.submit(() -> {
                    salida.await();
                    return limite.intentar(7);
                }));
            }
            salida.countDown();
            int aceptadas = 0;
            for (Future<Long> resultado : resultados) {
                if (resultado.get() == 0) {
                    aceptadas++;
                }
            }
            assertThat(aceptadas).isEqualTo(10);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void los_estudiantes_inactivos_se_descartan_cuando_el_mapa_crece_demasiado() {
        LimiteEjecuciones limite = new LimiteEjecuciones(10, reloj);
        for (long id = 1; id <= 10_001; id++) {
            limite.intentar(id);
        }
        assertThat(limite.estudiantesRegistrados()).isGreaterThan(10_000);

        reloj.avanzarSegundos(61);
        limite.intentar(99_999);

        // Solo queda quien ejecutó hace menos de un minuto.
        assertThat(limite.estudiantesRegistrados()).isEqualTo(1);
    }
}
