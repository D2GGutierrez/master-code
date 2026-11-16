package com.mastercode.userapi.cinturon;

import java.util.Map;

// HU50-T2: cuántos puntos da resolver un ejercicio de cada tipo. Son valores PROVISIONALES: HU50-T4 los confirma en
// docs/puntos-cinturon.md y los deja aquí (CA-50.2). Un tipo que no está en la tabla no da puntos.
public final class PuntosPorEjercicio {

    private static final Map<String, Integer> PUNTOS = Map.of(
            "escritura_codigo", 30,
            "prediccion_salida", 10,
            "opcion_multiple", 10,
            "completar_espacio", 10);

    private PuntosPorEjercicio() {
    }

    public static int de(String tipoEjercicio) {
        // Map.of no admite null: un problema sin tipo no da puntos.
        return tipoEjercicio == null ? 0 : PUNTOS.getOrDefault(tipoEjercicio, 0);
    }
}
