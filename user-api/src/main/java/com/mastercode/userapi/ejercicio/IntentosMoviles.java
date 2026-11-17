package com.mastercode.userapi.ejercicio;

// HU45-T3 (CA-45.5): cuántos intentos tiene el estudiante en cada ejercicio del celular, sumando todas sus sesiones y
// respuestas. Es una sola constante a propósito: cambiar el límite es cambiar este número (ADR-11, D22).
public final class IntentosMoviles {

    public static final int MAXIMO = 3;

    private IntentosMoviles() {
    }

    public static int restantes(int usados) {
        return Math.max(0, MAXIMO - usados);
    }
}
