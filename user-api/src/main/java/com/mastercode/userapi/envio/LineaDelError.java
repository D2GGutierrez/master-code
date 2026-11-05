package com.mastercode.userapi.envio;

import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// HU40-T2: saca del texto de un error de compilación la línea donde el motor dice que está. Pura (sin red ni base de
// datos) para probarla solo con textos. Si el motor no informa la línea, no hay nada que mostrar.
public final class LineaDelError {

    // Java y JavaScript la ponen tras el nombre del archivo («Main.java:3: error: …», «main.js:3»); Python, tras «line»
    // en la traza («File "main.py", line 3»). Se toma la primera que aparece.
    private static final Pattern LINEA = Pattern.compile("\\.(?:java|js|py):(\\d{1,6})\\b|\\bline (\\d{1,6})\\b");

    private LineaDelError() {
    }

    public static OptionalInt de(String error) {
        if (error == null) {
            return OptionalInt.empty();
        }
        Matcher encontrado = LINEA.matcher(error);
        if (!encontrado.find()) {
            return OptionalInt.empty();
        }
        int linea = Integer.parseInt(encontrado.group(1) != null ? encontrado.group(1) : encontrado.group(2));
        // Las líneas se cuentan desde 1: un 0 no apunta a ninguna.
        return linea > 0 ? OptionalInt.of(linea) : OptionalInt.empty();
    }
}
