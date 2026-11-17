package com.mastercode.userapi.dojo;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

// HU54-T2: la regla de docs/regla-dificultad.md. Dos aciertos seguidos suben un nivel y dos errores seguidos lo bajan.
// Función pura: sin base de datos ni reloj, para probarla con rachas largas (HU54-T5).
public final class ReglaDificultad {

    // Cuántos aciertos (o errores) seguidos cambian el nivel.
    static final int RACHA_PARA_CAMBIAR = 2;

    // El nivel de la sesión y sus dos rachas.
    public record Estado(Nivel nivel, int rachaAciertos, int rachaErrores) {

        public static Estado inicial(Nivel nivel) {
            return new Estado(nivel, 0, 0);
        }
    }

    private ReglaDificultad() {
    }

    // Estado después de una respuesta corregida (acierto o error). Una respuesta inválida no llega aquí: no cuenta.
    public static Estado aplicar(Estado antes, boolean acierto) {
        int aciertos = acierto ? antes.rachaAciertos() + 1 : 0;
        int errores = acierto ? 0 : antes.rachaErrores() + 1;
        if (acierto && aciertos >= RACHA_PARA_CAMBIAR) {
            // Después de cambiar (o de chocar con el tope) las dos rachas vuelven a 0.
            return Estado.inicial(antes.nivel().sube());
        }
        if (!acierto && errores >= RACHA_PARA_CAMBIAR) {
            return Estado.inicial(antes.nivel().baja());
        }
        return new Estado(antes.nivel(), aciertos, errores);
    }

    // En qué niveles buscar el siguiente ejercicio: primero el del estudiante, luego los más cercanos; a igual distancia,
    // el más fácil primero (CA-54.2).
    public static List<Nivel> ordenDeBusqueda(Nivel nivel) {
        return Arrays.stream(Nivel.values())
                .sorted(Comparator.comparingInt((Nivel n) -> Math.abs(n.ordinal() - nivel.ordinal()))
                        .thenComparingInt(Nivel::ordinal))
                .toList();
    }
}
