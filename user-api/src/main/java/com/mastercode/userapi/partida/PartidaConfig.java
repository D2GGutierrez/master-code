package com.mastercode.userapi.partida;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// El reloj de las partidas: un bean para que las pruebas puedan poner la hora que quieran.
@Configuration
public class PartidaConfig {

    @Bean
    Clock relojDePartida() {
        return Clock.systemUTC();
    }
}
