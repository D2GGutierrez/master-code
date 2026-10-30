package com.mastercode.userapi.envio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Collectors;

// HU38: cómo se califica un envío. Pura (sin base de datos ni red) para poder probarla solo con números.
// La fórmula está en docs/decisiones-tecnicas-adr.md (D7).
public final class Calificacion {

    public static final String CORRECTO = "correcto";
    public static final String PARCIAL = "parcial";
    public static final String INCORRECTO = "incorrecto";

    private Calificacion() {
    }

    // Compara la salida del programa con la esperada sin que cuenten los saltos de línea ni los espacios del final,
    // que el estudiante no ve. Mayúsculas y espacios del medio sí cuentan: un programa debe imprimir justo lo pedido.
    public static boolean coincide(String esperada, String obtenida) {
        return normalizar(esperada).equals(normalizar(obtenida));
    }

    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        // lines() separa por \n, \r\n y \r: así un programa que imprime con \r\n coincide con una salida con \n.
        return texto.lines().map(String::stripTrailing).collect(Collectors.joining("\n")).stripTrailing();
    }

    // Proporcional a los casos superados, con dos decimales: 3 de 4 casos son 75,00.
    public static BigDecimal puntaje(int superados, int total) {
        if (total <= 0) {
            throw new IllegalArgumentException("Un problema que se califica tiene al menos un caso de prueba.");
        }
        return BigDecimal.valueOf(superados)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    // correcto = todos los casos; incorrecto = ninguno; parcial = algunos. Solo «correcto» deja el problema resuelto.
    public static String resultado(int superados, int total) {
        if (superados >= total) {
            return CORRECTO;
        }
        return superados == 0 ? INCORRECTO : PARCIAL;
    }
}
