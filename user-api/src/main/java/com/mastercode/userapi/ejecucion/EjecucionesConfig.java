package com.mastercode.userapi.ejecucion;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EjecucionesConfig {

    // Un valor que no sea número entero mayor que 0 hace fallar el arranque con un mensaje claro.
    @Bean
    LimiteEjecuciones limiteEjecuciones(@Value("${ejecuciones.limite-por-minuto}") int limitePorMinuto) {
        return new LimiteEjecuciones(limitePorMinuto, Clock.systemUTC());
    }
}
