package com.mastercode.userapi.ejecucion;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;

// HU06-T4: cuántas veces seguidas puede «Ejecutar» un estudiante por minuto, para no saturar el sandbox.
// Ventana deslizante: cada estudiante tiene la lista de las horas de sus últimas ejecuciones y solo cuentan
// las de los últimos 60 s. Vive en memoria: si user-api se reinicia, los contadores empiezan de cero, y
// con varias réplicas cada una cuenta por su lado (hoy hay una sola; con más haría falta un contador compartido).
public class LimiteEjecuciones {

    private static final long VENTANA_MS = 60_000;
    // Pasado este número de estudiantes en el mapa se descartan los que llevan un minuto sin ejecutar.
    private static final int LIMPIAR_DESDE = 10_000;

    private final int limite;
    private final Clock reloj;
    private final ConcurrentHashMap<Long, ArrayDeque<Long>> usos = new ConcurrentHashMap<>();

    // El reloj se recibe para que las pruebas avancen el tiempo sin esperar un minuto real.
    public LimiteEjecuciones(int limite, Clock reloj) {
        if (limite < 1) {
            throw new IllegalArgumentException(
                    "EJECUCIONES_POR_MINUTO debe ser al menos 1 (valor recibido: " + limite + ")");
        }
        this.limite = limite;
        this.reloj = reloj;
    }

    // Registra una ejecución y devuelve 0 si puede seguir. Si ya llegó al límite no registra nada
    // y devuelve los segundos (mínimo 1) que faltan para que se libere un lugar.
    public long intentar(long estudianteId) {
        long ahora = reloj.millis();
        long[] espera = {0};
        // compute() es atómico por estudiante: dos peticiones simultáneas no se cuelan por encima del límite.
        usos.compute(estudianteId, (id, anteriores) -> {
            ArrayDeque<Long> fechas = anteriores == null ? new ArrayDeque<>() : anteriores;
            while (!fechas.isEmpty() && ahora - fechas.peekFirst() >= VENTANA_MS) {
                fechas.pollFirst();
            }
            if (fechas.size() >= limite) {
                long faltanMs = VENTANA_MS - (ahora - fechas.peekFirst());
                espera[0] = Math.max(1, (faltanMs + 999) / 1000);
            } else {
                fechas.addLast(ahora);
            }
            return fechas;
        });
        if (usos.size() > LIMPIAR_DESDE) {
            limpiar(ahora);
        }
        return espera[0];
    }

    // Solo para las pruebas: cuántos estudiantes tiene registrados el mapa.
    int estudiantesRegistrados() {
        return usos.size();
    }

    private void limpiar(long ahora) {
        for (Long id : usos.keySet()) {
            usos.computeIfPresent(id,
                    (clave, fechas) -> fechas.isEmpty() || ahora - fechas.peekLast() >= VENTANA_MS ? null : fechas);
        }
    }
}
