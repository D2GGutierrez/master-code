package com.mastercode.userapi.ejercicio;

// HU45-T1: corrige un ejercicio «predecir la salida» por comparación directa. Se compara lo que escribió el estudiante
// con la respuesta esperada, las dos normalizadas (TextoNormalizado). No usa sandbox ni IA: no ejecuta nada.
public final class CorrectorPrediccionSalida {

    private CorrectorPrediccionSalida() {
    }

    public static boolean esCorrecta(String esperada, String respuesta) {
        String esperadaNormalizada = TextoNormalizado.de(esperada);
        // Un ejercicio sin respuesta esperada no se puede acertar: sin esto, una respuesta en blanco lo daría por bueno.
        return !esperadaNormalizada.isEmpty() && esperadaNormalizada.equals(TextoNormalizado.de(respuesta));
    }
}
