package com.mastercode.userapi.ejercicio;

import java.util.List;

import org.springframework.stereotype.Component;

import com.mastercode.userapi.admin.RespuestaDelProblemaDto;

// H2: elige el corrector según el tipo del ejercicio. Ningún corrector ejecuta código: los ejercicios del celular no
// pasan por el sandbox ni por la IA (HU48-T1). Los de código siguen su propio camino, el envío oficial (HU36).
@Component
public class CorrectorDeEjercicios {

    // Una predicción de salida o un espacio por completar es una línea o unas pocas; más que esto no es una respuesta.
    static final int MAX_RESPUESTA = 1_000;

    // respuesta es el texto escrito (predicción de salida o lo que completa el espacio); opcionId, la alternativa elegida
    // (opción múltiple).
    public Veredicto corregir(RespuestaDelProblemaDto problema, String respuesta, Long opcionId) {
        return switch (problema.tipoEjercicio()) {
            // HU47-T2: completar el espacio se corrige igual que predecir la salida: el texto escrito contra la
            // respuesta esperada, sin espacios sobrantes ni diferencias de mayúsculas (CA-47.3).
            case "prediccion_salida", "completar_espacio" -> corregirTexto(problema, respuesta);
            case "opcion_multiple" -> corregirOpcion(problema, opcionId);
            default -> throw new RespuestaInvalidaException("Este ejercicio se resuelve enviando código.");
        };
    }

    private static Veredicto corregirTexto(RespuestaDelProblemaDto problema, String respuesta) {
        if (respuesta == null || respuesta.isBlank()) {
            throw new RespuestaInvalidaException("Escribe tu respuesta antes de enviarla.");
        }
        if (respuesta.length() > MAX_RESPUESTA) {
            throw new RespuestaInvalidaException("La respuesta es demasiado larga (máximo " + MAX_RESPUESTA + " caracteres).");
        }
        boolean correcta = CorrectorPrediccionSalida.esCorrecta(problema.respuestaEsperada(), respuesta);
        return new Veredicto(correcta, correcta ? null : problema.respuestaEsperada());
    }

    private static Veredicto corregirOpcion(RespuestaDelProblemaDto problema, Long opcionId) {
        if (opcionId == null) {
            throw new RespuestaInvalidaException("Elige una alternativa antes de enviarla.");
        }
        List<RespuestaDelProblemaDto.Opcion> opciones = problema.opciones() == null ? List.of() : problema.opciones();
        // Una alternativa de otro ejercicio no vale: se busca solo entre las de este.
        RespuestaDelProblemaDto.Opcion elegida = opciones.stream()
                .filter(opcion -> opcion.id() == opcionId)
                .findFirst()
                .orElseThrow(() -> new RespuestaInvalidaException("Esa alternativa no es de este ejercicio."));
        if (elegida.esCorrecta()) {
            return new Veredicto(true, null);
        }
        String correcta = opciones.stream()
                .filter(RespuestaDelProblemaDto.Opcion::esCorrecta)
                .map(RespuestaDelProblemaDto.Opcion::texto)
                .findFirst()
                .orElse(null);
        return new Veredicto(false, correcta);
    }
}
