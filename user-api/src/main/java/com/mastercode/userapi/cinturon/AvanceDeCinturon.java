package com.mastercode.userapi.cinturon;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

// HU51-T1: cuántos puntos le faltan a un cinturón para el siguiente. Función pura sobre la tabla de niveles: sin base de
// datos, para probarla en todos los niveles. El negro es el último: no tiene siguiente (siguiente y faltantes en null).
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.ALWAYS)
public record AvanceDeCinturon(String lenguaje, int nivel, String nombre, int puntos, Siguiente siguiente,
        Integer puntosFaltantes) {

    // El cinturón que viene: su nivel, su nombre y los puntos acumulados desde los que se alcanza.
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Siguiente(int nivel, String nombre, int puntosRequeridos) {
    }

    // Los puntos del nivel actual y del siguiente van en la respuesta para que la barra de avance (HU51-T2) se dibuje sin
    // volver a preguntar.
    public static AvanceDeCinturon de(CinturonRepository.Cinturon cinturon, List<CinturonRepository.Nivel> niveles) {
        CinturonRepository.Nivel actual = niveles.stream()
                .filter(n -> n.nivel() == cinturon.nivel())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Nivel de cinturón desconocido: " + cinturon.nivel()));
        // El siguiente es el menor nivel por encima del actual.
        CinturonRepository.Nivel proximo = niveles.stream()
                .filter(n -> n.nivel() > cinturon.nivel())
                .min((a, b) -> Integer.compare(a.nivel(), b.nivel()))
                .orElse(null);
        if (proximo == null) {
            return new AvanceDeCinturon(cinturon.lenguaje(), actual.nivel(), actual.nombre(), cinturon.puntos(), null, null);
        }
        return new AvanceDeCinturon(cinturon.lenguaje(), actual.nivel(), actual.nombre(), cinturon.puntos(),
                new Siguiente(proximo.nivel(), proximo.nombre(), proximo.puntosRequeridos()),
                Math.max(0, proximo.puntosRequeridos() - cinturon.puntos()));
    }
}
