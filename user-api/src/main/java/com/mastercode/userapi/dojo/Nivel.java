package com.mastercode.userapi.dojo;

import java.util.Locale;
import java.util.Optional;

// Los tres niveles de dificultad de los problemas (campo dificultad), de menor a mayor. FACIL es el nivel con el que
// empieza un estudiante nuevo (HU54-T4, docs/regla-dificultad.md).
public enum Nivel {
    FACIL("facil"), MEDIO("medio"), DIFICIL("dificil");

    public static final Nivel INICIAL = FACIL;

    private final String valor;

    Nivel(String valor) {
        this.valor = valor;
    }

    // El texto que usan la base de datos y admin-api.
    public String valor() {
        return valor;
    }

    public static Optional<Nivel> de(String texto) {
        if (texto == null) {
            return Optional.empty();
        }
        String limpio = texto.trim().toLowerCase(Locale.ROOT);
        for (Nivel nivel : values()) {
            if (nivel.valor.equals(limpio)) {
                return Optional.of(nivel);
            }
        }
        return Optional.empty();
    }

    // Un nivel más difícil; en el tope se queda.
    public Nivel sube() {
        return this == DIFICIL ? this : values()[ordinal() + 1];
    }

    // Un nivel más fácil; en el piso se queda.
    public Nivel baja() {
        return this == FACIL ? this : values()[ordinal() - 1];
    }
}
