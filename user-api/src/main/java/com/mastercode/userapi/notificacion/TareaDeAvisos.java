package com.mastercode.userapi.notificacion;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// HU52-T2: la tarea que revisa cada cierto tiempo a quién avisar de su racha. Está apagada por defecto
// (avisos.tarea-activa=false): se enciende cuando exista un enviador real. Con ella apagada ni siquiera se activa la
// programación de tareas de la aplicación. Una revisión que falla se registra y la siguiente corre igual.
@Component
@ConditionalOnProperty(name = "avisos.tarea-activa", havingValue = "true")
@EnableScheduling
public class TareaDeAvisos {

    private static final Logger registro = LoggerFactory.getLogger(TareaDeAvisos.class);

    private final AvisoDeRachaServicio avisos;

    public TareaDeAvisos(AvisoDeRachaServicio avisos) {
        this.avisos = avisos;
    }

    @Scheduled(fixedDelayString = "${avisos.intervalo:PT15M}")
    public void revisar() {
        try {
            int avisados = avisos.avisarLosQueCorresponde(Instant.now());
            if (avisados > 0) {
                registro.info("Avisos de racha enviados: {}", avisados);
            }
        } catch (RuntimeException e) {
            registro.warn("No se pudo revisar los avisos de racha: {}", e.toString());
        }
    }
}
