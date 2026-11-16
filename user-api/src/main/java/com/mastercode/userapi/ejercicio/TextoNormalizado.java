package com.mastercode.userapi.ejercicio;

import java.util.Locale;
import java.util.regex.Pattern;

// La regla de normalización de las respuestas escritas desde el celular (registro de cambios, «Normalización de
// respuestas móviles»; CA-45.3 y CA-47.3): sin espacios al inicio ni al final, espacios repetidos reducidos a uno y sin
// distinguir mayúsculas de minúsculas. Un salto de línea o un tabulador cuentan como espacio. UNICODE_CHARACTER_CLASS
// hace que también cuenten el espacio duro y los demás espacios de otros idiomas, que el teclado del celular puede
// poner sin que el estudiante lo note.
public final class TextoNormalizado {

    private static final Pattern ESPACIOS = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

    private TextoNormalizado() {
    }

    // Un texto nulo se trata como vacío.
    public static String de(String texto) {
        if (texto == null) {
            return "";
        }
        return ESPACIOS.matcher(texto).replaceAll(" ").strip().toLowerCase(Locale.ROOT);
    }
}
