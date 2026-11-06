package com.mastercode.userapi.evaluacion;

import java.time.OffsetDateTime;

// HU64-T3: hasta cuándo tiene el estudiante para resolver la evaluación. Es lo que pasa primero: su límite de minutos
// contados desde que empezó, o el fin de la ventana de la evaluación. Pura (sin base de datos ni red) para probarla
// solo con fechas.
public record PlazoDelIntento(OffsetDateTime inicio, OffsetDateTime fechaLimite) {

    public static PlazoDelIntento de(OffsetDateTime inicio, int limiteMinutos, OffsetDateTime finDeLaVentana) {
        OffsetDateTime porLimite = inicio.plusMinutes(limiteMinutos);
        return new PlazoDelIntento(inicio, porLimite.isBefore(finDeLaVentana) ? porLimite : finDeLaVentana);
    }

    // A la hora exacta del límite ya está vencido, igual que a la hora de fin de la ventana (HU64-T1).
    public boolean vencido(OffsetDateTime ahora) {
        return !ahora.isBefore(fechaLimite);
    }
}
